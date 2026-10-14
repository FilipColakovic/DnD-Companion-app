package dndcompanion.ui.common;

import java.awt.geom.Path2D;
import java.util.StringTokenizer;

/** Fixed vector artwork paths; no runtime SVG dependency. */
final class IconPaths {
    private IconPaths() {}

    static Path2D polygon(double... points) {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2) path.lineTo(points[i], points[i + 1]);
        path.closePath();
        return path;
    }

    /** Only the absolute path commands used by these fixed artwork glyphs. */
    static Path2D path(String data) {
        Path2D path = new Path2D.Double();
        StringTokenizer tokens = new StringTokenizer(data);
        while (tokens.hasMoreTokens()) {
            switch (tokens.nextToken()) {
                case "M" -> path.moveTo(number(tokens), number(tokens));
                case "L" -> path.lineTo(number(tokens), number(tokens));
                case "C" ->
                        path.curveTo(
                                number(tokens),
                                number(tokens),
                                number(tokens),
                                number(tokens),
                                number(tokens),
                                number(tokens));
                case "Q" ->
                        path.quadTo(number(tokens), number(tokens), number(tokens), number(tokens));
                case "Z" -> path.closePath();
                default -> throw new IllegalArgumentException("Unsupported icon path command");
            }
        }
        return path;
    }

    static double number(StringTokenizer tokens) {
        return Double.parseDouble(tokens.nextToken());
    }
}
