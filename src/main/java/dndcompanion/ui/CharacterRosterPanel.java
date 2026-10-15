package dndcompanion.ui;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.ClassId;
import dndcompanion.persistence.CharacterRepository.Entry;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Offline roster. All persistence and application lifecycle actions belong to its controller. */
public final class CharacterRosterPanel extends JPanel {
    public interface Actions {
        void create();

        void open(UUID id);

        void copy(Entry entry);

        void exportFile(Entry entry);

        void importFile();

        void rename(Entry entry);

        void campaign(Entry entry);

        void archive(Entry entry);

        void delete(Entry entry);

        void preferences();

        void exit();
    }

    private List<Entry> entries = List.of();
    private final JTextField search = new JTextField();
    private final JComboBox<String> campaign = new JComboBox<>(new String[] {"All campaigns"});
    private final JPanel cards = AppStyle.section(new GridBagLayout());
    private final JLabel storage = AppStyle.mono("OFFLINE", 11);
    private final JButton all, recent, archived;
    private String filter = "All";
    private boolean updating;
    private final Actions actions;

    public CharacterRosterPanel(Actions actions) {
        super(new BorderLayout(0, 14));
        this.actions = actions;
        setName("character-roster");
        putClientProperty("surfaceRole", "background");
        setBorder(new EmptyBorder(18, 18, 18, 18));
        JPanel top = AppStyle.section(new BorderLayout(0, 12));
        top.add(AppStyle.heading("D&D 2024 // CHARACTER SELECTION", 20), BorderLayout.NORTH);
        JPanel create = AppStyle.section(new BorderLayout(12, 0));
        JButton newCharacter = AppStyle.button("+  NEW CHARACTER", actions::create);
        newCharacter.setHorizontalAlignment(JButton.LEFT);
        newCharacter.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 18));
        newCharacter.putClientProperty("tactileRole", "yellow");
        newCharacter.setPreferredSize(new Dimension(400, 62));
        create.add(newCharacter);
        JButton importButton = AppStyle.button("IMPORT CHARACTER JSON", actions::importFile);
        importButton.putClientProperty("tactileRole", "blue");
        create.add(importButton, BorderLayout.EAST);
        top.add(create);
        JPanel filters = AppStyle.card(new BorderLayout(10, 8));
        search.getAccessibleContext().setAccessibleName("Search characters");
        search.setToolTipText("Search name, class, species, background or campaign");
        JPanel query = AppStyle.section(new BorderLayout(8, 0));
        query.add(AppStyle.mono("FILTER", 11), BorderLayout.WEST);
        query.add(search);
        filters.add(query);
        JPanel modes = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        all = AppStyle.compactButton("All", () -> setFilter("All"));
        recent = AppStyle.compactButton("Last played", () -> setFilter("Last played"));
        archived = AppStyle.compactButton("Archived", () -> setFilter("Archived"));
        modes.add(all);
        modes.add(recent);
        modes.add(archived);
        modes.add(campaign);
        filters.add(modes, BorderLayout.SOUTH);
        top.add(filters, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH);
        JPanel list = AppStyle.section(new BorderLayout());
        list.add(cards, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(list);
        AppStyle.scrollPane(scroll);
        scroll.getViewport().putClientProperty("surfaceRole", "background");
        add(scroll);
        JPanel footer = AppStyle.card(new BorderLayout(10, 0));
        footer.add(storage);
        JPanel controls = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        controls.add(AppStyle.compactButton("Global preferences", actions::preferences));
        controls.add(AppStyle.compactButton("Exit", actions::exit));
        footer.add(controls, BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                render();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                render();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                render();
                            }
                        });
        campaign.addActionListener(
                event -> {
                    if (!updating) render();
                });
        cards.addComponentListener(
                new java.awt.event.ComponentAdapter() {
                    private int columns;

                    @Override
                    public void componentResized(java.awt.event.ComponentEvent event) {
                        int next = cards.getWidth() < columnThreshold() ? 1 : 2;
                        if (next != columns) {
                            columns = next;
                            render();
                        }
                    }
                });
    }

    public void entries(List<Entry> next) {
        entries = List.copyOf(next);
        updating = true;
        String previous = (String) campaign.getSelectedItem();
        campaign.removeAllItems();
        campaign.addItem("All campaigns");
        next.stream()
                .map(Entry::campaign)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .forEach(campaign::addItem);
        if (previous != null) campaign.setSelectedItem(previous);
        if (campaign.getSelectedIndex() < 0) campaign.setSelectedIndex(0);
        updating = false;
        render();
    }

    private void setFilter(String filter) {
        this.filter = filter;
        render();
    }

    private void render() {
        if (updating) return;
        cards.removeAll();
        all.setText("ALL (" + entries.size() + ")");
        archived.setText("ARCHIVED (" + entries.stream().filter(Entry::archived).count() + ")");
        Entry last =
                entries.stream()
                        .filter(e -> e.lastPlayed() != null)
                        .max(java.util.Comparator.comparing(Entry::lastPlayed))
                        .orElse(null);
        recent.setText("LAST PLAYED (" + (last == null ? 0 : 1) + ")");
        all.putClientProperty("tactileRole", filter.equals("All") ? "yellow" : "neutral");
        recent.putClientProperty(
                "tactileRole", filter.equals("Last played") ? "yellow" : "neutral");
        archived.putClientProperty("tactileRole", filter.equals("Archived") ? "yellow" : "neutral");
        String term = search.getText().strip().toLowerCase(Locale.ROOT);
        List<Entry> visible =
                entries.stream()
                        .filter(e -> !filter.equals("Archived") || e.archived())
                        .filter(e -> !filter.equals("Last played") || e.equals(last))
                        .filter(
                                e ->
                                        campaign.getSelectedIndex() <= 0
                                                || e.campaign().equals(campaign.getSelectedItem()))
                        .filter(
                                e ->
                                        (e.state().name()
                                                        + " "
                                                        + e.state().classId()
                                                        + " "
                                                        + e.state().species()
                                                        + " "
                                                        + background(e)
                                                        + " "
                                                        + e.campaign())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(term))
                        .toList();
        int columns = cards.getWidth() >= columnThreshold() ? 2 : 1;
        for (int index = 0; index < visible.size(); index++) {
            Entry entry = visible.get(index);
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = index % columns;
            c.gridy = index / columns;
            c.weightx = 1;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(0, 0, 12, c.gridx == 0 && columns == 2 ? 12 : 0);
            cards.add(card(entry, entry.equals(last)), c);
        }
        if (visible.isEmpty())
            cards.add(
                    AppStyle.label(
                            entries.isEmpty()
                                    ? "No characters yet. Create your first character or import a"
                                            + " character JSON file."
                                    : "No matching characters.",
                            14,
                            false));
        storage.setText("OFFLINE  //  " + entries.size() + " CHARACTERS STORED");
        AppStyle.applyAppearance(this);
        revalidate();
        repaint();
    }

    private JPanel card(Entry entry, boolean last) {
        JPanel card = AppStyle.card(new BorderLayout(0, 12));
        card.setName("character-card-" + entry.id());
        card.putClientProperty("surfaceRole", "lowest");
        JPanel heading =
                AppStyle.surfacePanel(
                        new BorderLayout(),
                        entry.state().classId() == ClassId.FIGHTER ? "badgeRed" : "slotAvailable");
        heading.setBorder(new EmptyBorder(5, 7, 5, 7));
        JLabel status =
                AppStyle.mono(
                        entry.archived() ? "ARCHIVED" : last ? "LAST PLAYED" : "READY TO PLAY", 11);
        String headerInk =
                entry.state().classId() == ClassId.FIGHTER ? "badgeLightText" : "badgeText";
        AppStyle.setColorRole(status, headerInk);
        JLabel protection = AppStyle.mono(entry.locked() ? "PROTECTED" : "UNLOCKED", 10);
        AppStyle.setColorRole(protection, headerInk);
        heading.add(status);
        heading.add(protection, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        JPanel body = AppStyle.section(new BorderLayout(0, 10));
        JPanel identity = AppStyle.section(new GridLayout(0, 1, 0, 5));
        identity.add(AppStyle.heading(entry.state().name().toUpperCase(Locale.ROOT), 19));
        identity.add(
                AppStyle.mono(
                        "LEVEL "
                                + entry.state().level()
                                + " · "
                                + entry.state().classId().label()
                                + " // "
                                + entry.state().classes().getFirst().subclassLabel()
                                + " · "
                                + entry.state().species(),
                        11));
        identity.add(
                AppStyle.mono(
                        background(entry)
                                + (entry.campaign().isBlank() ? "" : " // " + entry.campaign()),
                        11));
        body.add(identity, BorderLayout.NORTH);
        JPanel stats = AppStyle.section(new GridLayout(1, 4, 6, 0));
        try (var controller = new CharacterController(entry.state())) {
            var session = controller.playSession();
            stats.add(metric("HP", session.hitPoints() + "/" + session.maxHitPoints(), "error"));
            if (session.sorcerer()) {
                stats.add(
                        metric(
                                "SORCERY PTS",
                                session.remainingPoints() + "/" + session.maxPoints(),
                                "accent"));
                var casting =
                        session.stats(
                                session.effectiveScore(
                                        CharacterSheet.Ability.CHA, entry.state().charisma()));
                stats.add(metric("SPELL DC", "" + casting.spellSaveDc(), "ink"));
                stats.add(metric("SPELL ATK", String.format("%+d", casting.spellAttack()), "ink"));
            } else {
                stats.add(
                        metric(
                                "SECOND WIND",
                                session.resource("fighter.secondWind").remaining()
                                        + "/"
                                        + session.resource("fighter.secondWind").maximum(),
                                "accent"));
                stats.add(
                        metric(
                                "ARMOR CLASS",
                                "" + session.armorClass(entry.state().charisma()),
                                "ink"));
                stats.add(
                        metric(
                                "ATTACKS",
                                ""
                                        + dndcompanion.domain.character.FighterRules.attacks(
                                                entry.state().level()),
                                "ink"));
            }
        }
        body.add(stats);
        card.add(body);
        JPanel footer = AppStyle.section(new BorderLayout(6, 0));
        JPanel tools = AppStyle.section(new FlowLayout(FlowLayout.LEFT, 5, 0));
        tools.add(AppStyle.compactButton("Copy", () -> actions.copy(entry)));
        tools.add(AppStyle.compactButton("Export", () -> actions.exportFile(entry)));
        JButton more = AppStyle.compactButton("Manage", () -> {});
        more.addActionListener(
                event -> {
                    JPopupMenu menu = new JPopupMenu();
                    menu.add(item("Rename", () -> actions.rename(entry)));
                    menu.add(item("Campaign", () -> actions.campaign(entry)));
                    menu.add(
                            item(
                                    entry.archived() ? "Restore from archive" : "Archive",
                                    () -> actions.archive(entry)));
                    menu.add(item("Delete permanently", () -> actions.delete(entry)));
                    menu.show(more, 0, more.getHeight());
                });
        tools.add(more);
        footer.add(tools);
        JButton open = AppStyle.compactButton("SELECT & ENTER >>", () -> actions.open(entry.id()));
        open.putClientProperty("tactileRole", "yellow");
        footer.add(open, BorderLayout.EAST);
        card.add(footer, BorderLayout.SOUTH);
        return card;
    }

    private static JMenuItem item(String title, Runnable action) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(e -> action.run());
        return item;
    }

    private static int columnThreshold() {
        return 1000 + Math.max(0, AppStyle.textSize() - 14) * 35;
    }

    private static String background(Entry entry) {
        return dndcompanion.domain.character.BackgroundCatalog.selected(
                        entry.state().play().sheet())
                .map(dndcompanion.domain.character.BackgroundCatalog.Background::name)
                .orElse(entry.state().play().sheet().note(CharacterSheet.Note.BACKGROUND));
    }

    private static JPanel metric(String title, String value, String role) {
        JPanel panel =
                AppStyle.surfacePanel(
                        new GridLayout(0, 1, 0, 6),
                        role.equals("error")
                                ? "noteRed"
                                : role.equals("accent") ? "noteBlue" : "low");
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(8, 5, 8, 5)));
        panel.add(AppStyle.mono(title, 9));
        JLabel number = AppStyle.label(value, 17, true);
        AppStyle.setColorRole(number, role);
        panel.add(number);
        return panel;
    }
}
