package org.example;

import org.example.persistence.CharacterStore;
import org.example.ui.GrimoirePanel;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    try {
                        org.example.persistence.SQLiteSpellRepository.bundled();
                    } catch (IllegalStateException error) {
                        JOptionPane.showMessageDialog(
                                null,
                                error.getMessage() + "\n" + error.getCause().getMessage(),
                                "Spell catalog could not be loaded",
                                JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                    Path database = Path.of("data", "character.db").toAbsolutePath();
                    try {
                        JFrame window = createWindow(new CharacterStore(database));
                        window.setVisible(true);
                    } catch (IOException | SQLException error) {
                        JOptionPane.showMessageDialog(
                                null,
                                "Could not open the saved character.\n"
                                        + database
                                        + "\n"
                                        + error.getMessage(),
                                "Character could not be loaded",
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
    }

    static JFrame createWindow(CharacterStore store) throws SQLException {
        GrimoirePanel panel = new GrimoirePanel(store.load());
        panel.setSaveLocked(store.isSaveLocked());
        JFrame window = new JFrame("Arcane Grimoire · DnD Companion (2024)");
        org.example.ui.common.WindowChrome.install(window);
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
