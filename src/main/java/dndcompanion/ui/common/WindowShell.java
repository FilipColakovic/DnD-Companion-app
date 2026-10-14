package dndcompanion.ui.common;

import dndcompanion.ui.GrimoirePanel;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/** Shared header and workspace navigation for secondary, modeless app windows. */
final class WindowShell extends JPanel {
    WindowShell(JDialog window, Container content, GrimoirePanel workspace) {
        super(new BorderLayout());
        JPanel header = AppStyle.surfacePanel(new BorderLayout(12, 0), "low");
        header.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 0, AppStyle.LINE),
                        new EmptyBorder(12, 16, 12, 16)));
        header.add(AppStyle.label(window.getTitle(), 16, true), BorderLayout.CENTER);
        add(header, BorderLayout.NORTH);
        add(content, BorderLayout.CENTER);
        if (workspace != null) {
            JPanel rail = AppStyle.surfacePanel(new BorderLayout(0, 18), "low");
            rail.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 0, 2, AppStyle.LINE),
                            new EmptyBorder(16, 12, 16, 12)));
            JPanel top = AppStyle.section(new BorderLayout(0, 18));
            CharacterSummary summary =
                    new CharacterSummary(workspace::snapshot, workspace.playSession());
            top.add(summary, BorderLayout.NORTH);
            JPanel navigation = AppStyle.section(new GridLayout(0, 1, 0, 8));
            String[] pages = {
                GrimoirePanel.COMBAT,
                GrimoirePanel.METAMAGIC,
                GrimoirePanel.FEATS,
                GrimoirePanel.LIBRARY,
                GrimoirePanel.CHARACTER
            };
            ArcaneIcon.Kind[] icons = {
                ArcaneIcon.Kind.BOLT,
                ArcaneIcon.Kind.SPARK,
                ArcaneIcon.Kind.SHIELD,
                ArcaneIcon.Kind.LIBRARY,
                ArcaneIcon.Kind.PERSON
            };
            for (int i = 0; i < pages.length; i++) {
                String page = pages[i];
                JButton button =
                        AppStyle.navigationButton(
                                page,
                                new ArcaneIcon(icons[i], 16),
                                () -> {
                                    workspace.showPage(page);
                                    Window owner = SwingUtilities.getWindowAncestor(workspace);
                                    if (owner != null) {
                                        owner.toFront();
                                        owner.requestFocus();
                                    }
                                });
                button.setFont(new Font("Segoe UI", Font.BOLD, 11));
                button.setToolTipText("Open " + page + " in the main window");
                navigation.add(button);
            }
            navigation.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(2, 0, 0, 0, AppStyle.LINE),
                            new EmptyBorder(14, 0, 0, 0)));
            top.add(navigation, BorderLayout.CENTER);
            rail.add(top, BorderLayout.NORTH);
            Runnable update = summary::refresh;
            java.beans.PropertyChangeListener nameUpdate = e -> summary.refresh();
            workspace.characterPanel().addPropertyChangeListener("characterState", nameUpdate);
            update.run();
            workspace.playSession().addListener(update);
            workspace.playSession().selection().addChangeListener(update);
            window.addWindowListener(
                    new java.awt.event.WindowAdapter() {
                        @Override
                        public void windowClosed(java.awt.event.WindowEvent e) {
                            workspace.playSession().removeListener(update);
                            workspace.playSession().selection().removeChangeListener(update);
                            workspace
                                    .characterPanel()
                                    .removePropertyChangeListener("characterState", nameUpdate);
                        }
                    });
            rail.putClientProperty("sidebarWidthBase", 280);
            rail.setPreferredSize(new Dimension(280, 0));
            add(rail, BorderLayout.WEST);
        }
    }

    static void install(JDialog window) {
        if (window.isModal()
                || Boolean.TRUE.equals(window.getRootPane().getClientProperty("bauhausShell")))
            return;
        Container content = window.getContentPane();
        if (content instanceof JComponent view
                && Boolean.FALSE.equals(view.getClientProperty("windowShell"))) return;
        if (!(content instanceof JPanel)
                || !content.getClass().getPackageName().startsWith("dndcompanion.ui")) return;
        GrimoirePanel workspace = null;
        for (Window owner = window.getOwner(); owner != null; owner = owner.getOwner()) {
            if (owner instanceof RootPaneContainer root
                    && root.getContentPane() instanceof GrimoirePanel panel) {
                workspace = panel;
                break;
            }
        }
        window.getRootPane().putClientProperty("bauhausShell", true);
        window.getRootPane().putClientProperty("workspaceContent", content);
        window.setContentPane(new WindowShell(window, content, workspace));
        if (workspace != null) {
            Dimension old = window.getSize(), minimum = window.getMinimumSize();
            Rectangle screen = window.getGraphicsConfiguration().getBounds();
            window.setSize(Math.min(screen.width - 60, old.width + 280), old.height);
            window.setMinimumSize(
                    new Dimension(
                            Math.min(screen.width - 60, minimum.width + 280), minimum.height));
            window.setLocationRelativeTo(window.getOwner());
        }
    }
}
