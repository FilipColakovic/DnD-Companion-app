package org.example.ui.character;

import org.example.domain.character.SorcererRules;
import org.example.domain.character.CharacterState;
import org.example.domain.character.Species;
import org.example.domain.character.PlaySession;
import org.example.domain.character.FeatCatalog;
import org.example.ui.spells.CastingPanel;
import org.example.ui.common.PillSelector;
import org.example.domain.spells.SpellSelection;
import org.example.ui.common.AppStyle;
import org.example.ui.common.SpellBookIcon;
import org.example.ui.spells.SpellBookPanel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicSliderUI;
import java.awt.*;

import static org.example.ui.common.AppStyle.LINE;
import static org.example.ui.common.AppStyle.MUTED;
import static org.example.ui.common.AppStyle.label;
import static org.example.ui.common.AppStyle.section;

/** Reusable panel; each input change refreshes one rules snapshot. */
public final class SorcererPanel extends JPanel {
    private final JSlider level = new JSlider(1, 20, 1);
    private final JLabel levelValue = label("1", 21, false);
    private final JLabel charismaValue = label("17", 21, false);
    private final JButton minus = new JButton("−");
    private final JButton plus = new JButton("+");
    private final SubclassSelector subclassButton = new SubclassSelector(this::selectSubclass);
    private SorcererRules.Subclass subclass = SorcererRules.Subclass.ABERRANT;
    private Species species = Species.HUMAN;
    private final PillSelector<Species> speciesButton = new PillSelector<>(
            java.util.List.of(Species.values()), Species.HUMAN, this::selectSpecies);
    private final JLabel saveDc = label("", 23, true);
    private final JLabel attack = label("", 23, true);
    private final JPanel pointTracker = section(new BorderLayout());
    private final JLabel cantrips = label("", 23, true);
    private final JLabel prepared = label("", 23, true);
    private final JLabel totalSlots = label("", 23, true);
    private final JPanel slotGrid = section(new GridLayout(0, 3, 24, 12));
    private final JPanel featInfo = AppStyle.card(new BorderLayout(0,10));
    private JDialog featsWindow;
    private int charisma = 17;
    private final SpellSelection spellSelection = new SpellSelection();
    private final PlaySession session = new PlaySession(spellSelection);
    private final JLabel saveStatus = label("Saved", 13, false);
    private final JLabel restStatus = label("", 13, false);
    private final JCheckBox darkMode = new JCheckBox("Dark theme");
    private final JComboBox<Integer> textSize = new JComboBox<>(new Integer[]{12,14,16,18});
    private CharacterState savedSnapshot;
    private JDialog selectedWindow, metamagicWindow, castingWindow;
    private JDialog spellWindow;
    private SpellBookPanel spellBook;
    private final JCheckBox saveLock = new JCheckBox("Lock saved character");
    private final JButton restoreSaved = new JButton("Restore saved");
    private java.util.function.Consumer<Boolean> saveLockHandler = locked -> { };
    private Runnable restoreHandler = () -> { };
    private boolean restoring;
    private boolean embedded;
    private String characterName;

    public SorcererPanel() {
        this(CharacterState.defaults());
    }

    public SorcererPanel(CharacterState initial) {
        super(new BorderLayout(0, 14));
        charisma = initial.charisma();
        characterName = initial.name();
        subclass = initial.subclass();
        species = initial.species();
        level.setValue(initial.level());
        spellSelection.restoreChoices(initial.chosenSpells());
        session.restore(initial.play());
        savedSnapshot = initial;
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 16, 20, 16));
        JPanel toolbar = section(new GridLayout(0, 1, 0, 8));
        JPanel saveRow = section(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        AppStyle.roundButton(saveLock);
        saveLock.setBorderPainted(true);
        saveLock.setOpaque(false);
        saveLock.setToolTipText("Save this character now and protect it while you experiment");
        saveLock.addActionListener(event -> {
            setSaveLocked(saveLock.isSelected());
            saveLockHandler.accept(saveLock.isSelected());
        });
        AppStyle.roundButton(restoreSaved);
        restoreSaved.setEnabled(false);
        restoreSaved.setToolTipText("Discard experiments and return to the saved character");
        restoreSaved.addActionListener(event -> restoreHandler.run());
        saveRow.add(saveStatus);
        saveRow.add(saveLock);
        saveRow.add(restoreSaved);
        toolbar.add(saveRow);
        JPanel actionRow = section(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        JButton spellsButton = new JButton("Spells", new SpellBookIcon());
        spellsButton.setFont(new Font("SansSerif", Font.PLAIN, 16));
        spellsButton.setBackground(Color.WHITE);
        AppStyle.roundButton(spellsButton);
        spellsButton.setToolTipText("Open the Sorcerer spell list and selector");
        spellsButton.getAccessibleContext().setAccessibleName("Open Sorcerer spellbook");
        spellsButton.addActionListener(event -> openSpellBook());
        actionRow.add(spellsButton);
        actionRow.add(AppStyle.button("Selected spells", () -> {
            if (selectedWindow == null || !selectedWindow.isDisplayable()) {
                selectedWindow = AppStyle.openWindow(this, "Prepared spells", SpellBookPanel.knownSpells(spellSelection));
            } else { selectedWindow.setVisible(true); selectedWindow.toFront(); }
        }));
        actionRow.add(AppStyle.button("Metamagic", () -> {
            if (metamagicWindow == null || !metamagicWindow.isDisplayable()) {
                metamagicWindow = AppStyle.openWindow(this, "Metamagic", new MetamagicPanel(session));
            } else { metamagicWindow.setVisible(true); metamagicWindow.toFront(); }
        }));
        actionRow.add(AppStyle.button("Cast spell", () -> {
            if (castingWindow == null || !castingWindow.isDisplayable()) {
                castingWindow = AppStyle.openWindow(this, "Cast a spell", new CastingPanel(session, () -> charisma));
            } else { castingWindow.setVisible(true); castingWindow.toFront(); }
        }));
        toolbar.add(actionRow);
        JPanel appearanceRow = section(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        AppStyle.roundButton(darkMode);
        darkMode.setSelected(initial.play().dark());
        textSize.setSelectedItem(initial.play().textSize());
        darkMode.addActionListener(e -> session.appearance(darkMode.isSelected(), (Integer) textSize.getSelectedItem()));
        textSize.addActionListener(e -> { if (!restoring) { session.appearance(darkMode.isSelected(), (Integer) textSize.getSelectedItem()); } });
        appearanceRow.add(darkMode); appearanceRow.add(label("Text size", 13, false)); appearanceRow.add(textSize);
        toolbar.add(appearanceRow);
        add(toolbar, BorderLayout.NORTH);
        JPanel card = AppStyle.card(new BorderLayout(0, 16));
        card.setBorder(new EmptyBorder(22, 16, 18, 16));
        JPanel inputs = section(new BorderLayout(0, 14));
        JPanel levelRow = section(new BorderLayout(30, 0));
        JPanel levelText = section(new GridLayout(2, 1, 0, 8));
        levelText.add(label("Sorcerer level", 18, true));
        levelText.add(levelValue);
        levelRow.add(levelText, BorderLayout.WEST);
        level.setOpaque(false);
        level.setPreferredSize(new Dimension(480, 40));
        level.setSnapToTicks(true);
        level.setMinorTickSpacing(1);
        level.setToolTipText("Sorcerer level: 1–20 (arrow keys also work)");
        level.getAccessibleContext().setAccessibleName("Sorcerer level");
        level.setUI(new BasicSliderUI(level) {
            @Override public void paintFocus(Graphics graphics) { }
            @Override protected Dimension getThumbSize() { return new Dimension(24, 24); }
            @Override public void paintTrack(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(AppStyle.SOFT);
                g.fillRect(trackRect.x,trackRect.y+(trackRect.height-18)/2,trackRect.width,18);
                g.setColor(AppStyle.LINE);g.drawRect(trackRect.x,trackRect.y+(trackRect.height-18)/2,trackRect.width,18);
                g.dispose();
            }
            @Override public void paintThumb(Graphics graphics) {
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(new Color(0xffcc00));
                g.fillRect(thumbRect.x,thumbRect.y,23,23);
                g.setColor(LINE);
                g.drawRect(thumbRect.x,thumbRect.y,23,23);
                g.dispose();
            }
        });
        levelRow.add(level, BorderLayout.CENTER);
        inputs.add(levelRow, BorderLayout.NORTH);
        JPanel charismaRow = section(new BorderLayout());
        charismaRow.add(label("Charisma score", 18, true), BorderLayout.WEST);
        JPanel stepper = section(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        configureButton(minus, "Decrease Charisma");
        configureButton(plus, "Increase Charisma");
        charismaValue.setHorizontalAlignment(SwingConstants.CENTER);
        charismaValue.setPreferredSize(new Dimension(30, 32));
        stepper.add(minus);
        stepper.add(charismaValue);
        stepper.add(plus);
        charismaRow.add(stepper, BorderLayout.EAST);
        inputs.add(charismaRow, BorderLayout.CENTER);
        JPanel subclassRow = section(new BorderLayout(12, 8));
        subclassRow.add(label("Subclass", 18, true), BorderLayout.WEST);
        subclassRow.add(subclassButton, BorderLayout.EAST);
        JPanel originRows = section(new GridLayout(2, 1, 0, 14));
        originRows.add(subclassRow);
        JPanel speciesRow = section(new BorderLayout(12, 8));
        speciesRow.add(label("Race / species", 18, true), BorderLayout.WEST);
        speciesButton.getAccessibleContext().setAccessibleName("Select PHB 2024 species and lineage");
        speciesButton.setToolTipText("Choose a PHB 2024 species; lineages determine automatic spell grants");
        speciesRow.add(speciesButton, BorderLayout.EAST);
        originRows.add(speciesRow);
        inputs.add(originRows, BorderLayout.SOUTH);
        JPanel upper = section(new BorderLayout(0, 20));
        upper.add(inputs, BorderLayout.CENTER);
        JSeparator separator = new JSeparator();
        separator.setForeground(LINE);
        upper.add(separator, BorderLayout.SOUTH);
        card.add(upper, BorderLayout.NORTH);
        JPanel body = section(new BorderLayout(0, 16));
        JPanel stats = section(new GridLayout(2, 3, 24, 14));
        stats.add(stat("Spell Save DC", saveDc));
        stats.add(stat("Spell attack", attack));
        stats.add(pointTracker);
        stats.add(stat("Cantrips", cantrips));
        stats.add(stat("Prepared spells", prepared));
        stats.add(stat("Total spell slots", totalSlots));
        body.add(stats, BorderLayout.NORTH);
        JPanel slots = section(new BorderLayout(0, 12));
        slots.add(label("Spell slots by level", 18, true), BorderLayout.NORTH);
        slots.add(slotGrid, BorderLayout.CENTER);
        body.add(slots, BorderLayout.CENTER);
        JPanel resources = section(new BorderLayout(0, 8));
        JPanel actions = section(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.add(AppStyle.button("Short Rest", () -> restStatus.setText(session.shortRest())));
        actions.add(AppStyle.button("Long Rest", () -> {
            session.longRest(); restStatus.setText("Long Rest: slots, points, and daily uses restored.");
        }));
        actions.add(AppStyle.button("Innate Sorcery", () -> {
            String error = session.toggleInnate();
            restStatus.setText(error.isEmpty() ? "Innate Sorcery " + (session.innateActive() ? "active (1 minute; end manually)." : "ended.") : error);
        }));
        resources.add(actions, BorderLayout.NORTH);
        resources.add(restStatus, BorderLayout.SOUTH);
        body.add(resources, BorderLayout.SOUTH);
        card.add(body, BorderLayout.CENTER);
        add(card, BorderLayout.CENTER);
        featInfo.setName("character-feats"); add(featInfo,BorderLayout.SOUTH);
        level.addChangeListener(event -> refresh());
        minus.addActionListener(event -> { charisma = Math.max(1, charisma - 1); refresh(); session.characterChanged(); });
        plus.addActionListener(event -> { charisma = Math.min(30, charisma + 1); refresh(); session.characterChanged(); });
        session.addListener(() -> {
            if (!restoring) {
                restoring = true;
                try {
                    darkMode.setSelected(session.snapshot().dark());
                    textSize.setSelectedItem(session.snapshot().textSize());
                } finally { restoring = false; }
                AppStyle.appearance(session.snapshot().dark(), session.snapshot().textSize());
                refresh();
            }
        });
        spellSelection.addChangeListener(this::updateSaveStatus);
        AppStyle.appearance(initial.play().dark(), initial.play().textSize());
        refresh();
    }

    private void refresh() {
        if (restoring) { return; }
        SorcererRules.Stats stats = SorcererRules.calculate(level.getValue(), charisma, subclass);
        spellSelection.setCharacter(level.getValue(), subclass, species);
        speciesButton.setSelectedOption(species);
        subclassButton.setSubclass(subclass);
        levelValue.setText(Integer.toString(level.getValue()));
        charismaValue.setText(Integer.toString(charisma));
        minus.setEnabled(charisma > 1);
        plus.setEnabled(charisma < 30);
        saveDc.setText(Integer.toString(stats.spellSaveDc()));
        saveDc.setToolTipText("8 + proficiency (" + stats.proficiencyBonus() + ") + Charisma modifier (" + stats.charismaModifier() + ")");
        attack.setText(String.format("%+d", stats.spellAttack()));
        attack.setToolTipText("Proficiency bonus + Charisma modifier");
        pointTracker.removeAll();
        pointTracker.add(resource("Sorcery Points remaining", session.remainingPoints(), session.maxPoints(), session::setRemainingPoints));
        cantrips.setText(Integer.toString(stats.cantrips() + spellSelection.speciesCount(true)));
        cantrips.setToolTipText("Includes class and subclass cantrips plus " + spellSelection.speciesCount(true)
                + " additional species cantrips; assumes class choices do not overlap automatic grants.");
        prepared.setText(Integer.toString(stats.preparedSpells() + spellSelection.speciesCount(false)));
        prepared.setToolTipText(stats.basePreparedSpells() + " class spells + " + stats.subclassPreparedSpells()
                + " subclass spells + " + spellSelection.speciesCount(false) + " additional species spells");
        totalSlots.setText(Integer.toString(stats.totalSpellSlots()));
        slotGrid.removeAll();
        for (int i = 0; i < stats.spellSlots().size(); i++) {
            int count = stats.spellSlots().get(i);
            if (count > 0) {
                int slot = i + 1;
                slotGrid.add(resource("Level " + slot + " slots", session.remainingSlots(slot), count,
                        value -> session.setRemainingSlots(slot, value)));
            }
        }
        // Keep the three-column layout even when only one or two slot levels are unlocked.
        int cells = ((slotGrid.getComponentCount() + 2) / 3) * 3;
        while (slotGrid.getComponentCount() < cells) { slotGrid.add(section(new BorderLayout())); }
        featInfo.removeAll();
        JPanel featHeading=section(new BorderLayout(10,0));
        featHeading.add(label("Selected feats",18,true),BorderLayout.CENTER);
        featHeading.add(AppStyle.button("Manage feats",() -> openFeats(null)),BorderLayout.EAST);
        featInfo.add(featHeading,BorderLayout.NORTH);
        JPanel selectedFeats=section(new GridLayout(0,1,0,8));
        session.feats().keySet().stream().sorted().forEach(name -> {
            FeatCatalog.Feat feat=FeatCatalog.named(name);
            JButton button=AppStyle.button(name+" · "+feat.category(),() -> openFeats(name));
            button.setToolTipText(feat.summary()); selectedFeats.add(button);
        });
        if(session.feats().isEmpty())selectedFeats.add(label("No feats selected",13,false));
        featInfo.add(selectedFeats,BorderLayout.CENTER);
        updateSaveStatus();
        AppStyle.applyAppearance(this);
        slotGrid.revalidate();
        slotGrid.repaint();
        revalidate();
        fitWindowToContent();
    }

    private void openSpellBook() {
        if (spellWindow == null || !spellWindow.isDisplayable()) {
            spellBook = createSpellBook();
            SpellBookPanel openedBook = spellBook;
            spellWindow = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Sorcerer spellbook · 2024 Player's Handbook", Dialog.ModalityType.MODELESS);
            spellWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            spellWindow.setContentPane(spellBook);
            spellWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent event) {
                    openedBook.dispose();
                }
            });
            spellWindow.setSize(1000, 740);
            spellWindow.setMinimumSize(new Dimension(900, 600));
            spellWindow.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        }
        spellBook.refresh();
        AppStyle.applyAppearance(spellWindow);
        spellWindow.setVisible(true);
        spellWindow.toFront();
    }

    public SpellBookPanel createSpellBook() { return new SpellBookPanel(spellSelection); }

    public CharacterState snapshot() {
        return new CharacterState(level.getValue(), charisma, subclass, spellSelection.chosenSpells(), species, session.snapshot(), characterName);
    }

    public boolean isSaveLocked() { return saveLock.isSelected(); }

    public void setSaveLocked(boolean locked) {
        saveLock.setSelected(locked);
        restoreSaved.setEnabled(locked);
        updateSaveStatus();
        saveLock.setToolTipText(locked
                ? "Saved character is protected. Experiments will not be saved on exit."
                : "Save this character now and protect it while you experiment");
    }

    public void setSaveActions(java.util.function.Consumer<Boolean> onLock, Runnable onRestore) {
        saveLockHandler = java.util.Objects.requireNonNull(onLock);
        restoreHandler = java.util.Objects.requireNonNull(onRestore);
    }

    public void restore(CharacterState state) {
        restoring = true;
        try {
            charisma = state.charisma();
            characterName = state.name();
            subclass = state.subclass();
            species = state.species();
            level.setValue(state.level());
            spellSelection.setCharacter(state.level(), state.subclass(), state.species());
            spellSelection.restoreChoices(state.chosenSpells());
            session.restore(state.play());
            darkMode.setSelected(state.play().dark());
            textSize.setSelectedItem(state.play().textSize());
            savedSnapshot = state;
        } finally { restoring = false; }
        AppStyle.appearance(state.play().dark(), state.play().textSize());
        restStatus.setText("");
        refresh();
        firePropertyChange("characterState", null, snapshot());
    }

    public void selectSpecies(Species selected) {
        species = java.util.Objects.requireNonNull(selected);
        refresh();
    }

    public void selectSubclass(SorcererRules.Subclass selected) {
        subclass = java.util.Objects.requireNonNull(selected);
        refresh();
    }

    /** Grow the frame and its minimum size to fit all unlocked slot levels. */
    public void fitWindowToContent() {
        if (embedded) { return; }
        Window window = SwingUtilities.getWindowAncestor(this);
        if (window == null) { return; }
        Dimension content = getPreferredSize();
        Insets insets = window.getInsets();
        Dimension required = new Dimension(Math.max(920, content.width + insets.left + insets.right),
                Math.max(720, content.height + insets.top + insets.bottom));
        window.setMinimumSize(required);
        window.setSize(Math.max(window.getWidth(), required.width), Math.max(window.getHeight(), required.height));
    }

    private static JPanel stat(String title, JLabel value) {
        JPanel panel = section(new BorderLayout(0, 8));
        JLabel caption = label(title, 13, false);
        caption.setForeground(MUTED);
        panel.add(caption, BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }
    public PlaySession playSession() { return session; }
    public void setLevel(int value) {
        if(value<1 || value>20)throw new IllegalArgumentException("Sorcerer level must be 1–20");
        level.setValue(value);
    }
    public void setCharismaScore(int value) {
        if(value<1 || value>30)throw new IllegalArgumentException("Charisma must be 1–30");
        if(charisma==value)return;charisma=value;refresh();session.characterChanged();
    }
    private void openFeats(String name) {
        FeatsPanel panel;
        if(featsWindow==null || !featsWindow.isDisplayable()) {
            panel=new FeatsPanel(session,this::snapshot); featsWindow=AppStyle.openWindow(this,"Feats · PHB 2024",panel);
        } else { panel=(FeatsPanel)AppStyle.windowContent(featsWindow); featsWindow.setVisible(true); featsWindow.toFront(); }
        if(name!=null)panel.inspect(name);
    }
    public String characterName() { return characterName; }
    public void setCharacterName(String name) {
        String value = java.util.Objects.requireNonNull(name).strip();
        if (value.isEmpty() || value.length() > 60) { throw new IllegalArgumentException("Character name must contain 1–60 characters."); }
        characterName = value; updateSaveStatus(); firePropertyChange("characterState", null, snapshot());
    }
    public void setEmbedded(boolean embedded) { this.embedded = embedded; }
    public String saveStatusText() { return saveStatus.getText(); }
    public void requestSaveLock(boolean locked) { setSaveLocked(locked); saveLockHandler.accept(locked); firePropertyChange("saveStatus", null, saveStatus.getText()); }
    public void restoreSaved() { restoreHandler.run(); }
    public void markSaved() { savedSnapshot = snapshot(); updateSaveStatus(); }
    private void updateSaveStatus() {
        if (restoring || savedSnapshot == null) { return; }
        boolean changed = !snapshot().equals(savedSnapshot);
        saveStatus.setText(isSaveLocked() ? (changed ? "Experimenting · save protected" : "Saved · protected")
                : (changed ? "Changes will save on exit" : "Saved"));
        firePropertyChange("saveStatus", null, saveStatus.getText());
    }
    private static JPanel resource(String title, int remaining, int maximum, java.util.function.IntConsumer change) {
        JPanel panel = section(new BorderLayout(0, 6));
        panel.add(label(title, 13, false), BorderLayout.NORTH);
        JPanel row = section(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton less = AppStyle.button("−", () -> change.accept(remaining - 1));
        JButton more = AppStyle.button("+", () -> change.accept(remaining + 1));
        less.setEnabled(remaining > 0); more.setEnabled(remaining < maximum);
        less.getAccessibleContext().setAccessibleName("Spend " + title);
        more.getAccessibleContext().setAccessibleName("Restore " + title);
        row.add(less); row.add(label(remaining + " / " + maximum, 18, true)); row.add(more);
        panel.add(row, BorderLayout.CENTER); return panel;
    }
    private static void configureButton(JButton button, String name) {
        AppStyle.roundButton(button);
        button.setFont(new Font("SansSerif", Font.PLAIN, 20));
        button.setPreferredSize(new Dimension(50, 32));
        button.setBackground(Color.WHITE);
        button.setForeground(MUTED);
        button.setContentAreaFilled(false);
        button.setBorder(new AppStyle.PillBorder(new Insets(3, 8, 3, 8)));
        button.getAccessibleContext().setAccessibleName(name);
        button.setToolTipText(name + " (1–30)");
    }
}
