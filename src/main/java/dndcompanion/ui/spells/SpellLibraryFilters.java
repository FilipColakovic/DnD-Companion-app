package dndcompanion.ui.spells;

import static dndcompanion.ui.spells.SpellLibraryComponents.filterButton;
import static dndcompanion.ui.spells.SpellLibraryComponents.row;
import static dndcompanion.ui.spells.SpellLibraryComponents.toggle;

import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellSelection;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.spells.SpellLibraryComponents.Wrap;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class SpellLibraryFilters extends JPanel {
    private final SpellSelection selection;
    final JTextArea selectionSummary = AppStyle.text("");
    final JTextField search = new JTextField(20);
    final JCheckBox available = new JCheckBox("Available"),
            chosen = new JCheckBox("Prepared only"),
            concentration = new JCheckBox("Concentration"),
            ritual = new JCheckBox("Ritual"),
            psionic = new JCheckBox("Psionic");
    final Map<Integer, JToggleButton> tiers = new LinkedHashMap<>();
    final Map<String, JToggleButton> schools = new LinkedHashMap<>();

    final Set<Integer> selectedTiers = new HashSet<>();
    final Set<String> selectedSchools = new HashSet<>();

    SpellLibraryFilters(SpellSelection selection, Runnable changed) {
        super(new BorderLayout());
        setOpaque(false);
        this.selection = selection;
        JPanel filters = AppStyle.card(new GridBagLayout());
        filters.putClientProperty("surfaceRole", "low");
        filters.setBorder(new EmptyBorder(12, 12, 16, 16));
        row(filters, selectionSummary);
        JPanel controls = new Wrap();
        controls.add(AppStyle.mono("SEARCH", 10));
        search.getAccessibleContext().setAccessibleName("Search library spells");
        controls.add(search);
        controls.add(filterButton("Clear", () -> search.setText("")));
        for (JCheckBox check : List.of(available, chosen, concentration, ritual, psionic)) {
            AppStyle.roundButton(check);
            check.addActionListener(e -> changed.run());
            controls.add(check);
        }
        row(filters, controls);
        JPanel tierRow = new Wrap();
        tierRow.add(AppStyle.mono("TIER", 10));
        for (int level = -1; level <= 9; level++) {
            int value = level;
            JToggleButton button =
                    toggle(
                            "",
                            () -> {
                                if (value < 0) selectedTiers.clear();
                                else if (!selectedTiers.remove(value)) selectedTiers.add(value);
                                changed.run();
                            });
            button.getAccessibleContext().setAccessibleName("Library tier " + level);
            tiers.put(level, button);
            tierRow.add(button);
        }
        tiers.get(-1).setSelected(true);
        row(filters, tierRow);
        JPanel schoolRow = new Wrap();
        schoolRow.add(AppStyle.mono("SCHOOL", 10));
        for (String name :
                List.of(
                        "All",
                        "Abjuration",
                        "Conjuration",
                        "Divination",
                        "Enchantment",
                        "Evocation",
                        "Illusion",
                        "Necromancy",
                        "Transmutation")) {
            JToggleButton button =
                    toggle(
                            name.toUpperCase(Locale.ROOT),
                            () -> {
                                if (name.equals("All")) selectedSchools.clear();
                                else if (!selectedSchools.remove(name)) selectedSchools.add(name);
                                changed.run();
                            });
            button.getAccessibleContext().setAccessibleName("Library school " + name);
            schools.put(name, button);
            schoolRow.add(button);
        }
        schools.get("All").setSelected(true);
        row(filters, schoolRow);
        add(filters);
        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                changed.run();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                changed.run();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                changed.run();
                            }
                        });
    }

    boolean matches(SpellCatalog.Spell spell) {
        SpellDetails d = SpellDetails.forSpell(spell);
        return spell.name()
                        .toLowerCase(Locale.ROOT)
                        .contains(search.getText().strip().toLowerCase(Locale.ROOT))
                && (!available.isSelected() || selection.available(spell))
                && (!chosen.isSelected() || selection.selected(spell))
                && (!concentration.isSelected() || d.concentration())
                && (!ritual.isSelected() || d.ritual())
                && (!psionic.isSelected()
                        || selection.subclass() == SorcererRules.Subclass.ABERRANT
                                && SpellCatalog.unlockLevel(spell, selection.subclass()) > 0);
    }

    void update(List<SpellCatalog.Spell> candidates) {
        tiers.forEach(
                (level, button) -> {
                    long count =
                            candidates.stream()
                                    .filter(s -> level < 0 || s.level() == level)
                                    .count();
                    button.setText(
                            (level < 0
                                            ? "ALL"
                                            : level == 0
                                                    ? "CANTRIP"
                                                    : level == 1
                                                            ? "1ST"
                                                            : level == 2
                                                                    ? "2ND"
                                                                    : level == 3
                                                                            ? "3RD"
                                                                            : level + "TH")
                                    + " ("
                                    + count
                                    + ")");
                });
        tiers.forEach(
                (level, button) ->
                        button.setSelected(
                                level < 0
                                        ? selectedTiers.isEmpty()
                                        : selectedTiers.contains(level)));
        schools.forEach(
                (name, button) ->
                        button.setSelected(
                                name.equals("All")
                                        ? selectedSchools.isEmpty()
                                        : selectedSchools.contains(name)));
        available.setText("Lv " + selection.level() + " available");
        psionic.setToolTipText("Aberrant Sorcery's subclass spell grants, including Mind Sliver");
    }
}
