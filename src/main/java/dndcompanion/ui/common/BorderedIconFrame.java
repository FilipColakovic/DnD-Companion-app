package dndcompanion.ui.common;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/** Shared tactile border from the supplied Metamagic and navigation icon sets. */
final class BorderedIconFrame {
    private static final Color INK = new Color(0x1a1a1a), CREAM = new Color(0xeae2d5);

    private BorderedIconFrame() {}

    static void paint(Graphics2D g) {
        g.setColor(INK);
        g.fillRoundRect(2, 2, 64, 64, 8, 8);
        g.setColor(CREAM);
        g.fillRoundRect(0, 0, 64, 64, 8, 8);
        g.setColor(INK);
        g.setStroke(new BasicStroke(2.5f));
        g.drawRoundRect(0, 0, 64, 64, 8, 8);
        g.setColor(new Color(0xfdfbf7));
        g.fillRoundRect(2, 2, 60, 60, 4, 4);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRoundRect(2, 2, 60, 60, 4, 4);
        Graphics2D inset = (Graphics2D) g.create();
        inset.setComposite(java.awt.AlphaComposite.SrcOver.derive(.4f));
        inset.setColor(INK);
        inset.setStroke(
                new BasicStroke(
                        1,
                        BasicStroke.CAP_BUTT,
                        BasicStroke.JOIN_ROUND,
                        10,
                        new float[] {2, 1},
                        0));
        inset.draw(new java.awt.geom.RoundRectangle2D.Double(4, 4, 56, 56, 4, 4));
        inset.dispose();
        g.setColor(INK);
        for (int cornerX : new int[] {2, 59}) {
            for (int cornerY : new int[] {2, 59}) g.fillRect(cornerX, cornerY, 3, 3);
        }
    }
}
