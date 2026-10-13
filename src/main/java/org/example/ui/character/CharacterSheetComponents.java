package org.example.ui.character;

import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

final class CharacterSheetComponents {
    private CharacterSheetComponents() {}

    static JPanel column() {
        return AppStyle.section(new GridBagLayout());
    }

    static JPanel card(String title) {
        JPanel card = FeatComponents.card(new GridBagLayout());
        FeatComponents.row(card, AppStyle.label(title, 15, true));
        return card;
    }

    static JPanel labelled(String title, JComponent component) {
        JPanel panel = AppStyle.section(new BorderLayout(0, 5));
        panel.add(AppStyle.mono(title, 10), BorderLayout.NORTH);
        panel.add(component, BorderLayout.CENTER);
        return panel;
    }

    static JPanel metric(String title, JLabel value) {
        JPanel panel = AppStyle.surfacePanel(new BorderLayout(0, 5), "low");
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(7, 7, 7, 7)));
        panel.add(AppStyle.mono(title, 9), BorderLayout.NORTH);
        panel.add(value, BorderLayout.CENTER);
        return panel;
    }

    static String signed(int number) {
        return String.format("%+d", number);
    }

    static void text(JTextComponent component, String value) {
        if (!component.getText().equals(value)) component.setText(value);
    }
}
