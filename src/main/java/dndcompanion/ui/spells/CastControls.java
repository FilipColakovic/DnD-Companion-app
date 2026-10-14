package dndcompanion.ui.spells;

import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

final class CastControls extends JPanel {
    final JLabel cost = AppStyle.label("No spell selected", 13, false);
    final JTextArea result = AppStyle.text("");
    final JButton castButton;
    final JComboBox<String> source = new JComboBox<>();
    final JComboBox<String> damageType = new JComboBox<>();
    final JButton healing;

    CastControls(Runnable cast, Runnable rollHealing) {
        super(new BorderLayout());
        setOpaque(false);
        castButton = AppStyle.primaryButton("Cast", cast);
        healing = AppStyle.compactButton("Roll healing", rollHealing);
        JPanel card = AppStyle.card(new BorderLayout(12, 4));
        card.setBorder(new EmptyBorder(9, 14, 9, 14));
        JPanel words = AppStyle.section(new BorderLayout(0, 3));
        words.add(cost, BorderLayout.NORTH);
        source.getAccessibleContext().setAccessibleName("Casting source");
        source.setRenderer(
                new javax.swing.DefaultListCellRenderer() {
                    @Override
                    public java.awt.Component getListCellRendererComponent(
                            javax.swing.JList<?> list,
                            Object value,
                            int index,
                            boolean selected,
                            boolean focus) {
                        return super.getListCellRendererComponent(
                                list,
                                dndcompanion.domain.character.PlaySession.castSourceLabel(
                                        (String) value),
                                index,
                                selected,
                                focus);
                    }
                });
        words.add(source, BorderLayout.WEST);
        result.setRows(1);
        words.add(result, BorderLayout.CENTER);
        words.add(healing, BorderLayout.SOUTH);
        damageType.getAccessibleContext().setAccessibleName("Spell damage type");
        words.add(damageType, BorderLayout.EAST);
        card.add(words, BorderLayout.CENTER);
        castButton.setPreferredSize(new Dimension(160, 48));
        card.add(castButton, BorderLayout.EAST);
        add(card);
    }
}
