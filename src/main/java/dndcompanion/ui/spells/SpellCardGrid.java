package dndcompanion.ui.spells;

import dndcompanion.ui.common.AppStyle;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.Arrays;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

final class SpellCardGrid extends JPanel implements Scrollable {
    SpellCardGrid() {
        super(new CardRows());
        setName("spell-card-grid");
        putClientProperty("surfaceRole", "background");
    }

    private int columns() {
        int width = getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth();
        return width >= 580 + Math.max(0, AppStyle.textSize() - 14) * 30 ? 2 : 1;
    }

    @Override
    public void doLayout() {
        ((GridLayout) getLayout()).setColumns(columns());
        super.doLayout();
    }

    @Override
    public Dimension getPreferredSize() {
        ((GridLayout) getLayout()).setColumns(columns());
        return super.getPreferredSize();
    }

    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(650, 600);
    }

    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    public boolean getScrollableTracksViewportHeight() {
        return false;
    }

    public int getScrollableUnitIncrement(Rectangle r, int o, int d) {
        return 48;
    }

    public int getScrollableBlockIncrement(Rectangle r, int o, int d) {
        return Math.max(48, r.height - 48);
    }
}

/** Equal widths with heights measured per pair, so one long spell does not stretch every card. */
final class CardRows extends GridLayout {
    CardRows() {
        super(0, 2, 8, 8);
    }

    private int[] heights(Container parent, int width) {
        Component[] children = parent.getComponents();
        int columns = getColumns();
        int[] heights = new int[(children.length + columns - 1) / columns];
        for (int index = 0; index < children.length; index++) {
            Component child = children[index];
            child.setSize(width, Math.max(1, child.getPreferredSize().height));
            layoutChildren(child);
            heights[index / columns] =
                    Math.max(heights[index / columns], child.getPreferredSize().height);
        }
        return heights;
    }

    private static void layoutChildren(Component child) {
        if (child instanceof Container container) {
            container.doLayout();
            for (Component nested : container.getComponents()) layoutChildren(nested);
        }
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
        int width = parent.getWidth() > 0 ? parent.getWidth() : 650;
        int columnWidth = Math.max(1, (width - (getColumns() - 1) * getHgap()) / getColumns());
        int[] heights = heights(parent, columnWidth);
        return new Dimension(
                width, Arrays.stream(heights).sum() + Math.max(0, heights.length - 1) * getVgap());
    }

    @Override
    public void layoutContainer(Container parent) {
        int columns = getColumns();
        int width = Math.max(1, (parent.getWidth() - (columns - 1) * getHgap()) / columns);
        int[] heights = heights(parent, width);
        int y = 0;
        Component[] children = parent.getComponents();
        for (int row = 0; row < heights.length; row++) {
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index < children.length)
                    children[index].setBounds(column * (width + getHgap()), y, width, heights[row]);
            }
            y += heights[row] + getVgap();
        }
    }
}
