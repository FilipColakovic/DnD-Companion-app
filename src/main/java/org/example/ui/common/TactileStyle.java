package org.example.ui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;

import javax.swing.Icon;
import javax.swing.JLabel;

/** Geometric combat heading icons for the shared Bauhaus theme. */
public final class TactileStyle {
    public static final Color YELLOW = new Color(0xffcc00), BLUE = new Color(0x0055ff);

    private TactileStyle() {}

    public static JLabel heading(String title, Color accent) {
        JLabel label = AppStyle.heading(title, 17);
        label.setFont(new Font("Consolas", Font.BOLD, 17));
        label.setIcon(
                new Icon() {
                    public int getIconWidth() {
                        return 22;
                    }

                    public int getIconHeight() {
                        return 22;
                    }

                    public void paintIcon(Component c, Graphics graphics, int x, int y) {
                        Graphics2D g = AppStyle.graphics(graphics);
                        g.translate(x, y);
                        g.setStroke(new BasicStroke(2));
                        g.setColor(accent);
                        if (title.contains("Dice")) {
                            Polygon die =
                                    new Polygon(
                                            new int[] {11, 20, 20, 11, 2, 2},
                                            new int[] {1, 6, 16, 21, 16, 6},
                                            6);
                            g.fillPolygon(die);
                            g.setColor(AppStyle.INK);
                            g.drawPolygon(die);
                            g.drawLine(2, 6, 11, 11);
                            g.drawLine(20, 6, 11, 11);
                            g.drawLine(11, 11, 11, 21);
                        } else {
                            g.fillRect(3, 3, 15, 15);
                            g.setColor(AppStyle.INK);
                            g.drawRect(3, 3, 15, 15);
                        }
                        g.dispose();
                    }
                });
        label.setIconTextGap(9);
        return label;
    }
}
