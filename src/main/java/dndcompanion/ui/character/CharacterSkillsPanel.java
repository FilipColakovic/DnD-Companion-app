package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.card;
import static dndcompanion.ui.character.CharacterSheetComponents.signed;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Skill;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class CharacterSkillsPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final EnumMap<Skill, SkillProficiencyRow> skillRows = new EnumMap<>(Skill.class);
    private final JLabel skillLegend = AppStyle.mono("", 11);
    private final JCheckBox impersonating = new JCheckBox("Disguised impersonation"),
            hiding = new JCheckBox("Hide during combat");

    CharacterSkillsPanel(CharacterFormBindings bindings, Consumer<Skill> rollSkill) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel skills = card("SKILL PROFICIENCIES");
        skillLegend.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        FeatComponents.row(skills, skillLegend);
        FeatComponents.row(skills, impersonating);
        FeatComponents.row(skills, hiding);
        for (Skill skill : Skill.values()) {
            SkillProficiencyRow row =
                    new SkillProficiencyRow(
                            skill,
                            tier -> {
                                if (!bindings.updating.getAsBoolean())
                                    session.setSheet(session.sheet().withSkill(skill, tier));
                            },
                            () -> rollSkill.accept(skill));
            skillRows.put(skill, row);
            FeatComponents.row(skills, row);
        }
        add(skills);
    }

    void refresh(
            CharacterState state,
            CharacterSheet sheet,
            SorcererRules.Stats stats,
            boolean restored) {
        skillLegend.setText(
                "■ PROF ("
                        + signed(stats.proficiencyBonus())
                        + ")   ★ EXP ("
                        + signed(stats.proficiencyBonus() * 2)
                        + ")");
        skillLegend.setToolTipText(
                "Click a square: untrained → proficient → expertise → untrained. Bonuses"
                        + " calculate from your ability scores and level.");
        Set<Skill> granted = session.grantedSkills();
        impersonating.setVisible(session.feats().containsKey("Actor"));
        hiding.setVisible(session.feats().containsKey("Skulker"));
        skillRows
                .values()
                .forEach(
                        row ->
                                row.refresh(
                                        session.effectiveSheet(),
                                        session.effectiveScore(
                                                CharacterSheet.Ability.CHA, state.charisma()),
                                        stats.proficiencyBonus(),
                                        granted,
                                        session.expertiseSkills()));
    }

    boolean advantage(Skill skill) {
        return session.feats().containsKey("Actor")
                        && impersonating.isSelected()
                        && (skill == Skill.DECEPTION || skill == Skill.PERFORMANCE)
                || session.feats().containsKey("Skulker")
                        && hiding.isSelected()
                        && skill == Skill.STEALTH;
    }
}
