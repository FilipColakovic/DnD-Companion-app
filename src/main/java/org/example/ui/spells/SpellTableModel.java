package org.example.ui.spells;

import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellSelection;

import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.table.AbstractTableModel;

/** Filtering and checkbox behavior kept separate from spellbook layout. */
final class SpellTableModel extends AbstractTableModel {
    private static final String[] COLUMNS = {"Choose", "Level", "Spell", "Availability"};
    private final SpellSelection selection;
    private final Consumer<String> onMessage;
    private List<SpellCatalog.Spell> spells = List.of();
    private List<List<Object>> displayedValues = List.of();
    private final boolean readOnly;

    SpellTableModel(SpellSelection selection, Consumer<String> onMessage) {
        this(selection, onMessage, false);
    }

    SpellTableModel(SpellSelection selection, Consumer<String> onMessage, boolean readOnly) {
        this.selection = selection;
        this.onMessage = onMessage;
        this.readOnly = readOnly;
    }

    boolean filter(String search, int level, boolean availableOnly, boolean selectedOnly) {
        String query = search.strip().toLowerCase(Locale.ROOT);
        var filtered =
                selection.catalog().stream()
                        .filter(
                                spell ->
                                        query.isEmpty()
                                                || spell.name()
                                                        .toLowerCase(Locale.ROOT)
                                                        .contains(query))
                        .filter(spell -> level < 0 || spell.level() == level)
                        .filter(spell -> !availableOnly || selection.available(spell))
                        .filter(spell -> !selectedOnly || selection.selected(spell))
                        .toList();
        boolean rowsChanged = !filtered.equals(spells);
        var previousValues = displayedValues;
        spells = filtered;
        displayedValues =
                java.util.stream.IntStream.range(0, getRowCount())
                        .mapToObj(
                                row ->
                                        java.util.stream.IntStream.range(0, getColumnCount())
                                                .mapToObj(column -> getValueAt(row, column))
                                                .toList())
                        .toList();
        if (rowsChanged) {
            fireTableDataChanged();
        } else {
            for (int row = 0; row < getRowCount(); row++) {
                for (int column = 0; column < getColumnCount(); column++) {
                    if (!displayedValues
                            .get(row)
                            .get(column)
                            .equals(previousValues.get(row).get(column))) {
                        fireTableCellUpdated(row, column);
                    }
                }
            }
        }
        return rowsChanged;
    }

    SpellCatalog.Spell spellAt(int row) {
        return spells.get(row);
    }

    public int getRowCount() {
        return spells.size();
    }

    public int getColumnCount() {
        return COLUMNS.length;
    }

    public String getColumnName(int column) {
        return COLUMNS[column];
    }

    public Class<?> getColumnClass(int column) {
        return column == 0 ? Boolean.class : String.class;
    }

    public Object getValueAt(int row, int column) {
        SpellCatalog.Spell spell = spellAt(row);
        return switch (column) {
            case 0 -> selection.selected(spell);
            case 1 -> spell.levelName();
            case 2 -> spell.name();
            default ->
                    selection.speciesSpell(spell)
                            ? selection.species().castingNote(spell.name(), selection.level())
                            : selection.automatic(spell)
                                    ? "Always prepared · subclass"
                                    : selection.available(spell)
                                            ? readOnly
                                                    ? spell.level() == 0
                                                            ? "Known cantrip"
                                                            : "Prepared"
                                                    : "Available"
                                            : spell.sorcererSpell()
                                                    ? "Needs higher Sorcerer level"
                                                    : "Unlocks at level "
                                                            + selection.species().grants().stream()
                                                                    .filter(
                                                                            grant ->
                                                                                    grant.name()
                                                                                            .equals(
                                                                                                    spell
                                                                                                            .name()))
                                                                    .mapToInt(
                                                                            grant ->
                                                                                    grant
                                                                                            .characterLevel())
                                                                    .findFirst()
                                                                    .orElse(
                                                                            SpellCatalog
                                                                                    .unlockLevel(
                                                                                            spell,
                                                                                            selection
                                                                                                    .subclass()));
        };
    }

    public boolean isCellEditable(int row, int column) {
        SpellCatalog.Spell spell = spellAt(row);
        return !readOnly
                && column == 0
                && !selection.automatic(spell)
                && (selection.available(spell) || selection.selected(spell));
    }

    public void setValueAt(Object value, int row, int column) {
        if (column != 0 || readOnly) {
            return;
        }
        String error = selection.choose(spellAt(row), Boolean.TRUE.equals(value));
        onMessage.accept(error.isEmpty() ? " " : error);
        if (!error.isEmpty()) {
            fireTableCellUpdated(row, column);
        }
    }
}
