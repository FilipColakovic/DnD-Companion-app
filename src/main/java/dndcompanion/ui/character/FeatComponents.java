package dndcompanion.ui.character;

import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.CardComponents;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

/** Shared compact geometry for the feat library, cards and inspector. */
final class FeatComponents {
    private FeatComponents() {}

    static JPanel card(LayoutManager layout) {
        return CardComponents.card(layout, new Insets(12, 12, 16, 16), null);
    }

    static JLabel tag(String text, boolean yellow) {
        return CardComponents.badge(
                text,
                11,
                false,
                yellow ? "slotAvailable" : "high",
                yellow ? "badgeText" : "ink",
                new Insets(4, 6, 4, 6));
    }

    static JLabel colorTag(String text, String surface, String ink) {
        JLabel label = tag(text, false);
        label.putClientProperty("surfaceRole", surface);
        AppStyle.setColorRole(label, ink);
        return label;
    }

    static JButton title(String name, Runnable action) {
        return CardComponents.title(name, action);
    }

    /** Two cards per row where there is room, with heights measured for each pair. */
    static JPanel cardGrid() {
        JPanel grid =
                AppStyle.section(
                        new LayoutManager() {
                            private int columns(Container parent) {
                                return parent.getWidth()
                                                >= 580 + Math.max(0, AppStyle.textSize() - 14) * 30
                                        ? 2
                                        : 1;
                            }

                            private int[] heights(Container parent, int columns, int width) {
                                int[] heights =
                                        new int
                                                [(parent.getComponentCount() + columns - 1)
                                                        / columns];
                                int index = 0;
                                for (Component child : parent.getComponents()) {
                                    child.setSize(
                                            width, Math.max(1, child.getPreferredSize().height));
                                    layoutChildren(child);
                                    heights[index / columns] =
                                            Math.max(
                                                    heights[index / columns],
                                                    child.getPreferredSize().height);
                                    index++;
                                }
                                return heights;
                            }

                            public void addLayoutComponent(String name, Component child) {}

                            public void removeLayoutComponent(Component child) {}

                            public Dimension minimumLayoutSize(Container parent) {
                                return new Dimension(0, 0);
                            }

                            public Dimension preferredLayoutSize(Container parent) {
                                int width = parent.getWidth() > 0 ? parent.getWidth() : 650,
                                        cols = columns(parent);
                                int[] heights =
                                        heights(
                                                parent,
                                                cols,
                                                Math.max(1, (width - (cols - 1) * 8) / cols));
                                return new Dimension(
                                        width,
                                        java.util.Arrays.stream(heights).sum()
                                                + Math.max(0, heights.length - 1) * 8);
                            }

                            public void layoutContainer(Container parent) {
                                int cols = columns(parent),
                                        width =
                                                Math.max(
                                                        1,
                                                        (parent.getWidth() - (cols - 1) * 8)
                                                                / cols);
                                int[] heights = heights(parent, cols, width);
                                int y = 0, index = 0;
                                for (int height : heights) {
                                    for (int col = 0;
                                            col < cols && index < parent.getComponentCount();
                                            col++)
                                        parent.getComponent(index++)
                                                .setBounds(col * (width + 8), y, width, height);
                                    y += height + 8;
                                }
                            }
                        });
        grid.setName("feat-card-grid");
        return grid;
    }

    private static void layoutChildren(Component child) {
        if (child instanceof Container container) {
            container.doLayout();
            for (Component nested : container.getComponents()) layoutChildren(nested);
        }
    }

    static JTextArea text(String value, int size) {
        JTextArea text =
                new JTextArea(value) {
                    @Override
                    public Dimension getPreferredSize() {
                        int width = getWidth() > 0 ? getWidth() : 300;
                        if (getUI() == null) return new Dimension(width, 20);
                        Insets insets = getInsets();
                        var view = getUI().getRootView(this);
                        view.setSize(
                                Math.max(1, width - insets.left - insets.right), Float.MAX_VALUE);
                        return new Dimension(
                                width,
                                (int) Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS))
                                        + insets.top
                                        + insets.bottom);
                    }
                };
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setFont(new Font("Segoe UI", Font.PLAIN, size));
        AppStyle.setColorRole(text, "ink");
        return text;
    }

    static void row(JPanel panel, JComponent child) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = panel.getComponentCount();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 8, 0);
        panel.add(child, c);
    }

    static JPanel block(String title, JComponent content) {
        JPanel block = AppStyle.surfacePanel(new BorderLayout(0, 5), "high");
        block.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(AppStyle.LINE),
                        new EmptyBorder(8, 8, 8, 8)));
        block.add(AppStyle.mono(title, 10), BorderLayout.NORTH);
        block.add(content, BorderLayout.CENTER);
        return block;
    }

    static JScrollPane scroll(JComponent view) {
        JScrollPane scroll = new JScrollPane(view);
        AppStyle.scrollPane(scroll);
        scroll.getViewport().putClientProperty("surfaceRole", "background");
        return scroll;
    }

    static final class WidthPanel extends JPanel implements Scrollable {
        WidthPanel(LayoutManager layout) {
            super(layout);
            putClientProperty("surfaceRole", "background");
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(420, 600);
        }

        public int getScrollableUnitIncrement(Rectangle r, int orientation, int direction) {
            return 24;
        }

        public int getScrollableBlockIncrement(Rectangle r, int orientation, int direction) {
            return Math.max(24, r.height - 24);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    static final class WrapPanel extends JPanel {
        WrapPanel(int gap) {
            super(new FlowLayout(FlowLayout.LEFT, gap, 4));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            FlowLayout layout = (FlowLayout) getLayout();
            int gap = layout.getHgap();
            int width =
                    getWidth() > 0
                            ? getWidth()
                            : getParent() != null && getParent().getWidth() > 0
                                    ? getParent().getWidth() - 24
                                    : 900;
            int used = gap, height = 4, row = 0;
            for (Component component : getComponents()) {
                if (!component.isVisible()) continue;
                Dimension size = component.getPreferredSize();
                if (used > gap && used + size.width + gap > width) {
                    height += row + 4;
                    used = gap;
                    row = 0;
                }
                used += size.width + gap;
                row = Math.max(row, size.height);
            }
            return new Dimension(width, height + row + 4);
        }
    }
}
