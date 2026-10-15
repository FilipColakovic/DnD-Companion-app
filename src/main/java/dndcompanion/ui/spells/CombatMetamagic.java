package dndcompanion.ui.spells;

import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Compatible, equipped modifiers selected for the current cast. */
final class CombatMetamagic extends JPanel {
    private final PlaySession session;
    private final JPanel options = AppStyle.section(new GridLayout(0, 2, 8, 6));
    private final JLabel points = AppStyle.mono("", 12);
    private final Map<Metamagic, JCheckBox> checks = new EnumMap<>(Metamagic.class);
    private String spellName = "";

    CombatMetamagic(PlaySession session, Runnable changed) {
        super(new BorderLayout(0, 8));
        this.session = session;
        setOpaque(false);
        setName("combat-metamagic");
        JPanel card = AppStyle.card(new BorderLayout(0, 8));
        card.putClientProperty("surfaceRole", "low");
        JPanel heading = AppStyle.section(new BorderLayout(8, 0));
        heading.add(AppStyle.label("METAMAGIC WEAVE MODS", 14, true));
        AppStyle.setColorRole(points, "accent");
        heading.add(points, BorderLayout.EAST);
        card.add(heading, BorderLayout.NORTH);
        card.add(options);
        add(card);
        for (Metamagic modifier : Metamagic.values()) {
            JCheckBox box = new JCheckBox();
            box.getAccessibleContext().setAccessibleName("Cast with " + modifier);
            AppStyle.roundButton(box);
            box.setBorderPainted(true);
            box.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(AppStyle.LINE),
                            new EmptyBorder(5, 6, 5, 6)));
            box.addActionListener(event -> changed.run());
            checks.put(modifier, box);
        }
    }

    void refresh(SpellCatalog.Spell spell, List<Metamagic> compatible) {
        String next = spell == null ? "" : spell.name();
        if (!next.equals(spellName)) checks.values().forEach(box -> box.setSelected(false));
        spellName = next;
        checks.forEach(
                (modifier, box) -> {
                    if (!compatible.contains(modifier)) box.setSelected(false);
                });
        if (options.getComponentCount() != compatible.size()
                || compatible.stream()
                        .anyMatch(modifier -> checks.get(modifier).getParent() != options)) {
            options.removeAll();
            compatible.forEach(modifier -> options.add(checks.get(modifier)));
        }
        compatible.forEach(
                modifier -> {
                    JCheckBox box = checks.get(modifier);
                    int cost = session.metamagicCost(Set.of(modifier));
                    box.setText(modifier.toString().replace(" Spell", "") + " (+" + cost + " SP)");
                    box.setToolTipText(modifier.eligibilityNote(spell));
                    box.setEnabled(box.isSelected() || cost <= session.remainingPoints());
                });
        points.setText(session.remainingPoints() + "/" + session.maxPoints() + " SP Available");
        setVisible(!compatible.isEmpty());
        revalidate();
    }

    Set<Metamagic> selected() {
        Set<Metamagic> result = EnumSet.noneOf(Metamagic.class);
        checks.forEach(
                (modifier, box) -> {
                    if (box.isSelected()) result.add(modifier);
                });
        return Set.copyOf(result);
    }
}
