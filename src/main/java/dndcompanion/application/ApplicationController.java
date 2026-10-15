package dndcompanion.application;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.ClassId;
import dndcompanion.persistence.CharacterJson;
import dndcompanion.persistence.CharacterRepository;
import dndcompanion.persistence.CharacterRepository.Entry;
import dndcompanion.persistence.CharacterRepository.Preferences;
import dndcompanion.persistence.SQLiteCharacterRepository;
import dndcompanion.persistence.SQLiteSpellRepository;
import dndcompanion.ui.CharacterRosterPanel;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.WindowChrome;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/** Single desktop lifecycle. IO runs serially off the EDT and always captures the character ID. */
public final class ApplicationController implements CharacterRosterPanel.Actions, AutoCloseable {
    private final JFrame window = new JFrame("DnD Companion · Characters (2024)");
    private final ExecutorService io =
            Executors.newSingleThreadExecutor(
                    task -> {
                        Thread thread = new Thread(task, "character-storage");
                        thread.setDaemon(true);
                        return thread;
                    });
    private final Path directory;
    private CharacterRepository repository;
    private CharacterRosterPanel roster;
    private GrimoirePanel active;
    private UUID activeId;
    private boolean busy, closed;
    private final java.awt.KeyEventDispatcher busyKeys =
            event -> busy && SwingUtilities.getWindowAncestor(event.getComponent()) == window;

    public ApplicationController(Path directory) {
        this.directory = directory.toAbsolutePath();
        WindowChrome.install(window);
        window.getGlassPane().addMouseListener(new java.awt.event.MouseAdapter() {});
        window.getGlassPane().addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {});
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .addKeyEventDispatcher(busyKeys);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent event) {
                        exit();
                    }

                    @Override
                    public void windowClosed(WindowEvent event) {
                        close();
                    }
                });
        var screen =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        window.setMinimumSize(
                new Dimension(Math.min(920, screen.width), Math.min(680, screen.height)));
        window.setSize(Math.min(1480, screen.width), Math.min(960, screen.height));
        window.setLocationRelativeTo(null);
        JPanel loading = AppStyle.section(new BorderLayout());
        loading.add(new JLabel("Opening local character storage…", JLabel.CENTER));
        window.setContentPane(loading);
    }

    public JFrame window() {
        return window;
    }

    ApplicationController(Path directory, CharacterRepository repository) {
        this(directory);
        this.repository = repository;
    }

    public GrimoirePanel activePanel() {
        return active;
    }

    public UUID activeId() {
        return activeId;
    }

    public boolean busy() {
        return busy;
    }

    public void start() {
        run(
                () -> {
                    SQLiteSpellRepository.bundled();
                    if (repository != null) return repository.preferences();
                    var store = new SQLiteCharacterRepository(directory.resolve("characters.db"));
                    store.importLegacy(directory.resolve("character.db"));
                    repository = store;
                    return store.preferences();
                },
                preferences -> {
                    AppStyle.appearance(preferences.dark(), preferences.textSize());
                    roster = new CharacterRosterPanel(this);
                    showRoster();
                },
                error -> {
                    JPanel failed = AppStyle.section(new BorderLayout());
                    failed.add(
                            new JLabel(
                                    "Could not open character storage. Your saves have not been"
                                            + " replaced.",
                                    JLabel.CENTER));
                    failed.add(AppStyle.button("Try again", this::start), BorderLayout.SOUTH);
                    window.setContentPane(failed);
                    report(error);
                });
    }

    private <T> void run(Callable<T> operation, Consumer<T> success) {
        run(operation, success, this::report);
    }

    private <T> void run(Callable<T> operation, Consumer<T> success, Consumer<Exception> failure) {
        if (busy || closed) return;
        busy = true;
        window.getGlassPane().setVisible(true);
        window.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
        io.submit(
                () -> {
                    try {
                        T result = operation.call();
                        SwingUtilities.invokeLater(
                                () -> finish(() -> success.accept(result), failure));
                    } catch (Exception error) {
                        SwingUtilities.invokeLater(
                                () -> finish(() -> failure.accept(error), failure));
                    }
                });
    }

    private void finish(Runnable complete, Consumer<Exception> failure) {
        busy = false;
        if (closed) return;
        window.getGlassPane().setVisible(false);
        window.setCursor(java.awt.Cursor.getDefaultCursor());
        try {
            complete.run();
        } catch (Exception error) {
            failure.accept(error);
        }
        window.revalidate();
        window.repaint();
    }

    private void report(Exception error) {
        JOptionPane.showMessageDialog(
                window,
                error.getMessage(),
                "Operation could not be completed",
                JOptionPane.ERROR_MESSAGE);
    }

    public void showRoster() {
        GrimoirePanel previous = active;
        UUID id = activeId;
        CharacterState state = previous == null ? null : previous.snapshot();
        boolean locked = previous != null && previous.isSaveLocked();
        run(
                () -> {
                    saveBeforeLeaving(id, state, locked);
                    return repository.list();
                },
                entries -> {
                    if (state != null)
                        AppStyle.appearance(state.play().dark(), state.play().textSize());
                    roster.entries(entries);
                    if (previous != null) previous.dispose();
                    active = null;
                    activeId = null;
                    window.setContentPane(roster);
                    window.setTitle("DnD Companion · Characters (2024)");
                });
    }

    private void saveBeforeLeaving(UUID id, CharacterState state, boolean locked) throws Exception {
        if (id == null) return;
        if (!locked) repository.save(id, state, false);
        repository.preferences(new Preferences(state.play().dark(), state.play().textSize()));
    }

    @Override
    public void open(UUID id) {
        var previous = active;
        var previousId = activeId;
        var outgoing = previous == null ? null : previous.snapshot();
        boolean locked = previous != null && previous.isSaveLocked();
        run(
                () -> {
                    saveBeforeLeaving(previousId, outgoing, locked);
                    Entry entry = repository.load(id);
                    Preferences prefs = repository.preferences();
                    CharacterState state = appearance(entry.state(), prefs);
                    return new Entry(
                            entry.id(),
                            state,
                            entry.locked(),
                            entry.archived(),
                            entry.campaign(),
                            entry.created(),
                            entry.updated(),
                            entry.lastPlayed());
                },
                entry -> {
                    GrimoirePanel panel = new GrimoirePanel(entry.state());
                    panel.setSaveLocked(entry.locked());
                    panel.setRosterAction(this::showRoster);
                    panel.setSaveActions(
                            lock -> {
                                CharacterState snapshot = panel.snapshot();
                                run(
                                        () -> {
                                            if (lock) repository.save(id, snapshot, true);
                                            else repository.lock(id, false);
                                            return true;
                                        },
                                        done -> {
                                            if (active == panel && lock) panel.markSaved();
                                        },
                                        error -> {
                                            panel.setSaveLocked(!lock);
                                            report(error);
                                        });
                            },
                            () ->
                                    run(
                                            () -> repository.load(id),
                                            saved -> {
                                                if (active == panel) {
                                                    panel.restore(
                                                            appearance(
                                                                    saved.state(),
                                                                    new Preferences(
                                                                            panel.snapshot()
                                                                                    .play()
                                                                                    .dark(),
                                                                            panel.snapshot()
                                                                                    .play()
                                                                                    .textSize())));
                                                    panel.setSaveLocked(saved.locked());
                                                }
                                            }));
                    panel.setSaveAction(
                            state ->
                                    run(
                                            () -> {
                                                repository.save(id, state, false);
                                                return true;
                                            },
                                            done -> {
                                                if (active == panel) panel.markSaved();
                                            }));
                    if (previous != null) previous.dispose();
                    active = panel;
                    activeId = id;
                    window.setContentPane(panel);
                    window.setTitle(entry.state().name() + " · DnD Companion (2024)");
                    run(
                            () -> {
                                repository.played(id);
                                return true;
                            },
                            done -> {});
                });
    }

    public static CharacterState appearance(CharacterState state, Preferences preferences) {
        return new CharacterState(
                state.level(),
                state.charisma(),
                state.subclass(),
                state.chosenSpells(),
                state.species(),
                state.play().withAppearance(preferences.dark(), preferences.textSize()),
                state.name());
    }

    public static CharacterState rename(CharacterState state, String name) {
        return new CharacterState(
                state.level(),
                state.charisma(),
                state.subclass(),
                state.chosenSpells(),
                state.species(),
                state.play(),
                name);
    }

    @Override
    public void create() {
        JTextField name = new JTextField("New character", 22);
        JComboBox<ClassId> classes =
                new JComboBox<>(new ClassId[] {ClassId.SORCERER, ClassId.FIGHTER});
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Name"));
        form.add(name);
        form.add(new JLabel("PHB 2024 class"));
        form.add(classes);
        form.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 12, 8, 12));
        String create = "Create character";
        JOptionPane prompt =
                new JOptionPane(
                        form,
                        JOptionPane.PLAIN_MESSAGE,
                        JOptionPane.OK_CANCEL_OPTION,
                        null,
                        new String[] {create, "Cancel"},
                        create);
        JDialog dialog = prompt.createDialog(window, create);
        // Apply the actual fonts and button padding before measuring the window.
        AppStyle.applyAppearance(dialog);
        dialog.setResizable(true);
        dialog.pack();
        Dimension fitted = dialog.getSize();
        dialog.setMinimumSize(
                new Dimension(Math.max(560, fitted.width), Math.max(260, fitted.height)));
        dialog.setSize(dialog.getMinimumSize());
        dialog.setLocationRelativeTo(window);
        try {
            dialog.setVisible(true);
        } finally {
            dialog.dispose();
        }
        if (!create.equals(prompt.getValue())) return;
        try {
            CharacterState state =
                    CharacterState.create(name.getText(), (ClassId) classes.getSelectedItem());
            run(() -> repository.create(state), this::open);
        } catch (RuntimeException error) {
            report(error);
        }
    }

    @Override
    public void copy(Entry entry) {
        String name = entry.state().name();
        name = name.substring(0, Math.min(name.length(), 55)) + " Copy";
        CharacterState copied = rename(entry.state(), name);
        run(() -> repository.create(copied), id -> showRoster());
    }

    @Override
    public void rename(Entry entry) {
        String name = JOptionPane.showInputDialog(window, "Character name", entry.state().name());
        if (name == null) return;
        try {
            CharacterState state = rename(entry.state(), name);
            run(
                    () -> {
                        repository.save(entry.id(), state, entry.locked());
                        return true;
                    },
                    done -> showRoster());
        } catch (RuntimeException error) {
            report(error);
        }
    }

    @Override
    public void campaign(Entry entry) {
        String name =
                JOptionPane.showInputDialog(
                        window, "Campaign (leave empty for none)", entry.campaign());
        if (name != null)
            run(
                    () -> {
                        repository.metadata(entry.id(), entry.archived(), name);
                        return true;
                    },
                    done -> showRoster());
    }

    @Override
    public void archive(Entry entry) {
        run(
                () -> {
                    repository.metadata(entry.id(), !entry.archived(), entry.campaign());
                    return true;
                },
                done -> showRoster());
    }

    @Override
    public void delete(Entry entry) {
        if (JOptionPane.showConfirmDialog(
                        window,
                        "Permanently delete “" + entry.state().name() + "”? This cannot be undone.",
                        "Delete character",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE)
                != JOptionPane.YES_OPTION) return;
        run(
                () -> {
                    repository.delete(entry.id());
                    return true;
                },
                done -> showRoster());
    }

    @Override
    public void importFile() {
        JFileChooser chooser = chooser();
        if (chooser.showOpenDialog(window) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath();
        run(
                () -> {
                    if (Files.size(path) > 4_000_000)
                        throw new IllegalArgumentException("Character file is too large.");
                    CharacterState state =
                            new CharacterJson()
                                    .decode(Files.readString(path, StandardCharsets.UTF_8));
                    return repository.create(state);
                },
                id -> showRoster());
    }

    @Override
    public void exportFile(Entry entry) {
        JFileChooser chooser = chooser();
        chooser.setSelectedFile(
                new java.io.File(
                        entry.state().name().replaceAll("[^\\p{L}\\p{N} _-]", "_") + ".json"));
        if (chooser.showSaveDialog(window) != JFileChooser.APPROVE_OPTION) return;
        Path path = chooser.getSelectedFile().toPath().toAbsolutePath();
        run(
                () -> Files.exists(path),
                exists -> {
                    if (exists
                            && JOptionPane.showConfirmDialog(
                                            window,
                                            "Replace existing file?",
                                            "Export character",
                                            JOptionPane.YES_NO_OPTION)
                                    != JOptionPane.YES_OPTION) return;
                    writeExport(path, entry);
                });
    }

    private void writeExport(Path path, Entry entry) {
        run(
                () -> {
                    Path temporary =
                            Files.createTempFile(path.getParent(), ".character-export-", ".json");
                    try {
                        Files.writeString(
                                temporary,
                                new CharacterJson()
                                        .encode(
                                                appearance(
                                                        entry.state(), new Preferences(false, 14))),
                                StandardCharsets.UTF_8);
                        try {
                            Files.move(
                                    temporary,
                                    path,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                        } catch (java.nio.file.AtomicMoveNotSupportedException error) {
                            Files.move(
                                    temporary,
                                    path,
                                    java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
                    } finally {
                        Files.deleteIfExists(temporary);
                    }
                    return true;
                },
                done -> {});
    }

    private JFileChooser chooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("DnD Companion character JSON", "json"));
        return chooser;
    }

    @Override
    public void preferences() {
        JCheckBox dark = new JCheckBox("Dark theme", AppStyle.isDark());
        JSpinner size = new JSpinner(new SpinnerNumberModel(AppStyle.textSize(), 12, 18, 1));
        JPanel form = new JPanel(new java.awt.GridLayout(0, 2, 8, 8));
        form.add(dark);
        form.add(new JLabel());
        form.add(new JLabel("Text size"));
        form.add(size);
        if (JOptionPane.showConfirmDialog(
                        window, form, "Global preferences", JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        Preferences preferences = new Preferences(dark.isSelected(), (Integer) size.getValue());
        run(
                () -> {
                    repository.preferences(preferences);
                    return true;
                },
                done -> {
                    AppStyle.appearance(preferences.dark(), preferences.textSize());
                    AppStyle.applyAppearance(roster);
                });
    }

    @Override
    public void exit() {
        if (busy || closed) return;
        if (repository == null) {
            window.dispose();
            close();
            return;
        }
        UUID id = activeId;
        CharacterState state = active == null ? null : active.snapshot();
        boolean locked = active != null && active.isSaveLocked();
        run(
                () -> {
                    saveBeforeLeaving(id, state, locked);
                    return true;
                },
                done -> {
                    window.dispose();
                    close();
                });
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager()
                .removeKeyEventDispatcher(busyKeys);
        if (active != null) active.dispose();
        io.shutdown();
    }
}
