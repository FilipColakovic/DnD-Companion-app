package org.example.ui.common;

import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.Path2D;

/** Small vector icons; their colors follow the current theme and parent control. */
public final class ArcaneIcon implements Icon {
    public enum Kind { BOLT, BOOK, SPARK, LIBRARY, SETTINGS, MOON, SUN, REST, SHIELD, DICE, PERSON }
    private final Kind kind;
    private final int size;
    public ArcaneIcon(Kind kind) { this(kind, 22); }
    public ArcaneIcon(Kind kind, int size) { this.kind = kind; this.size = size; }
    @Override public int getIconWidth() { return size; }
    @Override public int getIconHeight() { return size; }
    @Override public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = AppStyle.graphics(graphics);
        g.translate(x, y); g.scale(size / 24d, size / 24d);
        g.setColor(component != null && component.getForeground() != null ? component.getForeground() : AppStyle.ACCENT);
        g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        switch (kind) {
            case BOLT -> {
                Path2D p = path(14, 2, 5, 13, 11, 13, 9, 22, 20, 10, 14, 10); p.closePath(); g.draw(p);
            }
            case BOOK -> {
                g.draw(path(12, 6, 7, 4, 3, 5, 3, 20, 7, 19, 12, 21, 17, 19, 21, 20, 21, 5, 17, 4, 12, 6, 12, 21));
                g.drawLine(6, 9, 9, 10); g.drawLine(15, 10, 18, 9); g.drawLine(6, 13, 9, 14); g.drawLine(15, 14, 18, 13);
            }
            case SPARK -> {
                Path2D p = path(12, 2, 14, 9, 22, 12, 14, 15, 12, 22, 10, 15, 2, 12, 10, 9); p.closePath(); g.draw(p);
                g.drawLine(19, 3, 19, 7); g.drawLine(17, 5, 21, 5);
            }
            case LIBRARY -> {
                g.drawRoundRect(3, 4, 5, 16, 1, 1); g.drawRoundRect(9, 4, 5, 16, 1, 1);
                g.draw(path(15, 5, 19, 4, 23, 19, 19, 20, 15, 5));
                g.drawLine(4, 8, 7, 8); g.drawLine(10, 8, 13, 8);
            }
            case SETTINGS -> {
                g.drawOval(4, 4, 16, 16); g.drawOval(9, 9, 6, 6);
                for (int i = 0; i < 8; i++) { double a = Math.PI * i / 4; g.drawLine((int)(12 + 8*Math.cos(a)),(int)(12 + 8*Math.sin(a)),(int)(12 + 10*Math.cos(a)),(int)(12 + 10*Math.sin(a))); }
            }
            case MOON -> { g.drawArc(3, 3, 18, 18, 60, 290); g.drawArc(9, 1, 14, 14, 160, 220); }
            case SUN -> {
                g.drawOval(7, 7, 10, 10);
                for (int i = 0; i < 8; i++) { double a = Math.PI * i / 4; g.drawLine((int)(12 + 8*Math.cos(a)),(int)(12 + 8*Math.sin(a)),(int)(12 + 11*Math.cos(a)),(int)(12 + 11*Math.sin(a))); }
            }
            case REST -> { g.drawLine(3, 19, 21, 19); g.drawLine(3, 14, 21, 14); g.drawLine(3, 10, 3, 22); g.drawLine(21, 14, 21, 22); g.drawRoundRect(5, 10, 7, 4, 2, 2); g.drawLine(14, 5, 20, 5); g.drawLine(20, 5, 14, 10); g.drawLine(14, 10, 20, 10); }
            case SHIELD -> { Path2D p = path(12, 2, 21, 6, 20, 14, 17, 19, 12, 22, 7, 19, 4, 14, 3, 6); p.closePath(); g.draw(p); }
            case DICE -> { g.drawRoundRect(3, 3, 18, 18, 4, 4); g.fillOval(6, 6, 3, 3); g.fillOval(15, 6, 3, 3); g.fillOval(10, 10, 3, 3); g.fillOval(6, 15, 3, 3); g.fillOval(15, 15, 3, 3); }
            case PERSON -> { g.drawOval(8, 3, 8, 8); g.drawArc(4, 13, 16, 14, 0, 180); }
        }
        g.dispose();
    }
    private static Path2D path(double... points) {
        Path2D path = new Path2D.Double(); path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) { path.lineTo(points[i], points[i + 1]); }
        return path;
    }
}
