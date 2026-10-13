package org.example.ui.character;

import org.example.domain.character.CharacterSheet;
import org.example.domain.character.CharacterSheet.Skill;
import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Locale;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** A calculated skill check with a keyboard-accessible three-state proficiency marker. */
final class SkillProficiencyRow extends JPanel {
    private final JButton marker, label;
    private final JLabel bonus = AppStyle.mono("", 12), star = AppStyle.mono("★", 12);
    private final JLabel ability;
    private final Skill skill;
    private int tier;
    private boolean granted;

    SkillProficiencyRow(Skill skill, IntConsumer change, Runnable roll) {
        super(new BorderLayout(7, 0));
        this.skill = skill;
        setName("skill-row-" + skill);
        putClientProperty("surfaceRole", "low");
        marker =
                AppStyle.textButton(
                        "", () -> change.accept(granted ? (tier == 1 ? 2 : 0) : (tier + 1) % 3));
        marker.setPreferredSize(new Dimension(17, 22));
        marker.setFocusPainted(true);
        marker.getAccessibleContext().setAccessibleName("Proficiency for " + skill.label);
        add(marker, BorderLayout.WEST);
        JPanel identity = AppStyle.section(new BorderLayout(5, 0));
        label = AppStyle.textButton(skill.label.toUpperCase(Locale.ROOT), roll);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setToolTipText("Roll a " + skill.label + " check and record it in the combat log");
        identity.add(label, BorderLayout.CENTER);
        ability = AppStyle.mono("[" + skill.ability + "]", 10);
        identity.add(ability, BorderLayout.EAST);
        add(identity, BorderLayout.CENTER);
        JPanel total = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        bonus.setName("skill-bonus-" + skill);
        star.setFont(new Font("Segoe UI Symbol", Font.BOLD, 12));
        total.add(bonus);
        total.add(star);
        add(total, BorderLayout.EAST);
    }

    void refresh(
            CharacterSheet sheet,
            int charisma,
            int proficiency,
            java.util.Set<Skill> grantedSkills,
            java.util.Set<Skill> expertiseSkills) {
        granted = grantedSkills.contains(skill);
        tier = Math.max(sheet.tier(skill, grantedSkills), expertiseSkills.contains(skill) ? 2 : 0);
        marker.setEnabled(!expertiseSkills.contains(skill));
        putClientProperty(
                "surfaceRole", tier == 2 ? "slotAvailable" : tier == 1 ? "surface" : "low");
        setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE, tier > 0 ? 2 : 1),
                        new EmptyBorder(4, 6, 4, 6)));
        marker.setIcon(new TierIcon(tier));
        String state = tier == 0 ? "Untrained" : tier == 1 ? "Proficient" : "Expertise";
        String instruction =
                expertiseSkills.contains(skill)
                        ? "Expertise granted by a feat. Edit that choice on Character to change it."
                        : state
                                + " — click for "
                                + (tier == 0
                                        ? "proficiency"
                                        : tier == 1
                                                ? "expertise"
                                                : granted ? "proficiency" : "untrained")
                                + (granted
                                        ? ". Granted by a saved creation choice; edit that choice"
                                              + " to remove proficiency."
                                        : "");
        marker.getAccessibleContext().setAccessibleDescription(instruction);
        marker.setToolTipText(instruction);
        int value =
                org.example.domain.character.SkillRules.bonus(sheet.score(skill.ability, charisma), proficiency, tier);
        bonus.setText(String.format("%+d", value));
        star.setVisible(tier == 2);
        String ink = tier == 2 ? "badgeText" : "ink";
        AppStyle.setColorRole(label, ink);
        AppStyle.setColorRole(ability, ink);
        AppStyle.setColorRole(bonus, tier == 1 ? "error" : ink);
        AppStyle.setColorRole(star, "badgeText");
        setToolTipText(
                skill.label
                        + ": ability modifier "
                        + String.format(
                                "%+d",
                                CharacterSheet.modifier(sheet.score(skill.ability, charisma)))
                        + " + "
                        + tier
                        + " × proficiency bonus "
                        + proficiency
                        + " = "
                        + String.format("%+d", value));
    }

    private record TierIcon(int tier) implements Icon {
        public int getIconWidth() {
            return 11;
        }

        public int getIconHeight() {
            return 11;
        }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics g = graphics.create();
            g.setColor(
                    tier == 0
                            ? AppStyle.SURFACE
                            : tier == 1 ? new Color(0xe7362a) : new Color(0x0055ff));
            g.fillRect(x, y, 10, 10);
            g.setColor(AppStyle.LINE);
            g.drawRect(x, y, 10, 10);
            g.dispose();
        }
    }
}
