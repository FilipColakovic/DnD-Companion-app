package org.example.ui.spells;

import org.example.ui.common.AppStyle;
import org.example.ui.common.CardComponents;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

final class SpellLibraryComponents {
    private SpellLibraryComponents() {}

    static JLabel cardTag(String value, boolean yellow) {
        return CardComponents.badge(
                value.toUpperCase(Locale.ROOT),
                11,
                true,
                yellow ? "slotAvailable" : "high",
                yellow ? "badgeText" : "ink",
                new Insets(4, 6, 4, 6));
    }

    static JLabel coloredTag(String value, String surface) {
        JLabel label = cardTag(value, false);
        label.putClientProperty("surfaceRole", surface);
        AppStyle.setColorRole(label, "badgeLightText");
        return label;
    }

    static JLabel tag(String value, boolean yellow) {
        return CardComponents.badge(
                value.toUpperCase(Locale.ROOT),
                10,
                false,
                yellow ? "slotAvailable" : "high",
                yellow ? "badgeText" : "ink",
                new Insets(2, 4, 2, 4));
    }

    static JPanel block(String title, JComponent content) {
        JPanel panel = AppStyle.surfacePanel(new BorderLayout(0, 6), "high");
        panel.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(8, 8, 8, 8)));
        panel.add(AppStyle.mono(title, 10), BorderLayout.NORTH);
        panel.add(content);
        return panel;
    }

    static JTextArea wrapped(String value) {
        JTextArea area =
                new JTextArea(value) {
                    @Override
                    public Dimension getPreferredSize() {
                        int width = getWidth() > 0 ? getWidth() : 300;
                        getUI().getRootView(this).setSize(Math.max(1, width), Float.MAX_VALUE);
                        return new Dimension(
                                width,
                                (int)
                                        Math.ceil(
                                                getUI().getRootView(this)
                                                        .getPreferredSpan(
                                                                javax.swing.text.View.Y_AXIS)));
                    }
                };
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        return area;
    }

    static JButton filterButton(String text, Runnable action) {
        JButton button = AppStyle.compactButton(text, action);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        return button;
    }

    static JToggleButton toggle(String text, Runnable action) {
        JToggleButton button = new JToggleButton(text);
        AppStyle.roundButton(button);
        button.setFont(new Font("Segoe UI", Font.BOLD, 14));
        button.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(7, 9, 7, 9)));
        button.addActionListener(e -> action.run());
        return button;
    }

    static void row(JPanel panel, JComponent child) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = panel.getComponentCount();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 5, 0);
        panel.add(child, c);
    }

    static void glue(JPanel panel) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = panel.getComponentCount();
        c.weighty = 1;
        panel.add(Box.createVerticalGlue(), c);
    }

    static final class Wrap extends JPanel {
        Wrap() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 3));
            setOpaque(false);
            putClientProperty("surfaceRole", "background");
        }

        @Override
        public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 600;
            Container library = SwingUtilities.getAncestorOfClass(SpellLibraryPanel.class, this);
            if (library != null && library.getWidth() > 0)
                width = Math.max(100, Math.min(width, library.getWidth() - 100));
            int x = 0, height = 0, line = 0;
            for (Component child : getComponents()) {
                Dimension size = child.getPreferredSize();
                if (x > 0 && x + size.width > width - 10) {
                    height += line + 6;
                    x = 0;
                    line = 0;
                }
                x += size.width + 5;
                line = Math.max(line, size.height);
            }
            return new Dimension(width, height + line + 6);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
        }
    }
}
