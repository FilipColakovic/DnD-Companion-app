package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.LiveView;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;

/**
 * PHB-only library shell; cards, filtering and stable inspectors have their own responsibilities.
 */
public final class FeatsPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final Supplier<CharacterState> character;
    private final FeatFilters filters;
    private final JPanel filterCard;
    private final CardLayout catalogLayout = new CardLayout();
    private final JPanel catalogs = AppStyle.section(catalogLayout);
    private final BackgroundLibraryPanel backgrounds;
    private final JToggleButton featTab, backgroundTab;
    private Runnable characterAction = () -> {};
    private final JLabel count = AppStyle.label("", 14, true),
            milestone = AppStyle.label("", 16, true),
            context = AppStyle.mono("", 10);
    private final JLabel message =
            AppStyle.label("Feat choices and notes save with your character.", 11, false);
    private final JPanel list = new FeatComponents.WidthPanel(new GridBagLayout());
    private final JPanel inspectors = new FeatComponents.WidthPanel(new BorderLayout());
    private final JScrollPane inspectorScroll = FeatComponents.scroll(inspectors);
    private final Map<String, FeatCard> cards = new LinkedHashMap<>();
    private final Map<String, FeatInspector> details = new LinkedHashMap<>();
    private List<FeatCatalog.Feat> visible = List.of();
    private List<String> sectionSignature = List.of();
    private String focused = "War Caster";
    private final Runnable listener = this::refresh;
    private boolean refreshing, disposed;

    public FeatsPanel(PlaySession session, Supplier<CharacterState> character) {
        super(new BorderLayout(0, 10));
        this.session = session;
        this.character = character;
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel heading = FeatComponents.card(new BorderLayout(10, 6));
        JPanel title = AppStyle.section(new BorderLayout(0, 5));
        JPanel labels = new FeatComponents.WrapPanel(3);
        labels.add(FeatComponents.tag("PHB 2024", true));
        labels.add(context);
        title.add(labels, BorderLayout.NORTH);
        title.add(AppStyle.label("FEATS & BACKGROUNDS", 18, true), BorderLayout.CENTER);
        JPanel modes = new FeatComponents.WrapPanel(5);
        featTab = AppStyle.chip("Feats 75", () -> showCatalog(false));
        backgroundTab = AppStyle.chip("Backgrounds 16", () -> showCatalog(true));
        featTab.getAccessibleContext().setAccessibleName("Catalog Feats");
        backgroundTab.getAccessibleContext().setAccessibleName("Catalog Backgrounds");
        modes.add(featTab);
        modes.add(backgroundTab);
        title.add(modes, BorderLayout.SOUTH);
        heading.add(title, BorderLayout.CENTER);
        JPanel tiles = AppStyle.section(new GridLayout(1, 2, 6, 0));
        tiles.add(tile("EQUIPPED FEATS", count));
        tiles.add(tile("NEXT MILESTONE", milestone));
        heading.add(tiles, BorderLayout.EAST);
        filters = new FeatFilters(this::refresh);
        filterCard = FeatComponents.card(new BorderLayout());
        filterCard.putClientProperty("surfaceRole", "low");
        filterCard.add(filters, BorderLayout.CENTER);
        JPanel top = AppStyle.section(new BorderLayout(0, 8));
        top.add(heading, BorderLayout.NORTH);
        top.add(filterCard, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);
        for (FeatCatalog.Feat feat : FeatCatalog.all())
            cards.put(
                    feat.name(),
                    new FeatCard(feat, () -> inspect(feat.name()), () -> choose(feat)));
        JScrollPane library = FeatComponents.scroll(list);
        JSplitPane split =
                new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, library, inspectorScroll) {
                    private int previousWidth = -1;

                    @Override
                    public void doLayout() {
                        int desired =
                                getWidth() < 760
                                        ? JSplitPane.VERTICAL_SPLIT
                                        : JSplitPane.HORIZONTAL_SPLIT;
                        if (desired != getOrientation()) {
                            setOrientation(desired);
                            previousWidth = -1;
                        }
                        if (previousWidth != getWidth()) {
                            setDividerLocation(
                                    desired == JSplitPane.VERTICAL_SPLIT
                                            ? Math.max(120, getHeight() / 2)
                                            : (int) (getWidth() * .60));
                            previousWidth = getWidth();
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
        split.setOpaque(false);
        split.setDividerSize(10);
        split.setResizeWeight(.60);
        library.setMinimumSize(new Dimension(260, 150));
        inspectorScroll.setMinimumSize(new Dimension(260, 150));
        catalogs.add(split, "feats");
        backgrounds = new BackgroundLibraryPanel(session, () -> characterAction.run());
        catalogs.add(backgrounds, "backgrounds");
        add(catalogs, BorderLayout.CENTER);
        JPanel footer = FeatComponents.card(new BorderLayout(8, 0));
        footer.add(message, BorderLayout.CENTER);
        footer.add(
                AppStyle.compactButton("Print feat dossier", this::printDossier),
                BorderLayout.EAST);
        add(footer, BorderLayout.SOUTH);
        session.addListener(listener);
        session.selection().addChangeListener(listener);
        showCatalog(false);
        inspect(focused);
        refresh();
    }

    private static JPanel tile(String title, JLabel value) {
        JPanel tile = AppStyle.surfacePanel(new BorderLayout(0, 6), "high");
        tile.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(6, 8, 6, 8)));
        tile.add(AppStyle.mono(title, 9), BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    private FeatInspector inspector(String name) {
        return details.computeIfAbsent(
                name,
                key -> {
                    FeatInspector detail =
                            new FeatInspector(
                                    FeatCatalog.named(key),
                                    session,
                                    character,
                                    message::setText,
                                    () -> openDetails(key));
                    return detail;
                });
    }

    public void inspect(String name) {
        showCatalog(false);
        FeatCatalog.named(name);
        boolean changed = !name.equals(focused);
        focused = name;
        FeatInspector detail = inspector(name);
        if (detail.getParent() != inspectors) {
            inspectors.removeAll();
            inspectors.add(detail, BorderLayout.NORTH);
        }
        if (changed) inspectorScroll.getViewport().setViewPosition(new Point());
        AppStyle.applyAppearance(inspectors);
        inspectors.revalidate();
        inspectors.repaint();
    }

    private void choose(FeatCatalog.Feat feat) {
        if (session.backgroundFeat(feat.name())
                && !session.snapshot().feats().containsKey(feat.name())) {
            message.setText(
                    "This feat is granted by your background. Change the background to remove it.");
            inspect(feat.name());
            return;
        }
        if (session.feats().containsKey(feat.name())) session.removeFeat(feat.name());
        else {
            CharacterState state = character.get();
            if (feat.requirements(state.level(), state.charisma()).isEmpty()
                    && !feat.needsConfirmation(state.charisma()))
                message.setText(
                        session.equipFeat(
                                feat, inspector(feat.name()).draft(), state.charisma(), true));
            inspect(feat.name());
        }
    }

    private void refresh() {
        if (refreshing || disposed) return;
        refreshing = true;
        try {
            CharacterState state = character.get();
            int next = FeatCatalog.nextMilestone(state.level());
            count.setText(String.format("%02d SELECTED", session.feats().size()));
            milestone.setText(next == 0 ? "COMPLETE" : "LV " + next);
            context.setText(state.subclass() + " · LEVEL " + state.level());
            cards.values().forEach(card -> card.refresh(state));
            visible = filters.filtered(state);
            List<String> signature =
                    visible.stream()
                            .map(feat -> FeatListModel.group(feat, state) + ":" + feat.name())
                            .toList();
            if (!signature.equals(sectionSignature)) {
                sectionSignature = signature;
                list.removeAll();
                for (FeatListModel.Group group : FeatListModel.Group.values()) {
                    List<FeatCatalog.Feat> section =
                            visible.stream()
                                    .filter(feat -> FeatListModel.group(feat, state) == group)
                                    .toList();
                    if (section.isEmpty()) continue;
                    JPanel label = AppStyle.section(new BorderLayout());
                    label.setBorder(new EmptyBorder(5, 0, 4, 0));
                    label.add(
                            AppStyle.mono("■ " + group.title + " [" + section.size() + "]", 11),
                            BorderLayout.WEST);
                    label.setBorder(
                            BorderFactory.createCompoundBorder(
                                    BorderFactory.createMatteBorder(0, 0, 1, 0, AppStyle.LINE),
                                    new EmptyBorder(6, 0, 6, 0)));
                    FeatComponents.row(list, label);
                    JPanel grid = FeatComponents.cardGrid();
                    for (FeatCatalog.Feat feat : section) grid.add(cards.get(feat.name()));
                    FeatComponents.row(list, grid);
                }
                if (visible.isEmpty())
                    FeatComponents.row(
                            list, AppStyle.label("No feats match these filters.", 13, false));
                list.revalidate();
                list.repaint();
            }
            AppStyle.applyAppearance(this);
            backgrounds.refresh();
        } finally {
            refreshing = false;
        }
    }

    private void openDetails(String name) {
        JPanel window = new DetailWindow(name);
        AppStyle.openWindow(this, name, window);
    }

    private final class DetailWindow extends JPanel implements LiveView {
        private final FeatInspector detail;

        DetailWindow(String name) {
            super(new BorderLayout());
            setBorder(new EmptyBorder(12, 12, 12, 12));
            detail =
                    new FeatInspector(
                            FeatCatalog.named(name), session, character, message::setText, null);
            JPanel content = new FeatComponents.WidthPanel(new BorderLayout());
            content.add(detail, BorderLayout.NORTH);
            add(FeatComponents.scroll(content), BorderLayout.CENTER);
        }

        public void dispose() {
            detail.dispose();
        }
    }

    private void printDossier() {
        JTextArea document = new JTextArea("FEATS · " + character.get().name() + " · PHB 2024\n\n");
        session.feats().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(
                        entry -> {
                            FeatCatalog.Feat feat = FeatCatalog.named(entry.getKey());
                            document.append(
                                    feat.name()
                                            + " · "
                                            + feat.category()
                                            + "\n"
                                            + feat.summary()
                                            + "\n"
                                            + entry.getValue()
                                            + "\n\n");
                        });
        try {
            document.print();
        } catch (java.awt.print.PrinterException error) {
            message.setText("Printing failed: " + error.getMessage());
        }
    }

    public List<String> visibleFeatNames() {
        return visible.stream().map(FeatCatalog.Feat::name).toList();
    }

    private void showCatalog(boolean background) {
        catalogLayout.show(catalogs, background ? "backgrounds" : "feats");
        filterCard.setVisible(!background);
        featTab.setSelected(!background);
        backgroundTab.setSelected(background);
        revalidate();
        repaint();
    }

    public void showBackgrounds() {
        showCatalog(true);
        backgrounds.refresh();
    }

    public void setCharacterAction(Runnable action) {
        characterAction = Objects.requireNonNull(action);
    }

    public void dispose() {
        disposed = true;
        session.removeListener(listener);
        session.selection().removeChangeListener(listener);
        details.values().forEach(FeatInspector::dispose);
    }
}
