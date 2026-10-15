package dndcompanion.application;

import dndcompanion.persistence.CharacterStore;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.WindowChrome;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Compatibility fixture for single-save window checks; production uses ApplicationController. */
public final class LegacyWindowFactory {
    private LegacyWindowFactory() {}

    public static JFrame createWindow(CharacterStore store) throws SQLException {
        GrimoirePanel panel = new GrimoirePanel(store.load());
        panel.setSaveLocked(store.isSaveLocked());
        JFrame window = new JFrame("Arcane Grimoire · DnD Companion (2024)");
        WindowChrome.install(window);
        window.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        window.setContentPane(panel);
        panel.setSaveActions(
                locked -> {
                    try {
                        if (locked) {
                            store.save(panel.snapshot(), true);
                            panel.markSaved();
                        } else {
                            store.setSaveLocked(false);
                        }
                    } catch (SQLException error) {
                        panel.setSaveLocked(!locked);
                        JOptionPane.showMessageDialog(
                                window,
                                "Could not change save protection.\n" + error.getMessage(),
                                "Save protection unchanged",
                                JOptionPane.ERROR_MESSAGE);
                    }
                },
                () -> {
                    try {
                        panel.restore(store.load());
                    } catch (SQLException error) {
                        JOptionPane.showMessageDialog(
                                window,
                                "Could not restore the saved character.\n" + error.getMessage(),
                                "Character could not be restored",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
        panel.setSaveAction(
                state -> {
                    try {
                        store.save(state);
                        panel.markSaved();
                    } catch (SQLException error) {
                        JOptionPane.showMessageDialog(
                                window,
                                "Could not save the character.\n" + error.getMessage(),
                                "Character could not be saved",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
        window.addWindowListener(
                new WindowAdapter() {
                    @Override
                    public void windowClosed(WindowEvent event) {
                        panel.dispose();
                    }

                    @Override
                    public void windowClosing(WindowEvent event) {
                        try {
                            if (!panel.isSaveLocked()) {
                                store.save(panel.snapshot());
                            }
                            window.dispose();
                        } catch (SQLException error) {
                            JOptionPane.showMessageDialog(
                                    window,
                                    "Could not save your character. The window will stay open so"
                                            + " you can try again.\n"
                                            + store.path()
                                            + "\n"
                                            + error.getMessage(),
                                    "Character could not be saved",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }
                });
        java.awt.Rectangle screen =
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
        window.setMinimumSize(
                new java.awt.Dimension(Math.min(1100, screen.width), Math.min(720, screen.height)));
        window.setSize(Math.min(1480, screen.width), Math.min(960, screen.height));
        window.setLocationRelativeTo(null);
        return window;
    }
}
