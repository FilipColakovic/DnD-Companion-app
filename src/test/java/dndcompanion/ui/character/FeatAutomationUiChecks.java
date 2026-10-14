package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

/** Actual Character confirmation, live casting source controls and responsive feat previews. */
public final class FeatAutomationUiChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    var workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            4,
                                            17,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            Set.of("Misty Step"),
                                            Species.ORC));
                    JFrame frame = new JFrame();
                    frame.setContentPane(workspace);
                    frame.setSize(1480, 1000);
                    frame.setVisible(true);
                    try {
                        var session = workspace.playSession();
                        session.setSheet(
                                session.sheet()
                                        .withChoice(
                                                "sorcerer.skills", List.of("Arcana", "Deception")));
                        session.equipFeat(FeatCatalog.named("Telekinetic"), "", 17, true);
                        workspace.showPage(GrimoirePanel.CHARACTER);
                        check(
                                named(workspace, "pending-character-decisions").isVisible(),
                                "Sidebar reminder for unconfigured feat");
                        accessible(
                                        workspace,
                                        JCheckBox.class,
                                        "feat.Telekinetic.ability option CHA")
                                .doClick(0);
                        accessible(workspace, JButton.class, "Confirm feat.Telekinetic.ability")
                                .doClick(0);
                        check(
                                !named(workspace, "pending-character-decisions").isVisible(),
                                "Confirmed feat clears reminder");
                        check(
                                accessible(workspace, JSpinner.class, "CHA score")
                                        .getValue()
                                        .equals(17),
                                "Base spinner survives automatic bonus");
                        check(
                                session.effectiveScore(
                                                dndcompanion.domain.character.CharacterSheet.Ability
                                                        .CHA,
                                                17)
                                        == 18,
                                "Effective score applies immediately");
                        check(
                                workspace.combatPanel().selectSpell("Mage Hand"),
                                "Granted cantrip appears immediately in combat");
                        session.equipFeat(FeatCatalog.named("Fey-Touched"), "", 17, true);
                        session.setSheet(
                                session.sheet()
                                        .withChoice("feat.Fey-Touched.ability", List.of("INT"))
                                        .withChoice("feat.Fey-Touched.spell", List.of("Bless")));
                        workspace.combatPanel().selectSpell("Misty Step");
                        workspace.showPage(GrimoirePanel.COMBAT);
                        JComboBox<?> source =
                                accessible(
                                        workspace.combatPanel(), JComboBox.class, "Casting source");
                        source.setSelectedItem("Fey-Touched");
                        int slots = session.remainingSlots(2);
                        button(workspace.combatPanel(), "Cast").doClick(0);
                        check(session.remainingSlots(2) == slots, "UI free cast preserves slots");
                        check(
                                session.freeCasts(
                                                session.selection().catalog().stream()
                                                        .filter(s -> s.name().equals("Misty Step"))
                                                        .findFirst()
                                                        .orElseThrow(),
                                                "Fey-Touched")
                                        == 0,
                                "UI consumes feat use");
                        button(workspace.combatPanel(), "Show equipment & feat rolls").doClick(0);
                        for (boolean dark : new boolean[] {false, true})
                            for (int size : new int[] {12, 18})
                                for (int width : new int[] {900, 1480}) {
                                    session.appearance(dark, size);
                                    frame.setSize(width, 1000);
                                    for (String page :
                                            List.of(
                                                    GrimoirePanel.CHARACTER,
                                                    GrimoirePanel.COMBAT)) {
                                        workspace.showPage(page);
                                        frame.validate();
                                        layout(workspace);
                                        if (page.equals(GrimoirePanel.COMBAT)) {
                                            Component arsenal =
                                                    named(
                                                            workspace.combatPanel(),
                                                            "spell-arsenal-card");
                                            Component dossier =
                                                    named(
                                                            workspace.combatPanel(),
                                                            "combat-spell-dossier");
                                            var listBounds =
                                                    SwingUtilities.convertRectangle(
                                                            arsenal.getParent(),
                                                            arsenal.getBounds(),
                                                            workspace.combatPanel());
                                            var detailBounds =
                                                    SwingUtilities.convertRectangle(
                                                            dossier.getParent(),
                                                            dossier.getBounds(),
                                                            workspace.combatPanel());
                                            check(
                                                    width == 900
                                                            ? detailBounds.y
                                                                    > listBounds.y
                                                                            + listBounds.height
                                                            : detailBounds.x
                                                                    > listBounds.x
                                                                            + listBounds.width,
                                                    "Narrow combat stacks readable columns; normal"
                                                        + " combat remains side by side");
                                        }
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
                                                    Path.of(
                                                                    "target",
                                                                    "feat-automation-"
                                                                            + page.replaceAll(
                                                                                    "[^A-Za-z]", "")
                                                                            + "-"
                                                                            + dark
                                                                            + "-"
                                                                            + size
                                                                            + "-"
                                                                            + width
                                                                            + ".png")
                                                            .toFile());
                                        } catch (Exception error) {
                                            throw new RuntimeException(error);
                                        }
                                    }
                                }
                        session.removeFeat("Telekinetic");
                        check(
                                session.effectiveScore(
                                                dndcompanion.domain.character.CharacterSheet.Ability
                                                        .CHA,
                                                17)
                                        == 17,
                                "UI feat removal reverses score");
                    } finally {
                        workspace.dispose();
                        frame.dispose();
                    }
                });
        SwingUtilities.invokeAndWait(
                () -> {
                    for (Window window : Window.getWindows()) window.dispose();
                });
        System.out.println(
                "Passed: live feat reminders, choice confirmation, automatic spell grants,"
                        + " free-cast controls and 16 theme/width/text-size previews.");
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container container) layout(container);
    }

    private static Component named(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container container) {
                Component found = named(container, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T accessible(Container root, Class<T> type, String name) {
        T found =
                find(
                        root,
                        type,
                        component ->
                                component.getAccessibleContext() != null
                                        && name.equals(
                                                component
                                                        .getAccessibleContext()
                                                        .getAccessibleName()));
        if (found == null) throw new AssertionError("Missing " + name);
        return found;
    }

    private static JButton button(Container root, String text) {
        JButton found =
                find(
                        root,
                        JButton.class,
                        component -> ((JButton) component).getText().equals(text));
        if (found == null) throw new AssertionError("Missing button " + text);
        return found;
    }

    private static <T> T find(
            Container root, Class<T> type, java.util.function.Predicate<Component> predicate) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && predicate.test(child)) return type.cast(child);
            if (child instanceof Container container) {
                T found = find(container, type, predicate);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
