package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/** Counted tabs and independent search, level, selection and ASI filters. */
final class FeatFilters extends JPanel {
    private final JTextField search = new JTextField(20);
    private final JComboBox<String> sort =
            new JComboBox<>(new String[] {"Category & name", "Alphabetical"});
    private final JCheckBox available = new JCheckBox("Available at my level"),
            equipped = new JCheckBox("Equipped only"),
            asi = new JCheckBox("Ability increase");
    private final Map<String, JToggleButton> tabs = new LinkedHashMap<>();
    private final Set<String> categories = new HashSet<>();
    private boolean resetting;

    FeatFilters(Runnable changed) {
        super(new BorderLayout(0, 6));
        setOpaque(false);
        JPanel tabRow = new FeatComponents.WrapPanel(5);
        List<String> names = new ArrayList<>();
        names.add("All");
        for (var item : FeatCatalog.Category.values()) names.add(item.toString());
        for (String name : names) {
            JToggleButton button =
                    AppStyle.chip(
                            name,
                            () -> {
                                if (name.equals("All")) categories.clear();
                                else if (!categories.remove(name)) categories.add(name);
                                changed.run();
                            });
            button.setFont(new Font("Segoe UI", Font.BOLD, 14));
            button.setBorder(new javax.swing.border.EmptyBorder(7, 9, 10, 12));
            button.getAccessibleContext().setAccessibleName("Feat category " + name);
            button.setSelected(name.equals("All"));
            tabs.put(name, button);
            tabRow.add(button);
        }
        add(tabRow, BorderLayout.NORTH);
        JPanel controls = AppStyle.section(new BorderLayout(8, 6));
        JPanel searchRow = AppStyle.section(new BorderLayout(8, 0));
        search.setName("feat-search");
        search.getAccessibleContext().setAccessibleName("Search feats");
        search.setToolTipText("Search names, prerequisites or keywords");
        searchRow.add(AppStyle.label("Search", 12, false), BorderLayout.WEST);
        searchRow.add(search, BorderLayout.CENTER);
        JPanel right = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        sort.getAccessibleContext().setAccessibleName("Sort feats");
        right.add(sort);
        right.add(
                AppStyle.compactButton(
                        "Reset",
                        () -> {
                            resetting = true;
                            search.setText("");
                            categories.clear();
                            sort.setSelectedIndex(0);
                            available.setSelected(false);
                            equipped.setSelected(false);
                            asi.setSelected(false);
                            resetting = false;
                            changed.run();
                        }));
        searchRow.add(right, BorderLayout.EAST);
        controls.add(searchRow, BorderLayout.NORTH);
        JPanel toggles = new FeatComponents.WrapPanel(8);
        for (JCheckBox box : List.of(available, equipped, asi)) {
            AppStyle.roundButton(box);
            box.setFont(new Font("Segoe UI", Font.BOLD, 13));
            box.addActionListener(e -> changed.run());
            toggles.add(box);
        }
        controls.add(toggles, BorderLayout.SOUTH);
        add(controls, BorderLayout.CENTER);
        search.getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                update();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                update();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                update();
                            }

                            private void update() {
                                if (!resetting) changed.run();
                            }
                        });
        sort.addActionListener(
                e -> {
                    if (!resetting) changed.run();
                });
    }

    private boolean matches(FeatCatalog.Feat feat, CharacterState state) {
        String query = search.getText().strip().toLowerCase(Locale.ROOT);
        return (!available.isSelected()
                        || feat.requirements(state.level(), state.charisma()).isEmpty())
                && (!equipped.isSelected() || state.equippedFeats().containsKey(feat.name()))
                && (!asi.isSelected() || !feat.abilityIncrease().isEmpty())
                && (feat.name() + " " + feat.prerequisite() + " " + feat.summary())
                        .toLowerCase(Locale.ROOT)
                        .contains(query);
    }

    List<FeatCatalog.Feat> filtered(CharacterState state) {
        List<FeatCatalog.Feat> base =
                FeatCatalog.all().stream().filter(feat -> matches(feat, state)).toList();
        tabs.forEach(
                (name, button) -> {
                    button.setText(
                            name.toUpperCase(Locale.ROOT)
                                    + "  "
                                    + base.stream()
                                            .filter(
                                                    feat ->
                                                            name.equals("All")
                                                                    || feat.category()
                                                                            .toString()
                                                                            .equals(name))
                                            .count());
                    button.setSelected(
                            name.equals("All") ? categories.isEmpty() : categories.contains(name));
                });
        return FeatListModel.ordered(
                base.stream()
                        .filter(
                                feat ->
                                        categories.isEmpty()
                                                || categories.contains(feat.category().toString()))
                        .toList(),
                state,
                sort.getSelectedIndex() == 0);
    }
}
