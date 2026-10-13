package org.example.ui.character;

import org.example.domain.character.CharacterState;
import org.example.domain.character.SorcererRules;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

/** Dependency-free rules and Swing interaction checks. Run with -Djava.awt.headless=true. */
public final class SorcererChecks {
    public static void main(String[] args) throws Exception {
        int[] totals = {2, 3, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22};
        int[] prepared = {
            2, 4, 10, 11, 15, 16, 19, 20, 24, 25, 26, 26, 27, 27, 28, 28, 29, 30, 31, 32
        };
        int[] cantrips = {4, 4, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
        for (int level = 1; level <= 20; level++) {
            var stats = SorcererRules.calculate(level, 17);
            check(stats.totalSpellSlots() == totals[level - 1], "Slot total at " + level);
            check(stats.preparedSpells() == prepared[level - 1], "Prepared spells at " + level);
            check(stats.cantrips() == cantrips[level - 1], "Cantrips at " + level);
            check(stats.sorceryPoints() == (level == 1 ? 0 : level), "Sorcery points");
            for (int charisma = 1; charisma <= 30; charisma++) {
                var result = SorcererRules.calculate(level, charisma);
                int modifier = (int) Math.floor((charisma - 10) / 2.0);
                check(result.charismaModifier() == modifier, "Charisma rounding");
                check(result.spellSaveDc() == 8 + result.spellAttack(), "DC and attack relation");
            }
        }
        var first = SorcererRules.calculate(1, 17);
        check(first.spellSaveDc() == 13 && first.spellAttack() == 5, "Screenshot defaults");
        var last = SorcererRules.calculate(20, 20);
        check(last.spellSaveDc() == 19 && last.spellAttack() == 11, "Level 20 proficiency");
        check(
                last.spellSlots().equals(java.util.List.of(4, 3, 3, 3, 3, 2, 2, 1, 1)),
                "Level 20 slots");
        check(SorcererRules.Subclass.values().length == 4, "Only four PHB 2024 subclasses");
        for (var subclass : SorcererRules.Subclass.values()) {
            for (int level = 1; level <= 20; level++) {
                var stats = SorcererRules.calculate(level, 17, subclass);
                int bonus =
                        level < 3 || subclass == SorcererRules.Subclass.WILD_MAGIC
                                ? 0
                                : new int[] {4, 6, 8, 10}[Math.min(3, (level - 3) / 2)];
                check(stats.subclassPreparedSpells() == bonus, "Subclass prepared spells");
                int expectedCantrips =
                        cantrips[level - 1]
                                - (level >= 3 && subclass != SorcererRules.Subclass.ABERRANT
                                        ? 1
                                        : 0);
                check(stats.cantrips() == expectedCantrips, "Subclass cantrips");
                check(
                        stats.spellSlots().equals(SorcererRules.calculate(level, 17).spellSlots()),
                        "Subclass preserves slots");
            }
        }
        for (int[] invalid : new int[][] {{0, 17}, {21, 17}, {1, 0}, {1, 31}}) {
            try {
                SorcererRules.calculate(invalid[0], invalid[1]);
                throw new AssertionError("Invalid inputs accepted");
            } catch (IllegalArgumentException expected) {
            }
        }

        SwingUtilities.invokeAndWait(
                () -> {
                    var workspace = new GrimoirePanel(CharacterState.defaults());
                    try {
                        workspace.showPage(GrimoirePanel.CHARACTER);
                        var controller = workspace.characterController();
                        var level = accessible(workspace, JSpinner.class, "Character level");
                        level.setValue(20);
                        check(
                                controller.snapshot().level() == 20,
                                "Current level control updates controller");
                        var charisma = accessible(workspace, JSpinner.class, "CHA score");
                        charisma.setValue(30);
                        check(
                                controller.snapshot().charisma() == 30,
                                "Current ability control updates controller");
                        charisma.setValue(1);
                        check(controller.snapshot().charisma() == 1, "Lower ability boundary");
                        var selector = find(workspace, SubclassSelector.class);
                        var menu = selector.createMenu();
                        check(menu.getComponentCount() == 4, "Four PHB subclass choices");
                        ((JRadioButtonMenuItem) menu.getComponent(3)).doClick(0);
                        check(
                                controller.snapshot().subclass()
                                        == SorcererRules.Subclass.WILD_MAGIC,
                                "Menu updates current character");
                        for (int invalid : new int[] {0, 21})
                            try {
                                controller.setLevel(invalid);
                                throw new AssertionError("Invalid level accepted");
                            } catch (IllegalArgumentException expected) {
                            }
                        for (int invalid : new int[] {0, 31})
                            try {
                                controller.setCharismaScore(invalid);
                                throw new AssertionError("Invalid ability accepted");
                            } catch (IllegalArgumentException expected) {
                            }
                    } finally {
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: 20 levels, 600 ability combinations, four subclasses, bounds and current"
                        + " Character controls.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)
                    && child.getAccessibleContext() != null
                    && name.equals(child.getAccessibleContext().getAccessibleName()))
                return type.cast(child);
            if (child instanceof Container nested) {
                T result = accessible(nested, type, name);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T result = find(nested, type);
                if (result != null) return result;
            }
        }
        return null;
    }
}
