package dndcompanion;

import dndcompanion.application.ApplicationController;
import dndcompanion.application.LegacyWindowFactory;
import dndcompanion.persistence.CharacterStore;

import java.nio.file.Path;
import java.sql.SQLException;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(
                () -> {
                    var application = new ApplicationController(Path.of("data"));
                    application.window().setVisible(true);
                    application.start();
                });
    }

    /** Single-save compatibility entry point for existing window checks. */
    static JFrame createWindow(CharacterStore store) throws SQLException {
        return LegacyWindowFactory.createWindow(store);
    }
}
