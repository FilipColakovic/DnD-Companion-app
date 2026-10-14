package dndcompanion.ui.common;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** Custom caption and resize edges without changing a window's content pane or close handler. */
public final class WindowChrome {
    private WindowChrome() {}

    public static void install(Window window) {
        if (!(window instanceof RootPaneContainer container)) return;
        AppIcon.install(window);
        JRootPane root = container.getRootPane();
        if (Boolean.TRUE.equals(root.getClientProperty("customWindowChrome"))
                || window.isDisplayable()) return;
        if (window instanceof JFrame frame) frame.setUndecorated(true);
        else if (window instanceof JDialog dialog && !dialog.isModal()) dialog.setUndecorated(true);
        else return;
        root.putClientProperty("customWindowChrome", true);
        root.setBorder(BorderFactory.createLineBorder(AppStyle.LINE, 2));
        JPanel caption = AppStyle.surfacePanel(new BorderLayout(), "low");
        caption.setName("window-caption");
        JLabel appIcon = new JLabel(AppIcon.caption());
        appIcon.setToolTipText("DnD Companion");
        caption.add(appIcon, BorderLayout.WEST);
        caption.getAccessibleContext()
                .setAccessibleName(
                        window instanceof Frame frame
                                ? frame.getTitle()
                                : ((Dialog) window).getTitle());
        caption.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, AppStyle.LINE),
                        new EmptyBorder(7, 10, 9, 10)));
        JPanel controls = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 7, 0));
        if (window instanceof JFrame frame)
            controls.add(
                    control(
                            "Minimize window",
                            0,
                            () ->
                                    frame.setExtendedState(
                                            frame.getExtendedState() | Frame.ICONIFIED)));
        Maximizer maximizer = new Maximizer(window);
        JButton maximize = control("Maximize window", 1, maximizer::toggle);
        maximizer.button = maximize;
        controls.add(maximize);
        JButton close =
                control(
                        "Close window",
                        2,
                        () ->
                                window.dispatchEvent(
                                        new WindowEvent(window, WindowEvent.WINDOW_CLOSING)));
        close.putClientProperty("tactileRole", "red");
        AppStyle.setColorRole(close, "badgeLightText");
        controls.add(close);
        caption.add(controls, BorderLayout.EAST);
        root.getLayeredPane().add(caption, JLayeredPane.PALETTE_LAYER);
        LayoutManager original = root.getLayout();
        root.setLayout(
                new LayoutManager() {
                    public void addLayoutComponent(String name, Component component) {
                        original.addLayoutComponent(name, component);
                    }

                    public void removeLayoutComponent(Component component) {
                        original.removeLayoutComponent(component);
                    }

                    public Dimension preferredLayoutSize(Container parent) {
                        Dimension size = original.preferredLayoutSize(parent);
                        return new Dimension(
                                size.width, size.height + caption.getPreferredSize().height);
                    }

                    public Dimension minimumLayoutSize(Container parent) {
                        Dimension size = original.minimumLayoutSize(parent);
                        return new Dimension(
                                size.width, size.height + caption.getPreferredSize().height);
                    }

                    public void layoutContainer(Container parent) {
                        original.layoutContainer(parent);
                        int height = caption.getPreferredSize().height;
                        caption.setBounds(0, 0, root.getLayeredPane().getWidth(), height);
                        Container content = root.getContentPane();
                        Rectangle bounds = content.getBounds();
                        content.setBounds(
                                bounds.x,
                                bounds.y + height,
                                bounds.width,
                                Math.max(0, bounds.height - height));
                        if (root.getJMenuBar() != null) {
                            JMenuBar menu = root.getJMenuBar();
                            Rectangle menuBounds = menu.getBounds();
                            menu.setBounds(
                                    menuBounds.x,
                                    menuBounds.y + height,
                                    menuBounds.width,
                                    menuBounds.height);
                        }
                    }
                });
        MouseAdapter drag =
                new MouseAdapter() {
                    private Point start, position;

                    @Override
                    public void mousePressed(MouseEvent event) {
                        if (SwingUtilities.isLeftMouseButton(event)
                                && !maximizer.maximized()
                                && !resizeCursor(window.getCursor().getType())) {
                            start = event.getLocationOnScreen();
                            position = window.getLocation();
                        }
                    }

                    @Override
                    public void mouseDragged(MouseEvent event) {
                        if (start != null) {
                            Point point = event.getLocationOnScreen();
                            window.setLocation(
                                    position.x + point.x - start.x, position.y + point.y - start.y);
                        }
                    }

                    @Override
                    public void mouseReleased(MouseEvent event) {
                        start = null;
                    }

                    @Override
                    public void mouseClicked(MouseEvent event) {
                        if (SwingUtilities.isLeftMouseButton(event) && event.getClickCount() == 2)
                            maximizer.toggle();
                    }
                };
        caption.addMouseListener(drag);
        caption.addMouseMotionListener(drag);
        appIcon.addMouseListener(drag);
        appIcon.addMouseMotionListener(drag);
        ResizeEdges edges = new ResizeEdges(window, maximizer);
        Toolkit.getDefaultToolkit()
                .addAWTEventListener(
                        edges, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK);
        window.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent event) {
                        Toolkit.getDefaultToolkit().removeAWTEventListener(edges);
                    }
                });
        if (window instanceof JFrame frame)
            frame.addWindowStateListener(event -> maximizer.update());
        maximizer.update();
        AppStyle.applyAppearance(root);
    }

    private static JButton control(String label, int symbol, Runnable action) {
        JButton button = AppStyle.button("", action);
        button.setIcon(new CaptionIcon(symbol));
        button.setPreferredSize(new Dimension(31, 31));
        button.setBorder(new EmptyBorder(3, 3, 6, 6));
        button.setToolTipText(label);
        button.getAccessibleContext().setAccessibleName(label);
        return button;
    }

    private record CaptionIcon(int symbol) implements Icon {
        public int getIconWidth() {
            return 13;
        }

        public int getIconHeight() {
            return 13;
        }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setColor(component.getForeground());
            g.setStroke(new BasicStroke(symbol == 2 ? 2.2f : 1.5f));
            if (symbol == 0) g.drawLine(x + 3, y + 9, x + 10, y + 9);
            else if (symbol == 1) g.drawRect(x + 3, y + 3, 7, 7);
            else if (symbol == 3) {
                g.drawRect(x + 5, y + 2, 6, 6);
                g.drawRect(x + 2, y + 5, 6, 6);
            } else {
                g.drawLine(x + 3, y + 3, x + 10, y + 10);
                g.drawLine(x + 10, y + 3, x + 3, y + 10);
            }
            g.dispose();
        }
    }

    private static final class Maximizer {
        final Window window;
        JButton button;
        Rectangle restored;

        Maximizer(Window window) {
            this.window = window;
        }

        boolean maximized() {
            return window instanceof Frame frame
                    ? (frame.getExtendedState() & Frame.MAXIMIZED_BOTH) != 0
                    : restored != null;
        }

        void toggle() {
            if (!resizable(window)) return;
            if (window instanceof JFrame frame) {
                if (maximized()) frame.setExtendedState(Frame.NORMAL);
                else {
                    frame.setMaximizedBounds(usableBounds(window));
                    frame.setExtendedState(Frame.MAXIMIZED_BOTH);
                }
            } else if (restored == null) {
                restored = window.getBounds();
                window.setBounds(usableBounds(window));
            } else {
                window.setBounds(restored);
                restored = null;
            }
            update();
        }

        void update() {
            if (button == null) return;
            String title = maximized() ? "Restore window" : "Maximize window";
            button.setIcon(new CaptionIcon(maximized() ? 3 : 1));
            button.setToolTipText(title);
            button.getAccessibleContext().setAccessibleName(title);
            button.setEnabled(resizable(window));
            if (maximized()) window.setCursor(Cursor.getDefaultCursor());
        }
    }

    private static Rectangle usableBounds(Window window) {
        GraphicsConfiguration config = window.getGraphicsConfiguration();
        Rectangle bounds = new Rectangle(config.getBounds());
        Insets i = Toolkit.getDefaultToolkit().getScreenInsets(config);
        bounds.x += i.left;
        bounds.y += i.top;
        bounds.width -= i.left + i.right;
        bounds.height -= i.top + i.bottom;
        return bounds;
    }

    private static boolean resizable(Window window) {
        return window instanceof Frame frame
                ? frame.isResizable()
                : window instanceof Dialog dialog && dialog.isResizable();
    }

    private static boolean resizeCursor(int type) {
        return type >= Cursor.SW_RESIZE_CURSOR && type <= Cursor.E_RESIZE_CURSOR;
    }

    private static final class ResizeEdges implements AWTEventListener {
        final Window window;
        final Maximizer maximizer;
        Point pressed;
        Rectangle initial;
        int edge;

        ResizeEdges(Window window, Maximizer maximizer) {
            this.window = window;
            this.maximizer = maximizer;
        }

        @Override
        public void eventDispatched(AWTEvent event) {
            if (!(event instanceof MouseEvent mouse)
                    || !(mouse.getSource() instanceof Component component)
                    || SwingUtilities.getWindowAncestor(component) != window) return;
            if (!window.isShowing() || !resizable(window) || maximizer.maximized()) return;
            if (mouse.getID() == MouseEvent.MOUSE_MOVED) {
                Point p = SwingUtilities.convertPoint(component, mouse.getPoint(), window);
                int sides = sides(p);
                window.setCursor(Cursor.getPredefinedCursor(cursor(sides)));
            } else if (mouse.getID() == MouseEvent.MOUSE_PRESSED
                    && SwingUtilities.isLeftMouseButton(mouse)) {
                edge = sides(SwingUtilities.convertPoint(component, mouse.getPoint(), window));
                if (edge != 0) {
                    pressed = mouse.getLocationOnScreen();
                    initial = window.getBounds();
                    mouse.consume();
                }
            } else if (mouse.getID() == MouseEvent.MOUSE_DRAGGED && pressed != null) {
                Point p = mouse.getLocationOnScreen();
                int dx = p.x - pressed.x, dy = p.y - pressed.y;
                Rectangle b = new Rectangle(initial);
                Dimension min = window.getMinimumSize();
                if ((edge & 1) != 0) {
                    b.width = Math.max(min.width, initial.width - dx);
                    b.x = initial.x + initial.width - b.width;
                }
                if ((edge & 2) != 0) b.width = Math.max(min.width, initial.width + dx);
                if ((edge & 4) != 0) {
                    b.height = Math.max(min.height, initial.height - dy);
                    b.y = initial.y + initial.height - b.height;
                }
                if ((edge & 8) != 0) b.height = Math.max(min.height, initial.height + dy);
                window.setBounds(b);
                mouse.consume();
            } else if (mouse.getID() == MouseEvent.MOUSE_RELEASED) {
                pressed = null;
                edge = 0;
            }
        }

        int sides(Point p) {
            return (p.x < 6 ? 1 : p.x >= window.getWidth() - 6 ? 2 : 0)
                    | (p.y < 6 ? 4 : p.y >= window.getHeight() - 6 ? 8 : 0);
        }

        int cursor(int sides) {
            return switch (sides) {
                case 1 -> Cursor.W_RESIZE_CURSOR;
                case 2 -> Cursor.E_RESIZE_CURSOR;
                case 4 -> Cursor.N_RESIZE_CURSOR;
                case 8 -> Cursor.S_RESIZE_CURSOR;
                case 5 -> Cursor.NW_RESIZE_CURSOR;
                case 6 -> Cursor.NE_RESIZE_CURSOR;
                case 9 -> Cursor.SW_RESIZE_CURSOR;
                case 10 -> Cursor.SE_RESIZE_CURSOR;
                default -> Cursor.DEFAULT_CURSOR;
            };
        }
    }
}
