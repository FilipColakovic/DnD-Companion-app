package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.card;
import static dndcompanion.ui.character.CharacterSheetComponents.metric;
import static dndcompanion.ui.character.CharacterSheetComponents.signed;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.EnumMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.border.EmptyBorder;

final class CharacterAbilityPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final EnumMap<Ability, JSpinner> scores = new EnumMap<>(Ability.class);
    private final EnumMap<Ability, JLabel> modifiers = new EnumMap<>(Ability.class),
            saves = new EnumMap<>(Ability.class);
    private final JLabel dc = AppStyle.label("", 15, true);

    CharacterAbilityPanel(CharacterFormBindings bindings) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel attributes = card("ABILITY ATTRIBUTES");
        JPanel matrix = new JPanel(new GridLayout(0, 2, 7, 7));
        matrix.setOpaque(false);
        for (Ability ability : Ability.values()) {
            JPanel tile =
                    AppStyle.surfacePanel(
                            new BorderLayout(0, 5),
                            ability == Ability.CHA ? "slotAvailable" : "low");
            tile.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(AppStyle.LINE),
                            new EmptyBorder(8, 8, 8, 8)));
            JPanel label = AppStyle.section(new BorderLayout());
            label.add(AppStyle.mono(ability.name(), 11), BorderLayout.WEST);
            JLabel saving = AppStyle.mono("", 10);
            saves.put(ability, saving);
            label.add(saving, BorderLayout.EAST);
            tile.add(label, BorderLayout.NORTH);
            JPanel value = AppStyle.section(new BorderLayout(8, 0));
            JLabel modifier = AppStyle.label("", 23, true);
            modifiers.put(ability, modifier);
            value.add(modifier, BorderLayout.WEST);
            JSpinner score =
                    bindings.spinner(
                            ability.name() + " score",
                            ability == Ability.CHA ? editor.snapshot().charisma() : 10,
                            1,
                            30,
                            number -> {
                                if (ability == Ability.CHA) editor.setCharismaScore(number);
                                else session.setSheet(session.sheet().withAbility(ability, number));
                            });
            scores.put(ability, score);
            value.add(score, BorderLayout.EAST);
            tile.add(value, BorderLayout.CENTER);
            tile.add(AppStyle.mono("TOTAL (MODIFIER) / BASE SCORE", 9), BorderLayout.SOUTH);
            matrix.add(tile);
        }
        FeatComponents.row(attributes, matrix);
        FeatComponents.row(attributes, metric("SORCERER SPELL SAVE DC", dc));
        add(attributes);
    }

    void refresh(
            CharacterState state,
            CharacterSheet sheet,
            SorcererRules.Stats stats,
            boolean restored) {
        dc.setText(Integer.toString(stats.spellSaveDc()));
        for (Ability ability : Ability.values()) {
            int score = sheet.score(ability, state.charisma()),
                    total = session.effectiveScore(ability, state.charisma()),
                    modifier = CharacterSheet.modifier(total);
            scores.get(ability).setValue(score);
            scores.get(ability).setToolTipText("Base score " + score + "; with feats " + total);
            modifiers.get(ability).setText(total + " (" + signed(modifier) + ")");
            modifiers
                    .get(ability)
                    .setToolTipText(
                            "Effective score: " + total + " (feat bonus +" + (total - score) + ")");
            boolean trained = session.saveProficiency(ability);
            saves.get(ability)
                    .setText(
                            "SAVE "
                                    + signed(modifier + (trained ? stats.proficiencyBonus() : 0))
                                    + (trained ? " P" : ""));
            saves.get(ability)
                    .setToolTipText(
                            trained ? "Sorcerer saving throw proficiency" : "Ability modifier");
        }
    }
}
