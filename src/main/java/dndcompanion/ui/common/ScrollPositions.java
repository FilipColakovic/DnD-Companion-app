package dndcompanion.ui.common;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

/** Navigation resets scroll positions without replacing widgets or modifying editor drafts. */
public final class ScrollPositions {
    private ScrollPositions() {}

    public static void openAtTop(JComponent page) {
        reset(page);
        // CardLayout sizing and focus-driven scrolling settle after the navigation action.
        SwingUtilities.invokeLater(
                () -> {
                    if (page.isShowing()) reset(page);
                });
    }

    private static void reset(Component component) {
        if (component instanceof JScrollPane scroll) {
            scroll.getViewport().setViewPosition(new Point());
            scroll.getVerticalScrollBar().setValue(scroll.getVerticalScrollBar().getMinimum());
            scroll.getHorizontalScrollBar().setValue(scroll.getHorizontalScrollBar().getMinimum());
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) reset(child);
    }
}
