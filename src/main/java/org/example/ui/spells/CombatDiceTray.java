package org.example.ui.spells;

import org.example.domain.spells.DamageDice;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDice;
import org.example.domain.spells.SpellMechanics;
import org.example.ui.common.AppStyle;
import org.example.ui.common.TactileStyle;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.Optional;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;

final class CombatDiceTray extends JPanel {
    private final Supplier<SpellCatalog.Spell> selected;
    private final IntSupplier effective, level;
    private final Supplier<Optional<DamageDice.Formula>> damage;
    private final IntConsumer roll;
    private final JPanel diceOptions = AppStyle.section(new GridLayout(1, 0, 6, 0)),
            diceActions = AppStyle.section(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JTextArea diceNote = AppStyle.text("");
    private List<Integer> visibleDice = List.of();
    final JButton damageButton, criticalButton;

    CombatDiceTray(
            Supplier<SpellCatalog.Spell> selected,
            IntSupplier effective,
            IntSupplier level,
            Supplier<Optional<DamageDice.Formula>> damage,
            IntConsumer roll,
            Runnable rollDamage,
            Runnable critical) {
        super(new BorderLayout());
        setOpaque(false);
        this.selected = selected;
        this.effective = effective;
        this.level = level;
        this.damage = damage;
        this.roll = roll;
        damageButton = AppStyle.button("Quick roll damage", rollDamage);
        criticalButton = AppStyle.button("Crit ×2 dice", critical);
        JPanel card = AppStyle.card(new BorderLayout(0, 12));
        card.add(TactileStyle.heading("Arcane Dice Tray", TactileStyle.YELLOW), BorderLayout.NORTH);
        JPanel body = AppStyle.section(new GridLayout(0, 1, 0, 8));
        card.setName("spell-dice-tray");
        diceNote.setRows(3);
        body.add(diceOptions);
        body.add(diceNote);
        diceActions.add(damageButton);
        diceActions.add(criticalButton);
        body.add(diceActions);
        card.add(body, BorderLayout.CENTER);
        add(card);
    }

    List<Integer> visibleDiceSides() {
        return visibleDice;
    }

    void refresh() {
        SpellCatalog.Spell spell = selected.get();
        List<Integer> next = SpellDice.forSpell(spell, effective.getAsInt(), level.getAsInt());
        setVisible(!next.isEmpty());
        if (!visibleDice.equals(next)) {
            visibleDice = next;
            diceOptions.removeAll();
            for (int sides : next) {
                JButton die =
                        AppStyle.button(
                                "d" + sides,
                                () -> {
                                    roll.accept(sides);
                                });
                die.setToolTipText(
                        sides == 20 && spell != null && SpellMechanics.hasAttack(spell)
                                ? "Roll this spell's attack with the spell attack modifier"
                                : "Roll one d"
                                        + sides
                                        + "; follow the spell text for quantity, modifiers, and"
                                        + " timing");
                diceOptions.add(die);
            }
        }
        for (Component child : diceOptions.getComponents()) {
            if (child instanceof JButton die)
                die.setToolTipText(
                        "d20".equals(die.getText())
                                        && spell != null
                                        && SpellMechanics.hasAttack(spell)
                                ? "Roll this spell's attack with the spell attack modifier; Innate"
                                        + " Sorcery grants Advantage"
                                : "Roll one "
                                        + die.getText()
                                        + "; follow the spell text for quantity, modifiers, and"
                                        + " timing");
        }
        boolean automatic = spell != null && damage.get().isPresent();
        damageButton.setVisible(automatic);
        criticalButton.setVisible(automatic && SpellMechanics.hasAttack(spell));
        diceActions.setVisible(automatic);
        diceNote.setText(
                spell == null
                        ? ""
                        : automatic
                                ? "Damage: " + damage.get().orElseThrow().notation()
                                : SpellDice.note(spell));
        AppStyle.applyAppearance(this);
        revalidate();
        if (getParent() != null) getParent().revalidate();
    }
}
