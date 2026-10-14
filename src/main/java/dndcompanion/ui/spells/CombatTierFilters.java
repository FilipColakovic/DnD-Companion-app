package dndcompanion.ui.spells;

import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.ui.common.AppStyle;

import java.awt.Component;
import java.awt.FlowLayout;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

final class CombatTierFilters extends JPanel {
    final Set<Integer> selectedLevels = new HashSet<>();
    private List<Integer> filterLevels;
    private List<SpellCatalog.Spell> available = List.of();
    private final Runnable changed;

    CombatTierFilters(Runnable changed) {
        super(new FlowLayout(FlowLayout.LEFT, 6, 0));
        setOpaque(false);
        this.changed = changed;
    }

    void refresh(List<SpellCatalog.Spell> spells) {
        available = spells;
        rebuildFilters();
    }

    private void rebuildFilters() {
        selectedLevels.removeIf(
                level -> level > 0 && available.stream().noneMatch(s -> s.level() == level));
        List<Integer> levels =
                available.stream()
                        .map(SpellCatalog.Spell::level)
                        .filter(l -> l > 0)
                        .distinct()
                        .toList();
        if (levels.equals(filterLevels)) {
            for (Component component : getComponents())
                if (component instanceof JToggleButton button)
                    button.setSelected(
                            filterSelected((Integer) button.getClientProperty("spellLevel")));
            return;
        }
        filterLevels = levels;
        removeAll();
        addFilter("All", -1);
        addFilter("Cantrips", 0);
        levels.forEach(l -> addFilter("Level " + l, l));
        revalidate();
    }

    private boolean filterSelected(int level) {
        return level < 0 ? selectedLevels.isEmpty() : selectedLevels.contains(level);
    }

    private void addFilter(String title, int level) {
        JToggleButton filter =
                AppStyle.chip(
                        title,
                        () -> {
                            if (level < 0) selectedLevels.clear();
                            else if (!selectedLevels.remove(level)) selectedLevels.add(level);
                            rebuildFilters();
                            changed.run();
                        });
        filter.putClientProperty("spellLevel", level);
        filter.setSelected(filterSelected(level));
        add(filter);
    }
}
