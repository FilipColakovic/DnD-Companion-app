package dndcompanion.ui.spells;

import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

/** Compact specifications and summary; complete rules remain available in the details window. */
final class CombatDossier extends JPanel {
    final JTextArea preview = AppStyle.text("");
    final JTextArea compatibility = AppStyle.text("");
    final JPanel specifications = AppStyle.surfacePanel(new GridLayout(0, 1), "low");
    private final Field casting = new Field("Casting:"),
            range = new Field("Range:"),
            duration = new Field("Duration:"),
            components = new Field("Components:"),
            checks = new Field("Save:"),
            concentration = new Field("Concentration:");
    final JLabel dossierTitle = AppStyle.heading("Spell dossier", 25),
            dossierSubtitle = AppStyle.mono("Choose a prepared spell", 13);
    private SpellCatalog.Spell spell;
    private final JButton details;

    CombatDossier() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel card = AppStyle.surfacePanel(new BorderLayout(0, 16), "lowest");
        card.setBorder(new EmptyBorder(8, 10, 2, 10));
        JPanel heading = AppStyle.section(new GridLayout(0, 1, 0, 4));
        heading.add(dossierTitle);
        heading.add(dossierSubtitle);
        AppStyle.setColorRole(dossierSubtitle, "muted");
        heading.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, AppStyle.LINE),
                        new EmptyBorder(0, 0, 8, 0)));
        JPanel top = AppStyle.section(new BorderLayout(0, 16));
        top.add(heading, BorderLayout.NORTH);
        specifications.setLayout(
                new javax.swing.BoxLayout(specifications, javax.swing.BoxLayout.Y_AXIS));
        specifications.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE, 2),
                        new EmptyBorder(4, 10, 4, 10)));
        specifications.add(row(casting, range));
        specifications.add(row(duration));
        specifications.add(row(components));
        specifications.add(row(checks, concentration));
        top.add(specifications);
        card.add(top, BorderLayout.NORTH);
        card.setName("combat-spell-dossier");
        preview.setName("combat-spell-summary");
        checks.value.setName("combat-spell-rolls");
        preview.setFont(new Font("Segoe UI", Font.BOLD, 14));
        preview.setBorder(new EmptyBorder(0, 0, 0, 0));
        compatibility.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        AppStyle.setColorRole(compatibility, "muted");
        JPanel summary = AppStyle.section(new BorderLayout(0, 12));
        summary.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 2, 0, 0, new java.awt.Color(0xf5a000)),
                        new EmptyBorder(0, 10, 0, 4)));
        summary.add(preview);
        summary.add(compatibility, BorderLayout.SOUTH);
        card.add(summary);
        details =
                AppStyle.compactButton(
                        "Full details",
                        () -> {
                            if (spell != null)
                                AppStyle.openWindow(
                                        this, spell.name(), new SpellDetailPanel(spell));
                        });
        details.getAccessibleContext().setAccessibleName("Combat spell full details");
        JPanel footer = AppStyle.section(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 0));
        footer.add(details);
        card.add(footer, BorderLayout.SOUTH);
        add(card);
    }

    void entry(SpellCatalog.Spell spell) {
        this.spell = spell;
        details.setVisible(spell != null);
    }

    void specifications(
            String castingText,
            String rangeText,
            String durationText,
            String componentsText,
            String rollText,
            boolean required) {
        casting.value.setText(castingText);
        range.value.setText(rangeText);
        duration.value.setText(durationText);
        components.value.setText(componentsText);
        boolean saveOnly = rollText.startsWith("Save DC") && !rollText.contains("Attack");
        checks.label.setText(
                saveOnly ? "Save:" : rollText.startsWith("Attack") ? "Attack:" : "Rolls:");
        checks.value.setText(
                saveOnly
                        ? rollText.substring(5)
                        : rollText.startsWith("Attack ") ? rollText.substring(7) : rollText);
        concentration.value.setText(required ? "Required" : "Not required");
        AppStyle.setColorRole(concentration.label, required ? "error" : "muted");
        specifications.setVisible(true);
    }

    private static JPanel row(Field... fields) {
        JPanel row =
                new JPanel(new GridLayout(1, fields.length, 12, 0)) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        super.paintComponent(g);
                        g.setColor(AppStyle.SOFT);
                        g.drawLine(0, getHeight() - 1, getWidth(), getHeight() - 1);
                    }
                };
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(5, 0, 5, 0));
        for (Field field : fields) row.add(field);
        return row;
    }

    private static final class Field extends JPanel {
        final JLabel label;
        final JTextArea value = AppStyle.text("");

        Field(String title) {
            super(new BorderLayout(6, 0));
            setOpaque(false);
            label = AppStyle.mono(title, 13);
            label.setFont(label.getFont().deriveFont(Font.BOLD));
            AppStyle.setColorRole(label, "muted");
            value.setFont(new Font(Font.MONOSPACED, Font.BOLD, 13));
            add(label, BorderLayout.WEST);
            add(value);
        }
    }
}
