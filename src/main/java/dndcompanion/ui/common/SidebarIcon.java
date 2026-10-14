package dndcompanion.ui.common;

import static dndcompanion.ui.common.IconPaths.path;
import static dndcompanion.ui.common.IconPaths.polygon;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/** Scalable navigation artwork from the supplied sidebar-icons.svg export. */
public final class SidebarIcon implements Icon {
    private static final Color INK = new Color(0x1a1a1a),
            CREAM = new Color(0xeae2d5),
            BLUE = new Color(0x2563eb),
            RED = new Color(0xdc2626),
            GOLD = new Color(0xf59e0b);
    private final ArcaneIcon.Kind kind;
    private final int size;

    public SidebarIcon(ArcaneIcon.Kind kind, int size) {
        this.kind = java.util.Objects.requireNonNull(kind);
        if (size <= 0) throw new IllegalArgumentException("Icon size must be positive");
        this.size = size;
    }

    @Override
    public int getIconWidth() {
        return size;
    }

    @Override
    public int getIconHeight() {
        return size;
    }

    @Override
    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = AppStyle.graphics(graphics);
        g.translate(x, y);
        g.scale(size / 68d, size / 68d);
        g.translate(1.5, 1.5);
        BorderedIconFrame.paint(g);
        g.translate(32, 32);
        g.scale(.68, .68);
        switch (kind) {
            case BOLT -> {
                shape(g, polygon(5, -32, -18, 2, -2, 2, -8, 32, 20, -2, 2, -2), GOLD, 3);
                line(g, -22, -14, -10, -14, RED, 3);
                line(g, 10, 14, 22, 14, RED, 3);
                circle(g, -16, 16, 3, INK, 0);
                circle(g, 16, -16, 3, BLUE, 0);
            }
            case SPARK -> {
                shape(
                        g,
                        path("M 0 -30 Q 3 -10 24 0 Q 3 10 0 30 Q -3 10 -24 0 Q -3 -10 0 -30 Z"),
                        BLUE,
                        3);
                shape(
                        g,
                        path("M 0 -18 Q 2 -6 14 0 Q 2 6 0 18 Q -2 6 -14 0 Q -2 -6 0 -18 Z"),
                        GOLD,
                        2);
                circle(g, 0, 0, 4, Color.WHITE, 2);
                circle(g, -16, -16, 3, RED, 0);
                circle(g, 16, 16, 3, RED, 0);
            }
            case SHIELD -> {
                shape(
                        g,
                        path(
                                "M 0 -30 L 26 -14 L 26 10 C 26 24 0 34 0 34 C 0 34 -26 24 -26 10 L"
                                        + " -26 -14 Z"),
                        CREAM,
                        3);
                shape(
                        g,
                        path("M 0 -28 L 24 -13 L 24 9 C 24 22 0 31 0 31 Z"),
                        new Color(220, 38, 38, 242),
                        0);
                shape(
                        g,
                        path("M 0 -28 L -24 -13 L -24 9 C -24 22 0 31 0 31 Z"),
                        new Color(0xfbbf24),
                        0);
                line(g, 0, -28, 0, 31, INK, 2);
                shape(
                        g,
                        polygon(
                                0, -10, 3, -3, 10, -3, 4, 2, 6, 9, 0, 5, -6, 9, -4, 2, -10, -3, -3,
                                -3),
                        Color.WHITE,
                        1.5f);
            }
            case LIBRARY -> {
                shape(g, new RoundRectangle2D.Double(-24, -22, 10, 44, 2, 2), BLUE, 2.5f);
                line(g, -24, -10, -14, -10, Color.WHITE, 2);
                line(g, -24, 12, -14, 12, Color.WHITE, 2);
                shape(g, new RoundRectangle2D.Double(-10, -26, 12, 48, 2, 2), GOLD, 2.5f);
                circle(g, -4, -14, 2.5, INK, 0);
                line(g, -10, 14, 2, 14, INK, 2);
                Graphics2D leaning = (Graphics2D) g.create();
                leaning.translate(10, 0);
                leaning.rotate(Math.toRadians(16));
                shape(leaning, new RoundRectangle2D.Double(-6, -24, 12, 46, 2, 2), RED, 2.5f);
                line(leaning, -6, -8, 6, -8, Color.WHITE, 2);
                line(leaning, -6, 10, 6, 10, Color.WHITE, 2);
                leaning.dispose();
            }
            case PERSON -> {
                circle(g, 0, -14, 12, CREAM, 3);
                circle(g, 0, -14, 4, BLUE, 0);
                shape(g, path("M -24 24 C -24 8 -14 2 0 2 C 14 2 24 8 24 24 Z"), INK, 2.5f);
                shape(g, polygon(0, 2, -10, 14, 10, 14), GOLD, 1.5f);
                circle(g, 16, -22, 3, RED, 0);
            }
            default -> throw new IllegalArgumentException("No sidebar artwork for " + kind);
        }
        g.dispose();
    }

    private static void shape(Graphics2D g, Shape shape, Color fill, float stroke) {
        g.setColor(fill);
        g.fill(shape);
        if (stroke > 0) {
            g.setColor(INK);
            g.setStroke(new BasicStroke(stroke, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(shape);
        }
    }

    private static void line(
            Graphics2D g, double x1, double y1, double x2, double y2, Color color, float width) {
        g.setColor(color);
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x1, y1, x2, y2));
    }

    private static void circle(
            Graphics2D g, double x, double y, double radius, Color fill, float width) {
        shape(g, new Ellipse2D.Double(x - radius, y - radius, 2 * radius, 2 * radius), fill, width);
    }
}
