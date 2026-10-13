package org.example.ui.common;

import org.example.domain.character.CharacterState;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.util.Locale;
import java.util.function.Supplier;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Compact live designation and resource matrix shared by main and secondary sidebars. */
public final class CharacterSummary extends JPanel {
    private final Supplier<CharacterState> character;
    private final PlaySession session;
    private final JLabel name = AppStyle.label("", 17, true), lineage = AppStyle.mono("", 12);
    private final JLabel hp = AppStyle.label("", 20, true),
            points = AppStyle.label("", 20, true),
            dc = AppStyle.label("", 16, true),
            attack = AppStyle.label("", 16, true),
            innate = AppStyle.label("", 16, true);
    private final JLabel[] slotCounts = new JLabel[9];
    private final JPanel[] slotCells = new JPanel[9];

    public CharacterSummary(Supplier<CharacterState> character, PlaySession session) {
        this.character = character;
        this.session = session;
        setOpaque(false);
        setLayout(new BorderLayout(0, 16));
        JPanel identity = AppStyle.section(new GridLayout(0, 1, 0, 6));
        identity.add(AppStyle.kicker("DESIGNATION"));
        identity.add(name);
        identity.add(lineage);
        name.setName("sidebar-designation");
        lineage.setName("sidebar-lineage");
        add(identity, BorderLayout.NORTH);
        JPanel resources = AppStyle.section(new BorderLayout(0, 10));
        JPanel top = AppStyle.section(new GridLayout(1, 2, 10, 0));
        JPanel health = tile("HIT POINTS", hp);
        health.setToolTipText("Click to edit current HP or choose calculated/manual maximum HP");
        health.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton edit = AppStyle.button("", this::editHp);
        edit.setName("edit-hit-points");
        edit.getAccessibleContext().setAccessibleName("Edit hit points");
        edit.setLayout(new BorderLayout());
        edit.add(health, BorderLayout.CENTER);
        edit.setBorder(new EmptyBorder(0, 0, 0, 0));
        wireHpClick(health);
        hp.setName("sidebar-hp");
        AppStyle.setColorRole(hp, "error");
        AppStyle.setColorRole(points, "accent");
        AppStyle.setColorRole(dc, "accent");
        AppStyle.setColorRole(innate, "error");
        dc.setName("sidebar-dc");
        points.setName("sidebar-sp");
        attack.setName("sidebar-atk");
        dc.setToolTipText(
                "8 + proficiency + Charisma modifier; +1 while Innate Sorcery is active for"
                        + " Sorcerer spells");
        top.add(edit);
        top.add(tile("SORCERY PTS", points));
        resources.add(top, BorderLayout.NORTH);
        JPanel middle = AppStyle.section(new GridLayout(1, 3, 8, 0));
        middle.add(tile("SAVE DC", dc));
        middle.add(tile("SPELL ATK", attack));
        middle.add(tile("INNATE", innate));
        resources.add(middle, BorderLayout.CENTER);
        JPanel matrix = AppStyle.card(new BorderLayout(0, 8));
        matrix.setBorder(new EmptyBorder(10, 10, 14, 14));
        JPanel heading = AppStyle.section(new BorderLayout());
        heading.add(AppStyle.kicker("SPELL SLOTS"), BorderLayout.WEST);
        heading.add(AppStyle.mono("L1–L9", 10), BorderLayout.EAST);
        heading.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, AppStyle.LINE));
        matrix.add(heading, BorderLayout.NORTH);
        JPanel cells = AppStyle.section(new GridLayout(1, 9, 3, 0));
        for (int i = 0; i < 9; i++) {
            JPanel cell = AppStyle.surfacePanel(new GridLayout(2, 1), "high");
            cell.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
            JLabel level = AppStyle.label("" + (i + 1), 10, true);
            level.setHorizontalAlignment(SwingConstants.CENTER);
            JLabel count = AppStyle.label("", 10, true);
            count.setHorizontalAlignment(SwingConstants.CENTER);
            count.setName("sidebar-slot-" + (i + 1));
            cell.add(level);
            cell.add(count);
            slotCells[i] = cell;
            slotCounts[i] = count;
            cells.add(cell);
        }
        matrix.add(cells, BorderLayout.CENTER);
        resources.add(matrix, BorderLayout.SOUTH);
        add(resources, BorderLayout.CENTER);
        refresh();
    }

    private static JPanel tile(String title, JLabel value) {
        JPanel tile = AppStyle.card(new BorderLayout(0, 6));
        tile.setBorder(new EmptyBorder(10, 3, 14, 7));
        JLabel label = AppStyle.label(title, 10, true);
        AppStyle.setColorRole(label, "muted");
        label.setHorizontalAlignment(SwingConstants.CENTER);
        value.setHorizontalAlignment(SwingConstants.CENTER);
        tile.add(label, BorderLayout.NORTH);
        tile.add(value, BorderLayout.CENTER);
        return tile;
    }

    public void refresh() {
        CharacterState state = character.get();
        var stats = SorcererRules.calculate(state.level(), state.charisma(), state.subclass());
        String full = state.name().toUpperCase(Locale.ROOT) + " // SORCERER";
        name.setText("<html>" + escape(full) + "</html>");
        name.setToolTipText(full);
        lineage.setText(
                String.format(
                        "LEVEL %02d // %s",
                        state.level(),
                        state.subclass()
                                .toString()
                                .replace(" Sorcery", "")
                                .toUpperCase(Locale.ROOT)));
        hp.setText(
                session.maxHitPoints() == 0
                        ? "—/—"
                        : session.hitPoints() + "/" + session.maxHitPoints());
        points.setText(session.remainingPoints() + "/" + session.maxPoints());
        dc.setText("" + (stats.spellSaveDc() + (session.innateActive() ? 1 : 0)));
        attack.setText(String.format("%+d", stats.spellAttack()));
        innate.setText(Math.max(0, 2 - state.play().innateUses()) + "/2");
        innate.setToolTipText(
                "Free Innate Sorcery uses remaining; additional uses at level 7+ cost 2 SP");
        for (int i = 0; i < 9; i++) {
            boolean unlocked = session.maxSlots(i + 1) > 0;
            slotCounts[i].setText(
                    unlocked ? session.remainingSlots(i + 1) + "/" + session.maxSlots(i + 1) : "–");
            slotCells[i].putClientProperty("surfaceRole", unlocked ? "slotAvailable" : "high");
            slotCells[i].setBackground(unlocked ? new Color(0xffcc00) : AppStyle.HIGH);
            slotCells[i].setToolTipText(
                    unlocked
                            ? "Level " + (i + 1) + " slots: " + slotCounts[i].getText()
                            : "Level " + (i + 1) + " not unlocked");
            for (Component child : slotCells[i].getComponents())
                AppStyle.setColorRole((JComponent) child, unlocked ? "badgeText" : "muted");
        }
    }

    private void editHp() {
        JSpinner current = new JSpinner(new SpinnerNumberModel(session.hitPoints(), 0, 100000, 1));
        JSpinner maximum =
                new JSpinner(new SpinnerNumberModel(session.maxHitPoints(), 0, 100000, 1));
        JCheckBox automatic = new JCheckBox("Calculate from class", session.automaticHp());
        maximum.setEnabled(!automatic.isSelected());
        automatic.addActionListener(
                event -> {
                    maximum.setEnabled(!automatic.isSelected());
                    if (automatic.isSelected()) maximum.setValue(session.hpCalculation().maximum());
                });
        JPanel fields = AppStyle.section(new GridLayout(0, 2, 10, 10));
        fields.add(AppStyle.label("Current HP", 14, false));
        fields.add(current);
        fields.add(AppStyle.label("Maximum HP", 14, false));
        fields.add(maximum);
        AppStyle.applyAppearance(fields);
        fields.add(automatic);
        fields.add(AppStyle.label("Fixed Sorcerer HP + CON", 11, false));
        AppStyle.applyAppearance(fields);
        while (JOptionPane.showConfirmDialog(
                        this,
                        fields,
                        "Hit points",
                        JOptionPane.OK_CANCEL_OPTION,
                        JOptionPane.PLAIN_MESSAGE)
                == JOptionPane.OK_OPTION) {
            try {
                current.commitEdit();
                maximum.commitEdit();
                int value = (Integer) current.getValue(),
                        limit =
                                automatic.isSelected()
                                        ? session.hpCalculation().maximum()
                                        : (Integer) maximum.getValue();
                if (value < 0 || value > limit)
                    throw new IllegalArgumentException("Invalid current HP");
                if (automatic.isSelected()) {
                    session.setAutomaticHp(true);
                    session.setCurrentHitPoints(value);
                } else session.setHitPoints(value, limit);
                return;
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        this, "Current HP must be between zero and maximum HP.");
            }
        }
    }

    private void wireHpClick(Component component) {
        component.addMouseListener(
                new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        editHp();
                    }
                });
        if (component instanceof Container container)
            for (Component child : container.getComponents()) wireHpClick(child);
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
