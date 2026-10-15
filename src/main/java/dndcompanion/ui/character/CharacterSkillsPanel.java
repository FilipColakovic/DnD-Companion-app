package dndcompanion.ui.character;

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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

final class CharacterSkillsPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final EnumMap<Skill, SkillProficiencyRow> skillRows = new EnumMap<>(Skill.class);
    private final JLabel proficient = legend(false), expertise = legend(true);
    private final JCheckBox impersonating = new JCheckBox("Disguised impersonation"),
            hiding = new JCheckBox("Hide during combat");

    CharacterSkillsPanel(CharacterFormBindings bindings, Consumer<Skill> rollSkill) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel skills = FeatComponents.card(new GridBagLayout());
        JLabel title = AppStyle.mono("SKILL PROFICIENCIES", 13);
        title.setFont(title.getFont().deriveFont(Font.BOLD));
        JPanel legend = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        legend.add(proficient);
        legend.add(expertise);
        JPanel heading =
                new JPanel(new BorderLayout(4, 4)) {
                    private void arrange() {
                        boolean wrap =
                                getWidth() > 0
                                        && title.getPreferredSize().width
                                                        + legend.getPreferredSize().width
                                                        + 4
                                                > getWidth();
                        String position = wrap ? BorderLayout.SOUTH : BorderLayout.EAST;
                        if (!position.equals(((BorderLayout) getLayout()).getConstraints(legend))) {
                            remove(legend);
                            add(legend, position);
                        }
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        arrange();
                        return super.getPreferredSize();
                    }

                    @Override
                    public void doLayout() {
                        arrange();
                        super.doLayout();
                    }

                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(AppStyle.LINE);
                        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                    }
                };
        heading.setOpaque(false);
        heading.setName("skill-proficiencies-heading");
        heading.setBorder(new EmptyBorder(0, 0, 6, 0));
        heading.add(title);
        heading.add(legend, BorderLayout.EAST);
        FeatComponents.row(skills, heading);
        FeatComponents.row(skills, impersonating);
        FeatComponents.row(skills, hiding);
        for (Skill skill : Skill.values()) {
            SkillProficiencyRow row =
                    new SkillProficiencyRow(
                            skill,
                            tier -> {
                                if (!bindings.updating.getAsBoolean())
                                    session.setSkillTier(skill, tier);
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
        proficient.setText("PROF (" + signed(stats.proficiencyBonus()) + ")");
        expertise.setText("EXP (" + signed(stats.proficiencyBonus() * 2) + ")");
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
        return !session.sorcerer() && session.selection().level() >= 3 && skill == Skill.ATHLETICS
                || session.feats().containsKey("Actor")
                        && impersonating.isSelected()
                        && (skill == Skill.DECEPTION || skill == Skill.PERFORMANCE)
                || session.feats().containsKey("Skulker")
                        && hiding.isSelected()
                        && skill == Skill.STEALTH;
    }

    private static JLabel legend(boolean expert) {
        JLabel label = AppStyle.mono("", 9);
        label.setIconTextGap(3);
        label.setIcon(
                new Icon() {
                    public int getIconWidth() {
                        return 8;
                    }

                    public int getIconHeight() {
                        return 8;
                    }

                    public void paintIcon(Component component, Graphics g, int x, int y) {
                        g.setColor(expert ? AppStyle.ACCENT : AppStyle.ERROR);
                        g.fillRect(x, y, 8, 8);
                    }
                });
        label.setToolTipText(
                "Click a skill square: untrained → proficient → expertise → untrained. Bonuses"
                        + " follow your level.");
        return label;
    }
}
