package dndcompanion.ui.spells;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;

import javax.swing.JPanel;
import javax.swing.JViewport;
import javax.swing.Scrollable;

final class CombatArsenalGrid extends JPanel implements Scrollable {
    CombatArsenalGrid() {
        super(new GridLayout(0, 1, 6, 6));
        setOpaque(false);
        setName("combat-arsenal-grid");
    }

    private void columns(int width) {
        ((GridLayout) getLayout()).setColumns(width >= 490 ? 2 : 1);
    }

    @Override
    public Dimension getPreferredSize() {
        columns(getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth());
        return super.getPreferredSize();
    }

    @Override
    public void doLayout() {
        columns(getWidth());
        super.doLayout();
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(400, 235);
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
        return 64;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(64, visible.height - 64);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return false;
    }
}
