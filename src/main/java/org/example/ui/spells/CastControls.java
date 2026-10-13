package org.example.ui.spells;

import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

final class CastControls extends JPanel {
    final JLabel cost = AppStyle.label("No spell selected", 13, false);
    final JTextArea result = AppStyle.text("");
    final JButton castButton;

    CastControls(Runnable cast) {
        super(new BorderLayout());
        setOpaque(false);
        castButton = AppStyle.primaryButton("Cast", cast);
        JPanel card = AppStyle.card(new BorderLayout(12, 4));
        card.setBorder(new EmptyBorder(9, 14, 9, 14));
        JPanel words = AppStyle.section(new BorderLayout(0, 3));
        words.add(cost, BorderLayout.NORTH);
        result.setRows(1);
        words.add(result, BorderLayout.CENTER);
        card.add(words, BorderLayout.CENTER);
        castButton.setPreferredSize(new Dimension(160, 48));
        card.add(castButton, BorderLayout.EAST);
        add(card);
    }
}
