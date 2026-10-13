package org.example.ui.common;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.Icon;

/** Small vector book icon that remains readable without an emoji font or image asset. */
public final class SpellBookIcon implements Icon {
    public int getIconWidth() {
        return 24;
    }

    public int getIconHeight() {
        return 24;
    }

    public void paintIcon(Component component, Graphics graphics, int x, int y) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.translate(x, y);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(AppStyle.ACCENT);
        g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawRoundRect(3, 3, 18, 18, 3, 3);
        g.drawLine(7, 3, 7, 21);
        g.drawLine(11, 8, 17, 8);
        g.drawLine(11, 12, 17, 12);
        g.drawLine(11, 16, 15, 16);
        g.dispose();
    }
}
