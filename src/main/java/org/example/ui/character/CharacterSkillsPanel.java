package org.example.ui.character;

import static org.example.ui.character.CharacterSheetComponents.card;
import static org.example.ui.character.CharacterSheetComponents.signed;

import org.example.application.CharacterController;
import org.example.domain.character.CharacterSheet;
import org.example.domain.character.CharacterSheet.Skill;
import org.example.domain.character.CharacterState;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.EnumMap;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.JLabel;
import javax.swing.JPanel;

final class CharacterSkillsPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final EnumMap<Skill, SkillProficiencyRow> skillRows = new EnumMap<>(Skill.class);
    private final JLabel skillLegend = AppStyle.mono("", 11);

    CharacterSkillsPanel(CharacterFormBindings bindings, Consumer<Skill> rollSkill) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel skills = card("SKILL PROFICIENCIES");
        skillLegend.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 11));
        FeatComponents.row(skills, skillLegend);
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
        skillRows
                .values()
                .forEach(
                        row ->
                                row.refresh(
                                        sheet,
                                        state.charisma(),
                                        stats.proficiencyBonus(),
                                        granted,
                                        session.expertiseSkills()));
    }
}
