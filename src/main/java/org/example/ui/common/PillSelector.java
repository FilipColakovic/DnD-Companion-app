package org.example.ui.common;

import java.awt.BasicStroke;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/** Shared rounded selector with readable, keyboard-accessible options. */
public class PillSelector<T> extends JButton {
    private T selected;
    private final List<T> options;
    private final Consumer<T> onSelect;

    public PillSelector(List<T> options, T initial, Consumer<T> onSelect) {
        this.options = List.copyOf(options);
        this.onSelect = Objects.requireNonNull(onSelect);
        AppStyle.roundButton(this);
        setPreferredSize(new Dimension(260, 42));
        setSelectedOption(initial);
        addActionListener(
                event -> {
                    JPopupMenu menu = createMenu();
                    AppStyle.applyAppearance(menu);
                    menu.show(this, 0, getHeight() + 6);
                });
    }

    public void setSelectedOption(T option) {
        selected = Objects.requireNonNull(option);
        setText(selected.toString());
        setIcon(
                new Icon() {
                    public int getIconWidth() {
                        return 8;
                    }

                    public int getIconHeight() {
                        return 5;
                    }

                    public void paintIcon(Component c, Graphics g, int x, int y) {
                        g.setColor(c.getForeground());
                        g.fillPolygon(new int[] {x, x + 8, x + 4}, new int[] {y, y, y + 5}, 3);
                    }
                });
        setHorizontalTextPosition(SwingConstants.LEFT);
    }

    public JPopupMenu createMenu() {
        JPopupMenu menu =
                new JPopupMenu() {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = AppStyle.graphics(graphics);
                        g.setColor(AppStyle.SURFACE);
                        g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
                        g.setColor(AppStyle.LINE);
                        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                        g.dispose();
                    }
                };
        menu.setOpaque(false);
        menu.setBorder(new EmptyBorder(6, 6, 6, 6));
        ButtonGroup group = new ButtonGroup();
        int width = Math.max(getWidth(), getPreferredSize().width) - 12;
        for (T option : options) {
            JRadioButtonMenuItem item =
                    new JRadioButtonMenuItem(option.toString(), option.equals(selected)) {
                        @Override
                        protected void paintComponent(Graphics graphics) {
                            Graphics2D g = AppStyle.graphics(graphics);
                            if (getModel().isArmed() || getModel().isSelected()) {
                                g.setColor(AppStyle.SOFT);
                                g.fillRect(0, 0, getWidth(), getHeight());
                            }
                            g.setFont(getFont());
                            g.setColor(AppStyle.INK);
                            int baseline =
                                    (getHeight() - g.getFontMetrics().getHeight()) / 2
                                            + g.getFontMetrics().getAscent();
                            g.drawString(getText(), 38, baseline);
                            if (isSelected()) {
                                g.setStroke(
                                        new BasicStroke(
                                                1.8f,
                                                BasicStroke.CAP_ROUND,
                                                BasicStroke.JOIN_ROUND));
                                int middle = getHeight() / 2;
                                g.drawLine(13, middle, 17, middle + 4);
                                g.drawLine(17, middle + 4, 24, middle - 4);
                            }
                            g.dispose();
                        }
                    };
            item.setOpaque(false);
            item.setFocusPainted(false);
            item.setFont(new Font("SansSerif", Font.PLAIN, 16));
            item.setPreferredSize(new Dimension(width, 44));
            item.setBorder(new EmptyBorder(8, 12, 8, 12));
            item.addActionListener(
                    event -> {
                        setSelectedOption(option);
                        onSelect.accept(option);
                    });
            group.add(item);
            menu.add(item);
        }
        return menu;
    }
}
