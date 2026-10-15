package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterSheet.Field;
import dndcompanion.domain.character.CharacterSheet.Skill;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlayState;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/** Live sheet inputs, derived checks, resource controls and backward-compatible checkpoints. */
public final class CharacterSheetChecks {
    public static void main(String[] args) throws Exception {
        check(
                CharacterSheet.modifier(1) == -5
                        && CharacterSheet.modifier(9) == -1
                        && CharacterSheet.modifier(30) == 10,
                "Ability modifiers round down");
        CharacterSheet data =
                CharacterSheet.defaults().withAbility(Ability.INT, 14).withSkill(Skill.ARCANA, 2);
        check(
                data.skillBonus(Skill.ARCANA, 17, 3) == 8,
                "Expertise doubles proficiency rather than the ability modifier");
        check(
                data.withAbility(Ability.CON, 1).hitDieHealing(1) == 1,
                "2024 Hit Die healing has a minimum of 1 HP");
        check(
                data.withAbility(Ability.CHA, 18).score(Ability.CHA, 10) == 18,
                "Canonical ability scores include Charisma; legacy fallback remains supported");
        try {
            data.withSkill(Skill.ARCANA, 3);
            throw new AssertionError("Invalid tier accepted");
        } catch (IllegalArgumentException expected) {
        }
        check(
                PlayState.decode("points=0\n").sheet().equals(CharacterSheet.defaults()),
                "Old saves initialize a neutral sheet without changing saved Charisma");
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel panel =
                            new GrimoirePanel(
                                    new CharacterState(
                                            5,
                                            17,
                                            SorcererRules.Subclass.ABERRANT,
                                            Set.of("Fire Bolt"),
                                            Species.HUMAN));
                    AtomicReference<CharacterState> saved = new AtomicReference<>();
                    panel.setSaveAction(
                            state -> {
                                saved.set(state);
                                panel.markSaved();
                            });
                    panel.setSaveActions(
                            locked -> {
                                if (locked) {
                                    saved.set(panel.snapshot());
                                    panel.markSaved();
                                }
                            },
                            () -> panel.restore(saved.get()));
                    JFrame frame = new JFrame();
                    frame.setContentPane(panel);
                    frame.setSize(1480, 1000);
                    frame.setVisible(true);
                    try {
                        panel.showPage(GrimoirePanel.CHARACTER);
                        CharacterSheetPanel sheet = find(panel, CharacterSheetPanel.class, null);
                        accessible(sheet, JSpinner.class, "DEX score").setValue(14);
                        accessible(sheet, JSpinner.class, "INT score").setValue(14);
                        accessible(sheet, JSpinner.class, "WIS score").setValue(18);
                        accessible(sheet, JSpinner.class, "CON score").setValue(14);
                        JButton arcana = accessible(sheet, JButton.class, "Proficiency for Arcana");
                        check(
                                findNamed(sheet, JLabel.class, "skill-bonus-ARCANA")
                                        .getText()
                                        .equals("+2"),
                                "Untrained skill automatically uses its ability modifier");
                        arcana.doClick(0);
                        check(
                                panel.playSession().sheet().tier(Skill.ARCANA) == 1
                                        && findNamed(sheet, JLabel.class, "skill-bonus-ARCANA")
                                                .getText()
                                                .equals("+5"),
                                "First marker click adds proficiency");
                        arcana.doClick(0);
                        check(
                                panel.playSession().sheet().tier(Skill.ARCANA) == 2,
                                "Second marker click sets expertise");
                        arcana.doClick(0);
                        check(
                                panel.playSession().sheet().tier(Skill.ARCANA) == 0
                                        && findNamed(sheet, JLabel.class, "skill-bonus-ARCANA")
                                                .getText()
                                                .equals("+2"),
                                "Third marker click returns to untrained");
                        arcana.doClick(0);
                        arcana.doClick(0);
                        accessible(sheet, JButton.class, "Proficiency for Perception").doClick(0);
                        check(
                                findNamed(sheet, JLabel.class, "skill-bonus-ARCANA")
                                        .getText()
                                        .equals("+8"),
                                "Skill row updates derived expertise bonus");
                        check(
                                findNamed(sheet, JLabel.class, "passive-PERCEPTION")
                                        .getText()
                                        .equals("17"),
                                "Passive sense updates with ability and proficiency");
                        accessible(sheet, JSpinner.class, "CHA score").setValue(18);
                        check(
                                panel.snapshot().charisma() == 18
                                        && panel.playSession().sheet().score(Ability.CHA, 18) == 18,
                                "Charisma retains one source of truth");
                        accessible(sheet, JSpinner.class, "Character level").setValue(8);
                        check(
                                panel.snapshot().level() == 8,
                                "Sheet level changes update the shared character");
                        accessible(sheet, JSpinner.class, "Maximum hit points").setValue(30);
                        accessible(sheet, JSpinner.class, "Current hit points").setValue(10);
                        accessible(sheet, JSpinner.class, "Temporary hit points").setValue(7);
                        accessible(sheet, JSpinner.class, "Remaining Sorcery Points").setValue(4);
                        accessible(sheet, JSpinner.class, "Remaining level 1 slots").setValue(2);
                        check(
                                panel.playSession().remainingPoints() == 4
                                        && panel.playSession().remainingSlots(1) == 2,
                                "Existing resource corrections remain available");
                        accessible(sheet, JCheckBox.class, "Death save failure 2").doClick(0);
                        check(
                                panel.playSession().sheet().field(Field.DEATH_FAILURES) == 2,
                                "Death-save markers update the tracker");
                        var hpBefore = panel.playSession().hitPoints();
                        button(sheet, "Spend d6 Hit Die").doClick(0);
                        check(
                                panel.playSession().sheet().field(Field.SPENT_HIT_DICE) == 1
                                        && panel.playSession().hitPoints() > hpBefore
                                        && panel.playSession().hitPoints() <= 30,
                                "Hit Die healing spends one die and respects maximum HP");
                        button(sheet, "Long Rest").doClick(0);
                        check(
                                panel.playSession().hitPoints() == 30
                                        && panel.playSession().sheet().field(Field.SPENT_HIT_DICE)
                                                == 0
                                        && panel.playSession().sheet().field(Field.TEMP_HP) == 0
                                        && panel.playSession().sheet().field(Field.DEATH_FAILURES)
                                                == 0,
                                "Long Rest restores HP and dice and clears temporary/death"
                                        + " trackers");
                        accessible(sheet, JSpinner.class, "Current hit points").setValue(0);
                        check(
                                !button(sheet, "Long Rest").isEnabled()
                                        && !button(sheet, "Short Rest").isEnabled(),
                                "Known 0 HP blocks starting rest actions");
                        accessible(sheet, JSpinner.class, "Current hit points").setValue(30);
                        accessible(sheet, JTextArea.class, "Training and equipment notes")
                                .setText("Simple weapons; equipped staff");
                        button(sheet, "Commit to grimoire").doClick(0);
                        check(
                                saved.get().equals(panel.snapshot())
                                        && PlayState.decode(saved.get().play().encode())
                                                .equals(saved.get().play()),
                                "Commit saves all sheet fields and their encoded state");
                        panel.setSaveLocked(true);
                        accessible(sheet, JSpinner.class, "DEX score").setValue(20);
                        check(
                                !button(sheet, "Commit to grimoire").isEnabled(),
                                "Save lock prevents overwriting a checkpoint");
                        button(sheet, "Discard edits").doClick(0);
                        check(
                                panel.snapshot().equals(saved.get())
                                        && accessible(sheet, JSpinner.class, "DEX score")
                                                .getValue()
                                                .equals(14),
                                "Discard restores the entire sheet and live controls");
                        for (boolean dark : new boolean[] {false, true})
                            for (int size : new int[] {12, 14, 16, 18}) {
                                panel.playSession().appearance(dark, size);
                                layout(frame);
                                layout(frame);
                                preview(
                                        panel,
                                        "target/character-sheet-"
                                                + (dark ? "dark" : "light")
                                                + "-"
                                                + size
                                                + ".png");
                            }
                        panel.playSession().appearance(false, 18);
                        frame.setSize(980, 900);
                        layout(frame);
                        layout(frame);
                        preview(panel, "target/character-sheet-narrow-18.png");
                    } finally {
                        frame.dispose();
                        panel.dispose();
                    }
                });
        System.out.println(
                "Passed: six abilities, 18 skills, saves and passives, vitals, rest/resource"
                        + " tracking, commit/restore, legacy saves and both theme layouts.");
    }

    private static JButton button(Container root, String text) {
        return find(root, JButton.class, text);
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        return walk(root, type, c -> name.equals(c.getAccessibleContext().getAccessibleName()));
    }

    private static <T extends Component> T findNamed(Container root, Class<T> type, String name) {
        return walk(root, type, c -> name.equals(c.getName()));
    }

    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        return walk(
                root,
                type,
                c ->
                        text == null
                                || c instanceof AbstractButton button
                                        && text.equals(button.getText()));
    }

    private static <T extends Component> T walk(
            Container root, Class<T> type, java.util.function.Predicate<Component> predicate) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && predicate.test(c)) return type.cast(c);
            if (c instanceof Container child) {
                T found = walk(child, type, predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) if (c instanceof Container child) layout(child);
    }

    private static void preview(JComponent panel, String file) {
        BufferedImage image =
                new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", Path.of(file).toFile());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
