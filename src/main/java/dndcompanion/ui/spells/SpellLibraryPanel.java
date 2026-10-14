package dndcompanion.ui.spells;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellSelection;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.LiveView;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * Compact spell cards and a live codex inspector sharing the character's existing selection rules.
 */
public final class SpellLibraryPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final SpellSelection selection;
    private final Supplier<CharacterState> character;
    private final SpellLibraryFilters filterControls;
    private final Map<String, LibrarySpellCard> cards = new HashMap<>();
    private final SpellCardGrid list = new SpellCardGrid();
    private final SpellInspector inspector;
    private final JScrollPane listScroll = scroll(list), inspectorScroll;
    private final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    private final JLabel cataloged = AppStyle.mono("", 11),
            shown = AppStyle.mono("", 11),
            message = AppStyle.label(" ", 12, false);
    private final Runnable listener = this::changed;
    private List<SpellCatalog.Spell> visible = List.of(), listedSpells = List.of();
    private int layoutWidth = -1;
    private SpellCatalog.Spell focused;
    private boolean updating, disposed;

    public SpellLibraryPanel(PlaySession session, Supplier<CharacterState> character) {
        super(new BorderLayout(0, 12));
        this.session = session;
        selection = session.selection();
        this.character = character;
        filterControls = new SpellLibraryFilters(selection, this::filterChanged);
        inspector = new SpellInspector(session, character);
        inspectorScroll = scroll(inspector);
        split.setRightComponent(inspectorScroll);
        setName("spell-library-panel");
        getAccessibleContext().setAccessibleName("Spell Library");
        putClientProperty("surfaceRole", "background");
        setBorder(new EmptyBorder(14, 14, 14, 14));
        add(filterControls, BorderLayout.NORTH);
        JPanel left = AppStyle.section(new BorderLayout(0, 8));
        left.add(cataloged, BorderLayout.NORTH);
        left.add(listScroll, BorderLayout.CENTER);
        left.add(shown, BorderLayout.SOUTH);
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
        split.setLeftComponent(left);
        split.setResizeWeight(.6);
        split.setBorder(null);
        split.setDividerSize(10);
        add(split, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        session.addListener(listener);
        selection.addChangeListener(listener);
        refresh();
    }

    @Override
    public void doLayout() {
        int orientation =
                getWidth() < 850 ? JSplitPane.VERTICAL_SPLIT : JSplitPane.HORIZONTAL_SPLIT;
        if (split.getOrientation() != orientation) {
            split.setOrientation(orientation);
            layoutWidth = -1;
        }
        super.doLayout();
        if (layoutWidth != split.getWidth()) {
            layoutWidth = split.getWidth();
            split.setDividerLocation(.6);
        }
        split.doLayout();
    }

    public void showPreparedOnly() {
        updating = true;
        try {
            filterControls.search.setText("");
            filterControls.selectedTiers.clear();
            filterControls.selectedSchools.clear();
            for (JCheckBox check :
                    List.of(
                            filterControls.available,
                            filterControls.concentration,
                            filterControls.ritual,
                            filterControls.psionic)) check.setSelected(false);
            filterControls.chosen.setSelected(true);
        } finally {
            updating = false;
        }
        filterChanged();
    }

    private void filterChanged() {
        if (updating) return;
        refresh();
        listScroll.getViewport().setViewPosition(new Point());
    }

    /** Start at the first matching spell when navigation opens this page. */
    public void scrollToTop() {
        if (disposed) return;
        listScroll.getViewport().setViewPosition(new Point());
        // Repeat after CardLayout visibility and viewport sizing have settled.
        SwingUtilities.invokeLater(
                () -> {
                    if (!disposed && isShowing()) {
                        listScroll.getViewport().setViewPosition(new Point());
                    }
                });
    }

    private void changed() {
        if (SwingUtilities.isEventDispatchThread()) refresh();
        else SwingUtilities.invokeLater(this::refresh);
    }

    public void refresh() {
        if (updating || disposed) return;
        updating = true;
        try {
            var candidates = selection.catalog().stream().filter(filterControls::matches).toList();
            filterControls.update(candidates);
            Comparator<SpellCatalog.Spell> order =
                    Comparator.comparingInt(SpellCatalog.Spell::level)
                            .thenComparing(SpellCatalog.Spell::name);
            visible =
                    candidates.stream()
                            .filter(
                                    s ->
                                            filterControls.selectedTiers.isEmpty()
                                                    || filterControls.selectedTiers.contains(
                                                            s.level()))
                            .filter(
                                    s ->
                                            filterControls.selectedSchools.isEmpty()
                                                    || filterControls.selectedSchools.contains(
                                                            SpellDetails.forSpell(s).school()))
                            .sorted(order)
                            .toList();
            List<SpellCatalog.Spell> current = visible;
            if (!current.equals(listedSpells)) {
                listedSpells = List.copyOf(current);
                list.removeAll();
                for (var spell : current)
                    list.add(
                            cards.computeIfAbsent(
                                    spell.name(),
                                    name ->
                                            new LibrarySpellCard(
                                                    spell,
                                                    selection,
                                                    this::inspectedSpell,
                                                    this::inspect,
                                                    this::choose)));
                if (current.isEmpty()) list.add(AppStyle.text("No spells match these filters."));
                list.revalidate();
            }
            for (var spell : current) cards.get(spell.name()).update();
            if (focused == null
                    || !selection.catalog().contains(focused)
                    || filterControls.chosen.isSelected() && !selection.selected(focused))
                focused = visible.isEmpty() ? null : visible.getFirst();
            filterControls.selectionSummary.setText(selection.summary());
            cataloged.setText(
                    "CATALOGED: "
                            + visible.size()
                            + " ENTRIES // "
                            + character.get().name()
                            + " LV "
                            + selection.level());
            shown.setText(visible.size() + " MATCHING SPELLS");
            refreshInspector();
            AppStyle.applyAppearance(this);
            revalidate();
            repaint();
        } finally {
            updating = false;
        }
    }

    public int visibleSpellCount() {
        return visible.size();
    }

    public String inspectedSpell() {
        return focused == null ? "" : focused.name();
    }

    public void inspect(String name) {
        focused =
                selection.catalog().stream()
                        .filter(s -> s.name().equals(name))
                        .findFirst()
                        .orElseThrow();
        inspectorScroll.getViewport().setViewPosition(new Point());
        refreshInspector();
        cards.values().forEach(LibrarySpellCard::update);
        AppStyle.applyAppearance(this);
    }

    private void choose(SpellCatalog.Spell spell) {
        String error = selection.choose(spell, !selection.selected(spell));
        message.setText(error.isEmpty() ? spell.name() + " selection updated." : error);
        inspect(spell.name());
    }

    private void refreshInspector() {
        inspector.refresh(focused);
    }

    @Override
    public void dispose() {
        disposed = true;
        session.removeListener(listener);
        selection.removeChangeListener(listener);
    }

    private static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        AppStyle.scrollPane(scroll);
        scroll.getViewport().putClientProperty("surfaceRole", "background");
        return scroll;
    }
}
