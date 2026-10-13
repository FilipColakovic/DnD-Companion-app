package org.example;

import org.example.domain.character.CharacterState;
import org.example.domain.character.Proficiency;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

/**
 * Persistent widget identity, editable drafts and the responsive/theme/text-size rendering matrix.
 */
public final class ComponentRefreshChecks {
    public static void main(String[] args) throws Exception {
        Path previews = Path.of("target", "verification", "components");
        Files.createDirectories(previews);
        SwingUtilities.invokeAndWait(
                () -> {
                    var workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            5,
                                            17,
                                            SorcererRules.Subclass.ABERRANT,
                                            Set.of("Fireball"),
                                            Species.HIGH_ELF));
                    try {
                        var library = workspace.libraryPanel();
                        library.inspect("Fireball");
                        JTextArea rules =
                                find(
                                        library,
                                        JTextArea.class,
                                        area -> area.getText().startsWith("A bright streak"));
                        check(rules != null, "Inspector contains Fireball rules");
                        rules.setCaretPosition(12);
                        rules.moveCaretPosition(24);
                        var viewport =
                                ((JScrollPane)
                                                SwingUtilities.getAncestorOfClass(
                                                        JScrollPane.class, rules))
                                        .getViewport();
                        Point position = viewport.getViewPosition();
                        workspace.playSession().setRemainingPoints(1);
                        check(
                                rules
                                        == find(
                                                library,
                                                JTextArea.class,
                                                area ->
                                                        area.getText()
                                                                .startsWith("A bright streak")),
                                "Resource refresh retains the inspector text widget");
                        check(
                                rules.getCaret().getDot() == 24 && rules.getCaret().getMark() == 12,
                                "Resource refresh retains text selection");
                        check(
                                viewport.getViewPosition().equals(position),
                                "Resource refresh retains inspector viewport");
                        workspace.showPage(GrimoirePanel.CHARACTER);
                        JTextArea training =
                                find(
                                        workspace,
                                        JTextArea.class,
                                        area ->
                                                "Training and equipment notes"
                                                        .equals(
                                                                area.getAccessibleContext()
                                                                        .getAccessibleName()));
                        training.setText("Equipment draft: staff, ink and paper");
                        training.setCaretPosition(10);
                        training.moveCaretPosition(17);
                        workspace.playSession().setRemainingSlots(1, 0);
                        check(
                                training.getText().equals("Equipment draft: staff, ink and paper")
                                        && training.getCaret().getDot() == 17
                                        && training.getCaret().getMark() == 10,
                                "Unrelated resource updates preserve sheet drafts and selection");
                        for (Proficiency tier : Proficiency.values()) {
                            check(
                                    Proficiency.fromStoredValue(tier.storedValue()) == tier,
                                    "Proficiency save adapter");
                        }
                        for (boolean dark : new boolean[] {false, true})
                            for (int size : new int[] {12, 14, 16, 18})
                                for (int width : new int[] {900, 1500}) {
                                    workspace.playSession().appearance(dark, size);
                                    workspace.setSize(width, 1000);
                                    for (String page :
                                            new String[] {
                                                GrimoirePanel.CHARACTER,
                                                GrimoirePanel.LIBRARY,
                                                GrimoirePanel.COMBAT,
                                                GrimoirePanel.FEATS
                                            }) {
                                        workspace.showPage(page);
                                        layout(workspace);
                                        check(
                                                workspace.getPreferredSize().height > 0,
                                                "Valid responsive layout");
                                        var image =
                                                new BufferedImage(
                                                        width, 1000, BufferedImage.TYPE_INT_RGB);
                                        Graphics2D graphics = image.createGraphics();
                                        workspace.printAll(graphics);
                                        graphics.dispose();
                                        try {
                                            ImageIO.write(
                                                    image,
                                                    "png",
                                                    previews.resolve(
                                                                    page.replace(' ', '-')
                                                                                    .replace(
                                                                                            '&',
                                                                                            '_')
                                                                            + "-"
                                                                            + (dark
                                                                                    ? "dark"
                                                                                    : "light")
                                                                            + "-"
                                                                            + size
                                                                            + "-"
                                                                            + width
                                                                            + ".png")
                                                            .toFile());
                                        } catch (java.io.IOException error) {
                                            throw new java.io.UncheckedIOException(error);
                                        }
                                    }
                                }
                        workspace.dispose();
                        workspace.playSession().appearance(false, 14);
                        check(
                                rules.getCaret().getDot() == 24,
                                "Disposed view does not receive resource refreshes");
                    } finally {
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: persistent inspector widgets, drafts/caret/scroll, proficiency adapters"
                    + " and 64 theme/width/text-size previews.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container nested) layout(nested);
    }

    private static <T extends JComponent> T find(
            Container root, Class<T> type, java.util.function.Predicate<T> predicate) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && predicate.test(type.cast(child))) return type.cast(child);
            if (child instanceof Container nested) {
                T result = find(nested, type, predicate);
                if (result != null) return result;
            }
        }
        return null;
    }
}
