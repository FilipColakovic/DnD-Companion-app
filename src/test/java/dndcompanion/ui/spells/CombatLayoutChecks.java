package dndcompanion.ui.spells;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public final class CombatLayoutChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    var workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            3,
                                            17,
                                            SorcererRules.Subclass.ABERRANT,
                                            Set.of("Web", "Shield"),
                                            Species.HUMAN));
                    JFrame frame = new JFrame();
                    frame.setContentPane(workspace);
                    frame.setSize(1480, 960);
                    frame.setVisible(true);
                    try {
                        var combat = workspace.combatPanel();
                        combat.selectSpell("Web");
                        frame.validate();
                        layout(workspace);
                        frame.validate();
                        layout(workspace);
                        JPanel dossier = find(combat, JPanel.class, "combat-spell-dossier");
                        check(!combat.isDiceTrayVisible(), "Web has no casting dice tray");
                        check(
                                dossier.getHeight() > 420,
                                "Spell dossier absorbs unused vertical space");
                        check(findButton(combat, "End Turn") == null, "No End Turn button remains");
                        BufferedImage image =
                                new BufferedImage(
                                        workspace.getWidth(),
                                        workspace.getHeight(),
                                        BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        workspace.printAll(graphics);
                        graphics.dispose();
                        try {
                            ImageIO.write(
                                    image,
                                    "png",
                                    Path.of("target/combat-expanded-dossier.png").toFile());
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    } finally {
                        frame.dispose();
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: save-only spell dossier fills available height and End Turn control is"
                        + " removed.");
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) if (c instanceof Container nested) layout(nested);
    }

    private static <T extends Component> T find(Container root, Class<T> type, String name) {
        for (Component c : root.getComponents()) {
            if (type.isInstance(c) && name.equals(c.getName())) return type.cast(c);
            if (c instanceof Container nested) {
                T found = find(nested, type, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static AbstractButton findButton(Container root, String name) {
        for (Component c : root.getComponents()) {
            if (c instanceof AbstractButton button && name.equals(button.getText())) return button;
            if (c instanceof Container nested) {
                var found = findButton(nested, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
