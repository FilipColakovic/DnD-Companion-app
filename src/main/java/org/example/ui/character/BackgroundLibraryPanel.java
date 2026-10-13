package org.example.ui.character;

import org.example.domain.character.BackgroundCatalog;
import org.example.domain.character.PlaySession;
import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Background catalog in the same compact card/inspector layout as the feat and spell libraries. */
final class BackgroundLibraryPanel extends JPanel {
    private final PlaySession session;
    private final Runnable characterAction;
    private final JTextField search = new JTextField(22);
    private final JPanel grid = FeatComponents.cardGrid(),
            inspector = new FeatComponents.WidthPanel(new BorderLayout());
    private final Map<String, BackgroundCard> cards = new LinkedHashMap<>();
    private final JLabel count = AppStyle.mono("", 11);
    private String focused = "Acolyte", listStamp = "", inspectorStamp = "";

    BackgroundLibraryPanel(PlaySession session, Runnable characterAction) {
        super(new BorderLayout(0, 8));
        this.session = session;
        this.characterAction = characterAction;
        setOpaque(false);
        JPanel controls = FeatComponents.card(new BorderLayout(8, 0));
        controls.putClientProperty("surfaceRole", "low");
        controls.add(AppStyle.mono("SEARCH BACKGROUNDS", 11), BorderLayout.WEST);
        search.getAccessibleContext().setAccessibleName("Search backgrounds");
        controls.add(search);
        controls.add(AppStyle.compactButton("Clear", () -> search.setText("")), BorderLayout.EAST);
        add(controls, BorderLayout.NORTH);
        for (var background : BackgroundCatalog.all())
            cards.put(background.name(), new BackgroundCard(background));
        JPanel left = new FeatComponents.WidthPanel(new BorderLayout(0, 8));
        left.add(count, BorderLayout.NORTH);
        left.add(grid, BorderLayout.CENTER);
        JScrollPane library = FeatComponents.scroll(left),
                details = FeatComponents.scroll(inspector);
        JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, library, details) {
                    private int previous = -1;

                    @Override
                    public void doLayout() {
                        int orientation =
                                getWidth() < 760
                                        ? JSplitPane.VERTICAL_SPLIT
                                        : JSplitPane.HORIZONTAL_SPLIT;
                        if (getOrientation() != orientation) {
                            setOrientation(orientation);
                            previous = -1;
                        }
                        if (previous != getWidth()) {
                            setDividerLocation(
                                    orientation == JSplitPane.VERTICAL_SPLIT
                                            ? getHeight() / 2
                                            : (int) (getWidth() * .6));
                            previous = getWidth();
                        }
                        super.doLayout();
                    }
                };
        split.setUI(
                new javax.swing.plaf.basic.BasicSplitPaneUI() {
                    @Override
                    public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                        return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                            @Override
                            public void paint(Graphics g) {
                                g.setColor(AppStyle.BACKGROUND);
                                g.fillRect(0, 0, getWidth(), getHeight());
                            }
                        };
                    }
                });
        split.setBorder(null);
        split.setDividerSize(10);
        split.setResizeWeight(.6);
        library.setMinimumSize(new Dimension(260, 150));
        details.setMinimumSize(new Dimension(260, 150));
        add(split);
        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                refresh();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                refresh();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                refresh();
                            }
                        });
        refresh();
    }

    void inspect(String name) {
        BackgroundCatalog.named(name);
        focused = name;
        refreshInspector();
    }

    void refresh() {
        String query = search.getText().strip().toLowerCase(Locale.ROOT),
                selected =
                        BackgroundCatalog.selected(session.sheet())
                                .map(BackgroundCatalog.Background::name)
                                .orElse("");
        List<BackgroundCatalog.Background> visible =
                BackgroundCatalog.all().stream()
                        .filter(
                                background ->
                                        (background.name()
                                                        + " "
                                                        + background.featLabel()
                                                        + " "
                                                        + background.skills()
                                                        + " "
                                                        + background.tool()
                                                        + " "
                                                        + background.summary())
                                                .toLowerCase(Locale.ROOT)
                                                .contains(query))
                        .sorted(
                                Comparator.comparing(
                                                (BackgroundCatalog.Background background) ->
                                                        !background.name().equals(selected))
                                        .thenComparing(BackgroundCatalog.Background::name))
                        .toList();
        String stamp = visible.stream().map(BackgroundCatalog.Background::name).toList().toString();
        if (!stamp.equals(listStamp)) {
            listStamp = stamp;
            grid.removeAll();
            visible.forEach(background -> grid.add(cards.get(background.name())));
            grid.revalidate();
            grid.repaint();
        }
        cards.values().forEach(card -> card.refresh(selected));
        count.setText(
                visible.size()
                        + " PHB 2024 BACKGROUNDS // "
                        + (selected.isEmpty()
                                ? "NONE SELECTED"
                                : selected.toUpperCase(Locale.ROOT) + " SELECTED"));
        refreshInspector();
        AppStyle.applyAppearance(this);
    }

    private void refreshInspector() {
        var background = BackgroundCatalog.named(focused);
        String selected =
                BackgroundCatalog.selected(session.sheet())
                        .map(BackgroundCatalog.Background::name)
                        .orElse("");
        String stamp = focused + ":" + selected + ":" + session.pendingDecisions();
        if (stamp.equals(inspectorStamp)) return;
        inspectorStamp = stamp;
        inspector.removeAll();
        JPanel card = FeatComponents.card(new GridBagLayout());
        FeatComponents.row(
                card,
                FeatComponents.colorTag(
                        "BACKGROUND CODEX // PHB 2024", "badgeDark", "badgeYellowText"));
        JTextArea title = FeatComponents.text(background.name().toUpperCase(Locale.ROOT), 26);
        title.setFont(new Font("Segoe UI", Font.BOLD, 26));
        FeatComponents.row(card, title);
        FeatComponents.row(card, FeatComponents.text(background.summary(), 13));
        FeatComponents.row(
                card,
                FeatComponents.block(
                        "ORIGIN FEAT", FeatComponents.text(background.featLabel(), 14)));
        FeatComponents.row(
                card,
                FeatComponents.block(
                        "AUTOMATIC SKILL PROFICIENCIES",
                        FeatComponents.text(
                                String.join(
                                        " · ",
                                        background.skills().stream()
                                                .map(skill -> skill.label)
                                                .toList()),
                                13)));
        FeatComponents.row(
                card,
                FeatComponents.block(
                        "ABILITY SCORE OPTIONS",
                        FeatComponents.text(
                                String.join(
                                                " / ",
                                                background.abilities().stream()
                                                        .map(Enum::name)
                                                        .toList())
                                        + "\n"
                                        + "Choose +2/+1 or +1 to all three on Character. Your"
                                        + " ability fields hold final scores.",
                                12)));
        FeatComponents.row(
                card,
                FeatComponents.block(
                        "TOOL PROFICIENCY", FeatComponents.text(background.tool(), 13)));
        FeatComponents.row(
                card,
                FeatComponents.block(
                        "STARTING EQUIPMENT",
                        FeatComponents.text(
                                "Choose the PHB equipment package or 50 GP on Character. The"
                                    + " published entry lists the package.",
                                12)));
        JButton select =
                AppStyle.primaryButton(
                        background.name().equals(selected)
                                ? "Remove background"
                                : "Select background",
                        () -> {
                            session.selectBackground(
                                    background.name().equals(selected) ? null : background);
                            refresh();
                        });
        select.getAccessibleContext().setAccessibleName("Select background " + background.name());
        select.putClientProperty("tactileRole", "yellow");
        FeatComponents.row(card, select);
        FeatComponents.row(
                card, AppStyle.compactButton("Complete choices on Character", characterAction));
        FeatComponents.row(
                card,
                AppStyle.compactButton(
                        "Full PHB entry",
                        () -> {
                            try {
                                Desktop.getDesktop()
                                        .browse(java.net.URI.create(background.sourceUrl()));
                            } catch (Exception error) {
                                select.setToolTipText(background.sourceUrl());
                            }
                        }));
        inspector.add(card, BorderLayout.NORTH);
        AppStyle.applyAppearance(inspector);
        inspector.revalidate();
        inspector.repaint();
    }

    private final class BackgroundCard extends JPanel {
        final BackgroundCatalog.Background background;
        final JLabel status = FeatComponents.tag("AVAILABLE", false);
        final JButton select;

        BackgroundCard(BackgroundCatalog.Background background) {
            super(new BorderLayout());
            this.background = background;
            setOpaque(false);
            getAccessibleContext().setAccessibleName("Background card " + background.name());
            JPanel card = FeatComponents.card(new BorderLayout(0, 7));
            add(card);
            JPanel heading = AppStyle.section(new BorderLayout(8, 0));
            heading.add(
                    FeatComponents.title(
                            background.name().toUpperCase(Locale.ROOT),
                            () -> inspect(background.name())));
            select =
                    AppStyle.compactButton(
                            "Select",
                            () -> {
                                session.selectBackground(
                                        BackgroundCatalog.selected(session.sheet())
                                                        .filter(
                                                                item ->
                                                                        item.name()
                                                                                .equals(
                                                                                        background
                                                                                                .name()))
                                                        .isPresent()
                                                ? null
                                                : background);
                                inspect(background.name());
                                BackgroundLibraryPanel.this.refresh();
                            });
            select.getAccessibleContext()
                    .setAccessibleName("Choose background " + background.name());
            JPanel action = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            action.add(select);
            heading.add(action, BorderLayout.EAST);
            card.add(heading, BorderLayout.NORTH);
            JPanel body = AppStyle.section(new BorderLayout(0, 5));
            JPanel badges = new FeatComponents.WrapPanel(4);
            badges.add(FeatComponents.tag("BACKGROUND", true));
            badges.add(status);
            body.add(badges, BorderLayout.NORTH);
            body.add(
                    FeatComponents.colorTag(
                            background.featLabel().toUpperCase(Locale.ROOT),
                            "badgeBlue",
                            "badgeLightText"));
            JTextArea skills =
                    FeatComponents.text(
                            String.join(
                                    " // ",
                                    background.skills().stream()
                                            .map(skill -> skill.label.toUpperCase(Locale.ROOT))
                                            .toList()),
                            12);
            skills.setFont(new Font("Consolas", Font.BOLD, 12));
            AppStyle.setColorRole(skills, "accent");
            JPanel strip = AppStyle.surfacePanel(new BorderLayout(), "low");
            strip.setBorder(new EmptyBorder(4, 6, 4, 6));
            strip.add(skills);
            body.add(strip, BorderLayout.SOUTH);
            card.add(body);
            bind(card);
        }

        void bind(Component component) {
            if (!(component instanceof AbstractButton))
                component.addMouseListener(
                        new MouseAdapter() {
                            @Override
                            public void mouseClicked(MouseEvent event) {
                                if (SwingUtilities.isLeftMouseButton(event))
                                    inspect(background.name());
                            }
                        });
            if (component instanceof Container container)
                for (Component child : container.getComponents()) bind(child);
        }

        void refresh(String selected) {
            boolean active = background.name().equals(selected);
            status.setText(active ? "SELECTED" : "AVAILABLE");
            status.putClientProperty("surfaceRole", active ? "badgeDark" : "high");
            AppStyle.setColorRole(status, active ? "badgeLightText" : "ink");
            select.setText(active ? "Remove" : "Select");
            select.putClientProperty("tactileRole", active ? null : "yellow");
        }
    }
}
