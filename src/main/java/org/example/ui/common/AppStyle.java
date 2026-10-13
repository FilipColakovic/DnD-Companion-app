package org.example.ui.common;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.AbstractBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.BasicGraphicsUtils;
import javax.swing.plaf.basic.ComboPopup;
import javax.swing.text.JTextComponent;

/** Shared Bauhaus palette, mechanical surfaces, and keyboard-accessible Swing controls. */
public final class AppStyle {
    public static Color INK = new Color(24, 24, 24);
    public static Color MUTED = new Color(100, 100, 100);
    public static Color LINE = new Color(0x1a1a1a);
    public static Color SOFT = new Color(0xe8e3da);
    public static Color SURFACE = Color.WHITE;
    public static Color BACKGROUND = new Color(0xf4ede2);
    public static Color LOW = new Color(0xf2ede5);
    public static Color LOWEST = new Color(0xfaf7f2);
    public static Color HIGH = new Color(0xe8e3da);
    public static Color ACCENT = new Color(0x0055ff);
    public static Color GOLD = new Color(168, 96, 0);
    public static Color ERROR = new Color(173, 53, 60);
    private static boolean dark;
    private static int textSize = 14;
    private static boolean windowStylingInstalled;

    private AppStyle() {}

    public static JPanel card(LayoutManager layout) {
        return roundedPanel(layout, false);
    }

    public static JPanel surfacePanel(LayoutManager layout) {
        return roundedPanel(layout, true);
    }

    public static JPanel surfacePanel(LayoutManager layout, String role) {
        JPanel panel = new JPanel(layout);
        panel.putClientProperty("surfaceRole", role);
        panel.setBackground(surfaceColor(role));
        return panel;
    }

    private static Color surfaceColor(String role) {
        if ("noteBlue".equals(role)) return dark ? new Color(0x293c60) : new Color(0xd3dcff);
        if ("metamagicCard".equals(role)) return dark ? LOWEST : new Color(0xf8f5fc);
        if ("metamagicActive".equals(role)) return dark ? new Color(0x494024) : new Color(0xffefb3);
        if ("badgeRed".equals(role)) return new Color(0xe7362a);
        if ("badgeBlue".equals(role)) return new Color(0x0055ff);
        if ("badgeDark".equals(role)) return new Color(0x1a1a1a);
        if ("slotAvailable".equals(role)) return new Color(0xffcc00);
        return "low".equals(role)
                ? LOW
                : "lowest".equals(role)
                        ? LOWEST
                        : "high".equals(role)
                                ? HIGH
                                : "surface".equals(role) ? SURFACE : BACKGROUND;
    }

    private static JPanel roundedPanel(LayoutManager layout, boolean raised) {
        JPanel panel =
                new JPanel(layout) {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = graphics(graphics);
                        int shadow =
                                getClientProperty("shadowOffset") instanceof Integer offset
                                        ? offset
                                        : 4;
                        g.setColor(Color.BLACK);
                        g.fillRect(shadow, shadow, getWidth() - shadow, getHeight() - shadow);
                        g.setColor(surfaceColor((String) getClientProperty("surfaceRole")));
                        g.fillRect(0, 0, getWidth() - shadow - 1, getHeight() - shadow - 1);
                        g.setColor(LINE);
                        g.setStroke(new BasicStroke(2));
                        g.drawRect(1, 1, getWidth() - shadow - 3, getHeight() - shadow - 3);
                        g.dispose();
                    }
                };
        panel.setOpaque(false);
        panel.putClientProperty("surfaceRole", raised ? "high" : "surface");
        panel.setBorder(new EmptyBorder(20, 20, 24, 24));
        return panel;
    }

    public static JPanel section(LayoutManager layout) {
        JPanel panel = new JPanel(layout);
        panel.setOpaque(false);
        return panel;
    }

    public static void roundButton(AbstractButton button) {
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setForeground(INK);
        button.setBackground(SURFACE);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setRolloverEnabled(true);
        button.setBorder(new PillBorder());
        button.setMargin(new Insets(9, 14, 9, 14));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
        if (button instanceof JButton
                || button instanceof JToggleButton
                        && !(button instanceof JCheckBox || button instanceof JRadioButton)) {
            button.setUI(new ArcaneButtonUI());
        }
    }

    public static JButton textButton(String title, Runnable action) {
        JButton button = new JButton(title);
        button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        button.putClientProperty("tactileCustomPaint", true);
        button.setFont(new Font("Segoe UI", Font.BOLD, 12));
        button.setBorder(new EmptyBorder(0, 0, 0, 0));
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.addActionListener(event -> action.run());
        return button;
    }

    public static JButton compactButton(String title, Runnable action) {
        JButton button = button(title, action);
        button.setFont(new Font("Segoe UI", Font.BOLD, 11));
        button.setBorder(new EmptyBorder(5, 7, 8, 10));
        return button;
    }

    public static JButton button(String title, Runnable action) {
        JButton button = new JButton(title);
        roundButton(button);
        button.addActionListener(event -> action.run());
        return button;
    }

    public static JButton primaryButton(String title, Runnable action) {
        JButton button = button(title, action);
        button.putClientProperty("buttonRole", "primary");
        setColorRole(button, "primaryText");
        return button;
    }

    public static JButton navigationButton(String title, Icon icon, Runnable action) {
        JButton button = button(title, action);
        button.setIcon(icon);
        button.setHorizontalAlignment(SwingConstants.LEFT);
        button.putClientProperty("buttonRole", "navigation");
        button.setBorder(new PillBorder(new Insets(12, 16, 12, 16)));
        setColorRole(button, "muted");
        return button;
    }

    public static JToggleButton chip(String title, Runnable action) {
        JToggleButton button = new JToggleButton(title);
        roundButton(button);
        button.putClientProperty("buttonRole", "chip");
        button.addActionListener(event -> action.run());
        return button;
    }

    public static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(INK);
        return label;
    }

    public static JLabel heading(String title, int size) {
        JLabel label = label(title, size, true);
        label.setFont(new Font("Segoe UI Semibold", Font.BOLD, size));
        return label;
    }

    public static JLabel kicker(String title) {
        JLabel label = mono(title.toUpperCase(java.util.Locale.ROOT), 11);
        setColorRole(label, "muted");
        return label;
    }

    public static JLabel mono(String title, int size) {
        JLabel label = label(title, size, false);
        label.setFont(new Font("Consolas", Font.PLAIN, size));
        return label;
    }

    public static JLabel badge(String title) {
        JLabel label =
                new JLabel(title) {
                    @Override
                    protected void paintComponent(Graphics graphics) {
                        Graphics2D g = graphics(graphics);
                        g.setColor(new Color(0xffcc00));
                        g.fillRect(0, 0, getWidth(), getHeight());
                        g.setColor(new Color(0x1a1a1a));
                        g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                        g.dispose();
                        super.paintComponent(graphics);
                    }
                };
        label.setFont(new Font("Consolas", Font.PLAIN, 11));
        label.setBorder(new EmptyBorder(5, 8, 5, 8));
        label.setOpaque(false);
        setColorRole(label, "badgeText");
        return label;
    }

    public static void setColorRole(JComponent component, String role) {
        component.putClientProperty("colorRole", role);
        component.setForeground(roleColor(role));
    }

    private static Color roleColor(String role) {
        if ("muted".equals(role)) {
            return MUTED;
        }
        if ("accent".equals(role)) {
            return ACCENT;
        }
        if ("gold".equals(role)) {
            return GOLD;
        }
        if ("error".equals(role)) {
            return ERROR;
        }
        if ("badgeLightText".equals(role)) {
            return Color.WHITE;
        }
        if ("badgeYellowText".equals(role)) {
            return new Color(0xffcc00);
        }
        if ("primaryText".equals(role) || "badgeText".equals(role)) {
            return new Color(0x1a1a1a);
        }
        return INK;
    }

    public static Graphics2D graphics(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        return g;
    }

    public static JTextArea text(String value) {
        JTextArea text = new JTextArea(value);
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        text.setForeground(INK);
        text.putClientProperty("colorRole", "ink");
        text.setOpaque(false);
        return text;
    }

    public static void scrollPane(JScrollPane scroll) {
        scroll.setBorder(null);
        scroll.getViewport().setBackground(SURFACE);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        JScrollBar bar = scroll.getVerticalScrollBar();
        bar.setPreferredSize(new Dimension(10, 0));
        bar.setUnitIncrement(24);
        bar.setOpaque(false);
        bar.setUI(
                new javax.swing.plaf.basic.BasicScrollBarUI() {
                    @Override
                    protected JButton createDecreaseButton(int orientation) {
                        return emptyButton();
                    }

                    @Override
                    protected JButton createIncreaseButton(int orientation) {
                        return emptyButton();
                    }

                    private JButton emptyButton() {
                        JButton button = new JButton();
                        button.setPreferredSize(new Dimension(0, 0));
                        return button;
                    }

                    @Override
                    protected void paintTrack(
                            Graphics graphics, JComponent component, Rectangle bounds) {}

                    @Override
                    protected void paintThumb(
                            Graphics graphics, JComponent component, Rectangle bounds) {
                        Graphics2D g = graphics(graphics);
                        g.setColor(MUTED);
                        g.fillRect(
                                bounds.x + 2,
                                bounds.y + 2,
                                Math.max(4, bounds.width - 4),
                                Math.max(4, bounds.height - 4));
                        g.dispose();
                    }
                });
    }

    public static JDialog openWindow(Component parent, String title, JComponent content) {
        JDialog dialog =
                new JDialog(
                        SwingUtilities.getWindowAncestor(parent),
                        title,
                        Dialog.ModalityType.MODELESS);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setContentPane(content);
        dialog.setSize(960, 740);
        dialog.setMinimumSize(new Dimension(740, 540));
        dialog.addWindowListener(
                new java.awt.event.WindowAdapter() {
                    @Override
                    public void windowClosed(java.awt.event.WindowEvent event) {
                        if (content instanceof LiveView view) {
                            view.dispose();
                        }
                    }
                });
        applyAppearance(dialog);
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog;
    }

    public static boolean isDark() {
        return dark;
    }

    public static Container windowContent(JDialog window) {
        Object content = window.getRootPane().getClientProperty("workspaceContent");
        return content instanceof Container original ? original : window.getContentPane();
    }

    public static int textSize() {
        return textSize;
    }

    public static void appearance(boolean useDark, int size) {
        if (dark == useDark && textSize == size && windowStylingInstalled) {
            return;
        }
        dark = useDark;
        textSize = size;
        INK = dark ? new Color(0xf5f0e8) : new Color(0x1a1a1a);
        MUTED = dark ? new Color(0xc9c4ba) : new Color(0x4a4a4a);
        LINE = dark ? new Color(0xaaa397) : new Color(0x1a1a1a);
        SOFT = dark ? new Color(0x393733) : new Color(0xe8e3da);
        BACKGROUND = dark ? new Color(0x20201e) : new Color(0xf4ede2);
        LOW = dark ? new Color(0x292825) : new Color(0xf2ede5);
        LOWEST = dark ? new Color(0x242320) : new Color(0xfaf7f2);
        SURFACE = dark ? new Color(0x302e2a) : Color.WHITE;
        HIGH = dark ? new Color(0x45413a) : new Color(0xe8e3da);
        ACCENT = dark ? new Color(0x9dbfff) : new Color(0x0055ff);
        GOLD = dark ? new Color(0xffcc00) : new Color(0x995c00);
        ERROR = dark ? new Color(0xff9589) : new Color(0xcc0000);
        defaults();
        if (!windowStylingInstalled) {
            windowStylingInstalled = true;
            Toolkit.getDefaultToolkit()
                    .addAWTEventListener(
                            event -> {
                                if (event instanceof java.awt.event.WindowEvent windowEvent
                                        && windowEvent.getID()
                                                == java.awt.event.WindowEvent.WINDOW_OPENED) {
                                    applyAppearance(windowEvent.getWindow());
                                }
                            },
                            AWTEvent.WINDOW_EVENT_MASK);
        }
        for (Window window : Window.getWindows()) {
            applyAppearance(window);
        }
    }

    private static void defaults() {
        for (String name :
                new String[] {
                    "Panel",
                    "OptionPane",
                    "Label",
                    "Button",
                    "ToggleButton",
                    "CheckBox",
                    "RadioButton",
                    "MenuItem",
                    "RadioButtonMenuItem",
                    "PopupMenu",
                    "ToolTip"
                }) {
            UIManager.put(name + ".background", SURFACE);
            UIManager.put(name + ".foreground", INK);
        }
        for (String name :
                new String[] {
                    "ComboBox", "List", "Table", "TextField", "TextArea", "EditorPane", "TextPane"
                }) {
            UIManager.put(name + ".background", SURFACE);
            UIManager.put(name + ".foreground", INK);
            UIManager.put(name + ".selectionBackground", HIGH);
            UIManager.put(name + ".selectionForeground", INK);
            UIManager.put(name + ".caretForeground", INK);
        }
        UIManager.put("ComboBox.disabledForeground", MUTED);
        UIManager.put("ComboBox.disabledBackground", SURFACE);
        UIManager.put("CheckBox.icon", new ChoiceIcon(false));
        UIManager.put("RadioButton.icon", new ChoiceIcon(true));
        UIManager.put("MenuItem.selectionBackground", HIGH);
        UIManager.put("MenuItem.selectionForeground", ACCENT);
        UIManager.put("RadioButtonMenuItem.selectionBackground", HIGH);
        UIManager.put("RadioButtonMenuItem.selectionForeground", ACCENT);
        UIManager.put(
                "ToolTip.border",
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(LINE), new EmptyBorder(5, 8, 5, 8)));
    }

    public static void applyAppearance(Component component) {
        if (component instanceof JDialog dialog) {
            WindowShell.install(dialog);
            WindowChrome.install(dialog);
        }
        if (component instanceof JComponent c) {
            c.setBorder(themedBorder(c.getBorder()));
            if (c.getClientProperty("sidebarWidthBase") instanceof Integer width) {
                c.setPreferredSize(new Dimension(width + Math.max(0, textSize - 14) * 10, 0));
            }
            if (c instanceof JButton button
                    && !(button.getUI() instanceof ArcaneButtonUI)
                    && !Boolean.TRUE.equals(button.getClientProperty("tactileCustomPaint"))) {
                roundButton(button);
            }
            Font base = (Font) c.getClientProperty("baseFont");
            if (base == null && c.getFont() != null) {
                base = c.getFont();
                if ("SansSerif".equals(base.getFamily()) || "Dialog".equals(base.getFamily())) {
                    base = new Font("Segoe UI", base.getStyle(), base.getSize());
                }
                c.putClientProperty("baseFont", base);
            }
            if (base != null) {
                c.setFont(base.deriveFont(Math.max(10f, base.getSize2D() + textSize - 14)));
            }
            String role = (String) c.getClientProperty("colorRole");
            if (role == null) {
                Color original = c.getForeground();
                role =
                        original != null
                                        && (original.equals(MUTED)
                                                || original.equals(new Color(100, 100, 100))
                                                || original.equals(new Color(180, 183, 192))
                                                || original.equals(new Color(0xcfc2d6))
                                                || original.equals(new Color(0x615868)))
                                ? "muted"
                                : "ink";
                if (original != null
                        && (original.equals(ACCENT)
                                || original.equals(new Color(0xddb7ff))
                                || original.equals(new Color(0x7f39bb)))) {
                    role = "accent";
                }
                if (original != null
                        && (original.equals(GOLD)
                                || original.equals(new Color(0xffb95f))
                                || original.equals(new Color(0xa86000)))) {
                    role = "gold";
                }
                if (original != null
                        && (original.equals(ERROR)
                                || original.equals(Color.RED)
                                || original.equals(new Color(0xffb4ab))
                                || original.equals(new Color(0xad353c)))) {
                    role = "error";
                }
                c.putClientProperty("colorRole", role);
            }
            c.setForeground(roleColor(role));
            String surfaceRole = (String) c.getClientProperty("surfaceRole");
            c.setBackground(surfaceColor(surfaceRole));
            if (c instanceof JTextComponent
                    || c instanceof JTable
                    || c instanceof JList<?>
                    || c instanceof JComboBox<?>
                    || c instanceof JViewport && surfaceRole == null
                    || c instanceof JPopupMenu) {
                c.setBackground(SURFACE);
            }
            if (c instanceof JTextComponent text) {
                text.setCaretColor(INK);
                text.setSelectionColor(HIGH);
                text.setSelectedTextColor(INK);
            }
            if (c instanceof JTextField field && !(field.getBorder() instanceof PillBorder)) {
                field.setBorder(new PillBorder(new Insets(9, 12, 9, 12)));
            }
            if (c instanceof JEditorPane editor) {
                editor.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true);
            }
            if (c instanceof JTable table) {
                applyAppearance(table.getTableHeader());
                table.setSelectionBackground(HIGH);
                table.setSelectionForeground(INK);
                table.setGridColor(LINE);
                table.getTableHeader().setForeground(MUTED);
                table.getTableHeader().setBackground(SURFACE);
                for (int i = 0; i < table.getColumnCount(); i++) {
                    if (table.getColumnModel().getColumn(i).getCellRenderer()
                            instanceof Component renderer) {
                        applyAppearance(renderer);
                    }
                }
                if (table.getDefaultRenderer(Boolean.class) instanceof Component renderer) {
                    applyAppearance(renderer);
                }
                if (table.getDefaultEditor(Boolean.class) instanceof DefaultCellEditor editor) {
                    applyAppearance(editor.getComponent());
                }
            }
            if (c instanceof JComboBox<?> combo) {
                styleCombo(combo);
            }
            if (c instanceof JList<?> list) {
                styleList(list);
            }
            if (c instanceof JCheckBox box && box.getIcon() == null) {
                box.setIcon(new ChoiceIcon(false));
            }
            if (c instanceof JRadioButton box && box.getIcon() == null) {
                box.setIcon(new ChoiceIcon(true));
            }
            if (c instanceof AbstractButton button) {
                button.setFocusPainted(false);
            }
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                applyAppearance(child);
            }
        }
        if (component instanceof JComponent c) {
            c.revalidate();
            c.repaint();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void styleList(JList<?> list) {
        list.setSelectionBackground(HIGH);
        list.setSelectionForeground(INK);
        if (!(list.getCellRenderer() instanceof ThemedListRenderer)) {
            ((JList) list).setCellRenderer(new ThemedListRenderer(list.getCellRenderer()));
        }
    }

    private static javax.swing.border.Border themedBorder(javax.swing.border.Border border) {
        if (border instanceof javax.swing.border.LineBorder line)
            return BorderFactory.createLineBorder(LINE, line.getThickness());
        if (border instanceof javax.swing.border.MatteBorder matte
                && matte.getMatteColor() != null) {
            Insets i = matte.getBorderInsets();
            return BorderFactory.createMatteBorder(i.top, i.left, i.bottom, i.right, LINE);
        }
        if (border instanceof javax.swing.border.CompoundBorder compound)
            return BorderFactory.createCompoundBorder(
                    themedBorder(compound.getOutsideBorder()),
                    themedBorder(compound.getInsideBorder()));
        return border;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void styleCombo(JComboBox<?> combo) {
        if (!(combo.getRenderer() instanceof ThemedListRenderer)) {
            ((JComboBox) combo).setRenderer(new ThemedListRenderer(combo.getRenderer()));
        }
        if (!Boolean.TRUE.equals(combo.getClientProperty("arcaneComboUI"))) {
            combo.putClientProperty("arcaneComboUI", true);
            combo.setUI(
                    new BasicComboBoxUI() {
                        @Override
                        protected JButton createArrowButton() {
                            JButton arrow = new JButton();
                            roundButton(arrow);
                            arrow.setIcon(
                                    new Icon() {
                                        public int getIconWidth() {
                                            return 10;
                                        }

                                        public int getIconHeight() {
                                            return 6;
                                        }

                                        public void paintIcon(
                                                Component component,
                                                Graphics graphics,
                                                int x,
                                                int y) {
                                            Graphics2D g = graphics(graphics);
                                            g.setColor(MUTED);
                                            g.setStroke(
                                                    new BasicStroke(
                                                            1.5f,
                                                            BasicStroke.CAP_ROUND,
                                                            BasicStroke.JOIN_ROUND));
                                            g.drawLine(x + 1, y + 1, x + 5, y + 5);
                                            g.drawLine(x + 5, y + 5, x + 9, y + 1);
                                            g.dispose();
                                        }
                                    });
                            arrow.setBorder(new EmptyBorder(0, 7, 0, 7));
                            arrow.putClientProperty("buttonRole", "arrow");
                            arrow.setPreferredSize(new Dimension(28, 20));
                            return arrow;
                        }

                        @Override
                        protected ComboPopup createPopup() {
                            BasicComboPopup popup = new BasicComboPopup(comboBox);
                            popup.setBorder(
                                    BorderFactory.createCompoundBorder(
                                            BorderFactory.createLineBorder(LINE),
                                            new EmptyBorder(4, 4, 4, 4)));
                            popup.getList().setSelectionBackground(HIGH);
                            popup.getList().setSelectionForeground(INK);
                            popup.getList().setBackground(SURFACE);
                            return popup;
                        }

                        @Override
                        public void paintCurrentValueBackground(
                                Graphics g, Rectangle bounds, boolean hasFocus) {
                            g.setColor(SURFACE);
                            g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
                        }
                    });
            if (!(combo.getBorder() instanceof PillBorder)) {
                combo.setBorder(new PillBorder(new Insets(8, 10, 8, 4)));
            }
        }
        if (combo.getUI().getAccessibleChild(combo, 0) instanceof BasicComboPopup popup) {
            popup.setBackground(SURFACE);
            popup.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(LINE), new EmptyBorder(4, 4, 4, 4)));
            popup.getList().setFont(combo.getFont());
            popup.getList().setBackground(SURFACE);
            popup.getList().setForeground(INK);
            popup.getList().setSelectionBackground(HIGH);
            popup.getList().setSelectionForeground(INK);
        }
    }

    private static final class ThemedListRenderer implements ListCellRenderer<Object> {
        private final ListCellRenderer<Object> delegate;

        @SuppressWarnings("unchecked")
        ThemedListRenderer(ListCellRenderer<?> delegate) {
            this.delegate =
                    delegate == null
                            ? new DefaultListCellRenderer()
                            : (ListCellRenderer<Object>) delegate;
        }

        @Override
        public Component getListCellRendererComponent(
                JList<?> list, Object value, int index, boolean selected, boolean focused) {
            Component component =
                    delegate.getListCellRendererComponent(list, value, index, selected, false);
            component.setForeground(selected ? ACCENT : INK);
            component.setBackground(selected ? HIGH : SURFACE);
            if (component instanceof JLabel label) {
                label.setOpaque(true);
                label.setBorder(
                        index == -1 ? new EmptyBorder(2, 2, 2, 2) : new EmptyBorder(8, 8, 8, 8));
                label.setFont(list.getFont());
            }
            return component;
        }
    }

    private static final class ChoiceIcon implements Icon {
        private final boolean radio;

        ChoiceIcon(boolean radio) {
            this.radio = radio;
        }

        @Override
        public int getIconWidth() {
            return 16;
        }

        @Override
        public int getIconHeight() {
            return 16;
        }

        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = graphics(graphics);
            boolean selected = component instanceof AbstractButton button && button.isSelected();
            g.setColor(selected ? new Color(0xffcc00) : SURFACE);
            if (radio) {
                g.fillOval(x + 1, y + 1, 14, 14);
            } else {
                g.fillRoundRect(x + 1, y + 1, 14, 14, 5, 5);
            }
            g.setColor(LINE);
            if (radio) {
                g.drawOval(x + 1, y + 1, 14, 14);
            } else {
                g.drawRoundRect(x + 1, y + 1, 14, 14, 5, 5);
            }
            if (selected) {
                g.setColor(new Color(0x1a1a1a));
                if (radio) {
                    g.fillOval(x + 5, y + 5, 6, 6);
                } else {
                    g.setStroke(
                            new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    g.drawLine(x + 4, y + 8, x + 7, y + 11);
                    g.drawLine(x + 7, y + 11, x + 12, y + 5);
                }
            }
            g.dispose();
        }
    }

    private static final class ArcaneButtonUI extends BasicButtonUI {
        @Override
        public void paint(Graphics graphics, JComponent component) {
            AbstractButton button = (AbstractButton) component;
            String role = (String) button.getClientProperty("buttonRole");
            boolean hovered = button.getModel().isRollover();
            boolean pressed = button.getModel().isPressed();
            boolean selected = button.isSelected();
            Graphics2D g = graphics(graphics);
            String pigment = (String) button.getClientProperty("tactileRole");
            boolean yellow =
                    "primary".equals(role) && pigment == null
                            || "yellow".equals(pigment)
                            || selected;
            Color fill =
                    yellow
                            ? new Color(0xffcc00)
                            : "red".equals(pigment)
                                    ? new Color(0xe63b2e)
                                    : "blue".equals(pigment)
                                            ? new Color(0x0055ff)
                                            : hovered || pressed ? HIGH : SOFT;
            boolean navigation = "navigation".equals(role);
            if (navigation && !selected && !hovered && !pressed
                    || "arrow".equals(role) && !hovered && !pressed) {
                fill = null;
            }
            if (fill != null) {
                int shift = pressed && button.getModel().isArmed() ? 2 : 0;
                g.setColor(Color.BLACK);
                g.fillRect(3, 3, component.getWidth() - 3, component.getHeight() - 3);
                g.setColor(button.isEnabled() ? fill : blend(fill, SURFACE));
                g.fillRect(shift, shift, component.getWidth() - 4, component.getHeight() - 4);
                g.setColor(LINE);
                g.drawRect(shift, shift, component.getWidth() - 5, component.getHeight() - 5);
                if (!navigation) {
                    g.setColor(pressed ? new Color(0, 0, 0, 90) : new Color(255, 255, 255, 150));
                    g.drawLine(shift + 2, shift + 2, component.getWidth() - 7, shift + 2);
                    g.drawLine(shift + 2, shift + 2, shift + 2, component.getHeight() - 7);
                    g.setColor(pressed ? new Color(255, 255, 255, 140) : new Color(0, 0, 0, 90));
                    g.drawLine(
                            shift + 2,
                            component.getHeight() - 7,
                            component.getWidth() - 7,
                            component.getHeight() - 7);
                    g.drawLine(
                            component.getWidth() - 7,
                            shift + 2,
                            component.getWidth() - 7,
                            component.getHeight() - 7);
                }
            }
            button.setForeground(
                    yellow
                            ? new Color(0x1a1a1a)
                            : "red".equals(pigment) || "blue".equals(pigment)
                                    ? Color.WHITE
                                    : navigation && !hovered
                                            ? MUTED
                                            : roleColor(
                                                    (String)
                                                            button.getClientProperty("colorRole")));
            g.dispose();
            super.paint(graphics, component);
        }

        @Override
        protected void paintText(
                Graphics graphics, AbstractButton button, Rectangle rectangle, String text) {
            FontMetrics metrics = graphics.getFontMetrics();
            graphics.setColor(button.isEnabled() ? button.getForeground() : MUTED);
            BasicGraphicsUtils.drawStringUnderlineCharAt(
                    graphics,
                    text,
                    button.getDisplayedMnemonicIndex(),
                    rectangle.x + getTextShiftOffset(),
                    rectangle.y + metrics.getAscent() + getTextShiftOffset());
        }
    }

    private static Color blend(Color first, Color second) {
        return new Color(
                (first.getRed() + second.getRed()) / 2,
                (first.getGreen() + second.getGreen()) / 2,
                (first.getBlue() + second.getBlue()) / 2);
    }

    public static final class PillBorder extends AbstractBorder {
        private final Insets padding;

        public PillBorder() {
            this(new Insets(9, 14, 9, 14));
        }

        public PillBorder(Insets padding) {
            this.padding = (Insets) padding.clone();
        }

        @Override
        public Insets getBorderInsets(Component component) {
            return (Insets) padding.clone();
        }

        @Override
        public Insets getBorderInsets(Component component, Insets insets) {
            insets.set(padding.top, padding.left, padding.bottom, padding.right);
            return insets;
        }

        @Override
        public void paintBorder(
                Component component, Graphics graphics, int x, int y, int width, int height) {
            Graphics2D g = graphics(graphics);
            String role =
                    component instanceof JComponent c
                            ? (String) c.getClientProperty("buttonRole")
                            : null;
            if (!(component instanceof AbstractButton)
                    && !"navigation".equals(role)
                    && !"arrow".equals(role)) {
                g.setColor(
                        "primary".equals(role)
                                ? ACCENT
                                : component instanceof AbstractButton button && button.isSelected()
                                        ? ACCENT
                                        : LINE);
                g.drawRect(x, y, width - 1, height - 1);
            }
            g.dispose();
        }
    }
}
