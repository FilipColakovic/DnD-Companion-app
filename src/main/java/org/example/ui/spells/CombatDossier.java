package org.example.ui.spells;

import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class CombatDossier extends JPanel {
    final JTextArea preview = AppStyle.text("");
    final JLabel dossierTitle = AppStyle.heading("Spell dossier", 25),
            dossierSubtitle = AppStyle.label("Choose a prepared spell", 12, false);

    CombatDossier() {
        super(new BorderLayout());
        setOpaque(false);
        JPanel card = AppStyle.card(new BorderLayout(0, 10));
        JPanel heading = AppStyle.section(new GridLayout(0, 1, 0, 4));
        heading.add(dossierTitle);
        heading.add(dossierSubtitle);
        card.add(heading, BorderLayout.NORTH);
        card.setName("combat-spell-dossier");
        preview.setRows(11);
        JScrollPane scroll = new JScrollPane(preview);
        AppStyle.scrollPane(scroll);
        scroll.setPreferredSize(new Dimension(360, 230));
        card.add(scroll, BorderLayout.CENTER);
        add(card);
    }
}
