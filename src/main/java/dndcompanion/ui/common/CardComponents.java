package dndcompanion.ui.common;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.LayoutManager;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;

/** Library primitives with configurable palettes and existing card geometry. */
public final class CardComponents {
    public static final int LEVEL_TILE_SIZE = 44, LEVEL_TILE_SHADOW = 2, TITLE_SIZE = 16;

    private CardComponents() {}

    public static JPanel card(LayoutManager layout, Insets padding, String surface) {
        JPanel card = AppStyle.card(layout);
        card.setBorder(new EmptyBorder(padding));
        if (surface != null) card.putClientProperty("surfaceRole", surface);
        return card;
    }

    public static JLabel badge(
            String text, int size, boolean bold, String surface, String ink, Insets padding) {
        JLabel label = AppStyle.mono(text, size);
        if (bold) label.setFont(label.getFont().deriveFont(Font.BOLD));
        label.setOpaque(true);
        label.putClientProperty("surfaceRole", surface);
        AppStyle.setColorRole(label, ink);
        label.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE), new EmptyBorder(padding)));
        return label;
    }

    public static JPanel levelTile(String value, boolean yellow, String tooltip) {
        JPanel tile =
                card(new BorderLayout(), new Insets(3, 3, 5, 5), yellow ? "slotAvailable" : "low");
        tile.putClientProperty("shadowOffset", LEVEL_TILE_SHADOW);
        tile.setPreferredSize(new Dimension(LEVEL_TILE_SIZE, LEVEL_TILE_SIZE));
        JLabel label = AppStyle.mono(value, 20);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        AppStyle.setColorRole(label, yellow ? "badgeText" : "ink");
        label.setToolTipText(tooltip);
        tile.add(label);
        return tile;
    }

    public static JPanel metadataStrip(javax.swing.JComponent content) {
        JPanel panel = AppStyle.surfacePanel(new BorderLayout(), "low");
        panel.setBorder(new EmptyBorder(4, 6, 4, 6));
        panel.add(content);
        return panel;
    }

    public static JButton title(String name, Runnable action) {
        JButton button =
                new JButton(
                        "<html><u>"
                                + name.replace("&", "&amp;")
                                        .replace("<", "&lt;")
                                        .replace(">", "&gt;")
                                + "</u></html>") {
                    @Override
                    public Dimension getPreferredSize() {
                        int width = getWidth() > 0 ? getWidth() : 180;
                        Object renderer = getClientProperty(BasicHTML.propertyKey);
                        if (renderer instanceof View view) {
                            view.setSize(width, Float.MAX_VALUE);
                            return new Dimension(
                                    width, (int) Math.ceil(view.getPreferredSpan(View.Y_AXIS)));
                        }
                        return super.getPreferredSize();
                    }

                    @Override
                    public Dimension getMinimumSize() {
                        return new Dimension(0, getPreferredSize().height);
                    }
                };
        button.setUI(new BasicButtonUI());
        button.putClientProperty("tactileCustomPaint", true);
        button.setFont(new Font("Segoe UI", Font.BOLD, TITLE_SIZE));
        AppStyle.setColorRole(button, "ink");
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setVerticalAlignment(SwingConstants.TOP);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setToolTipText(name);
        button.addActionListener(event -> action.run());
        return button;
    }
}
