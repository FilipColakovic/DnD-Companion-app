package dndcompanion.application;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.ClassId;
import dndcompanion.persistence.SQLiteCharacterRepository;
import dndcompanion.ui.common.AppStyle;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public final class RosterWindowChecks {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target"), "roster-window-");
        Path database = directory.resolve("characters.db");
        var repository = new SQLiteCharacterRepository(database);
        UUID sorcerer = repository.create(CharacterState.create("Pajo Patak", ClassId.SORCERER));
        UUID fighter = repository.create(CharacterState.create("Mira Champion", ClassId.FIGHTER));
        repository.create(CharacterState.create("Ignis Vane", ClassId.SORCERER));
        repository.metadata(fighter, false, "Campaign A");
        AtomicReference<ApplicationController> reference = new AtomicReference<>();
        SwingUtilities.invokeAndWait(
                () -> {
                    var application = new ApplicationController(directory, repository);
                    reference.set(application);
                    application.window().setVisible(true);
                    application.start();
                });
        var application = reference.get();
        try {
            await(application);
            SwingUtilities.invokeAndWait(
                    () ->
                            check(
                                    application
                                            .window()
                                            .getContentPane()
                                            .getName()
                                            .equals("character-roster"),
                                    "App starts on roster"));
            SwingUtilities.invokeAndWait(() -> application.open(sorcerer));
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> {
                        application.activePanel().characterController().setLevel(4);
                        application.showRoster();
                    });
            await(application);
            check(
                    repository.load(sorcerer).state().level() == 4,
                    "Unlocked switch saves automatically");
            SwingUtilities.invokeAndWait(() -> application.open(sorcerer));
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> application.activePanel().characterController().requestSaveLock(true));
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> {
                        application.activePanel().characterController().setLevel(7);
                        application.showRoster();
                    });
            await(application);
            check(
                    repository.load(sorcerer).state().level() == 4
                            && repository.load(sorcerer).locked(),
                    "Locked experiments discarded on return");
            SwingUtilities.invokeAndWait(() -> application.open(fighter));
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> {
                        check(
                                !application.activePanel().playSession().sorcerer(),
                                "Fighter opens in shared dashboard");
                        check(
                                !named(application.window(), "nav-Metamagic").isVisible(),
                                "Fighter hides Metamagic navigation");
                        application.activePanel().characterController().setLevel(5);
                    });
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.executeUpdate(
                        "CREATE TRIGGER reject_save BEFORE UPDATE OF snapshot ON characters BEGIN"
                                + " SELECT RAISE(FAIL, 'test save failure'); END");
            }
            SwingUtilities.invokeAndWait(
                    () -> {
                        Timer dismiss = new Timer(100, null);
                        dismiss.addActionListener(
                                event -> {
                                    for (Window window : Window.getWindows())
                                        if (window instanceof JDialog dialog
                                                && dialog.isShowing()) {
                                            dialog.dispose();
                                            dismiss.stop();
                                        }
                                });
                        dismiss.start();
                        application.showRoster();
                    });
            await(application);
            SwingUtilities.invokeAndWait(
                    () ->
                            check(
                                    application.activeId().equals(fighter)
                                            && application.activePanel().snapshot().level() == 5,
                                    "Failed save keeps active screen and unsaved state"));
            check(
                    repository.load(fighter).state().level() == 1,
                    "Failed save leaves previous snapshot intact");
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.executeUpdate("DROP TRIGGER reject_save");
            }
            SwingUtilities.invokeAndWait(application::showRoster);
            await(application);
            check(
                    repository.load(fighter).state().level() == 5,
                    "Successful retry saves the correct character");
            for (boolean dark : new boolean[] {false, true})
                for (int size : new int[] {12, 18})
                    for (int width : new int[] {1480, 1000}) {
                        SwingUtilities.invokeAndWait(
                                () -> {
                                    AppStyle.appearance(dark, size);
                                    AppStyle.applyAppearance(application.window());
                                    application.window().setSize(width, 960);
                                    application.window().validate();
                                    layout(application.window());
                                });
                        SwingUtilities.invokeAndWait(
                                () -> {
                                    application.window().validate();
                                    layout(application.window());
                                    var root = application.window().getContentPane();
                                    BufferedImage image =
                                            new BufferedImage(
                                                    root.getWidth(),
                                                    root.getHeight(),
                                                    BufferedImage.TYPE_INT_RGB);
                                    Graphics2D graphics = image.createGraphics();
                                    root.printAll(graphics);
                                    graphics.dispose();
                                    try {
                                        ImageIO.write(
                                                image,
                                                "png",
                                                Path.of(
                                                                "target/roster-"
                                                                        + (dark ? "dark" : "light")
                                                                        + "-"
                                                                        + size
                                                                        + "-"
                                                                        + width
                                                                        + ".png")
                                                        .toFile());
                                    } catch (Exception error) {
                                        throw new RuntimeException(error);
                                    }
                                });
                    }
            SwingUtilities.invokeAndWait(() -> application.open(fighter));
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> {
                        Timer dismiss = new Timer(100, null);
                        dismiss.addActionListener(
                                event -> {
                                    for (Window window : Window.getWindows())
                                        if (window instanceof JDialog dialog
                                                && dialog.isShowing()) {
                                            dialog.dispose();
                                            dismiss.stop();
                                        }
                                });
                        dismiss.start();
                        application.open(UUID.randomUUID());
                    });
            await(application);
            SwingUtilities.invokeAndWait(
                    () -> {
                        check(
                                application.activeId().equals(fighter),
                                "Failed load retains current character and dashboard");
                        application.activePanel().characterController().setLevel(18);
                        application
                                .activePanel()
                                .characterController()
                                .selectSpecies(dndcompanion.domain.character.Species.ORC);
                        var session = application.activePanel().playSession();
                        session.setSheet(
                                session.sheet()
                                        .withChoice(
                                                "fighter.skills",
                                                java.util.List.of("Athletics", "Perception"))
                                        .withChoice("fighter.style", java.util.List.of("Defense"))
                                        .withChoice(
                                                "fighter.style.extra", java.util.List.of("Dueling"))
                                        .withChoice(
                                                "fighter.mastery",
                                                java.util.List.of(
                                                        "Longsword",
                                                        "Longbow",
                                                        "Greatsword",
                                                        "Javelin",
                                                        "Spear",
                                                        "Battleaxe"))
                                        .withChoice(
                                                "equipment.weapon", java.util.List.of("Longsword"))
                                        .withChoice("equipment.armor", java.util.List.of("Plate")));
                        check(
                                named(application.window(), "class-actions").isVisible(),
                                "Fighter has class action controls");
                        check(
                                session.pendingDecisions() == 0,
                                "Fighter choices resolve independently of Sorcerer choices");
                    });
            for (boolean dark : new boolean[] {false, true})
                for (int size : new int[] {12, 18})
                    for (int width : new int[] {1480, 1000}) {
                        for (String page :
                                java.util.List.of(
                                        dndcompanion.ui.GrimoirePanel.COMBAT,
                                        dndcompanion.ui.GrimoirePanel.CHARACTER)) {
                            SwingUtilities.invokeAndWait(
                                    () -> {
                                        application
                                                .activePanel()
                                                .playSession()
                                                .appearance(dark, size);
                                        application.activePanel().showPage(page);
                                        application.window().setSize(width, 960);
                                        application.window().validate();
                                        layout(application.window());
                                    });
                            SwingUtilities.invokeAndWait(
                                    () -> {
                                        application.window().validate();
                                        layout(application.window());
                                        var root = application.window().getContentPane();
                                        checkTop(root);
                                        BufferedImage image =
                                                new BufferedImage(
                                                        root.getWidth(),
                                                        root.getHeight(),
                                                        BufferedImage.TYPE_INT_RGB);
                                        Graphics2D graphics = image.createGraphics();
                                        root.printAll(graphics);
                                        graphics.dispose();
                                        try {
                                            ImageIO.write(
                                                    image,
                                                    "png",
                                                    Path.of(
                                                                    "target/fighter-"
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
                                    });
                        }
                    }
        } finally {
            SwingUtilities.invokeAndWait(
                    () -> {
                        application.close();
                        application.window().dispose();
                    });
        }
        System.out.println(
                "Passed: roster startup, UUID switching, save protection, failed-write retention,"
                        + " retry, Fighter capabilities and 24 responsive theme previews.");
    }

    private static void await(ApplicationController application) throws Exception {
        long deadline = System.nanoTime() + 20_000_000_000L;
        while (System.nanoTime() < deadline) {
            boolean[] busy = {true};
            SwingUtilities.invokeAndWait(() -> busy[0] = application.busy());
            if (!busy[0]) {
                SwingUtilities.invokeAndWait(() -> {});
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("Desktop operation did not finish");
    }

    private static Component named(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) return child;
            if (child instanceof Container nested) {
                Component found = named(nested, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container nested) layout(nested);
    }

    private static void checkTop(Container root) {
        for (Component child : root.getComponents()) {
            if (child instanceof javax.swing.JScrollPane scroll && scroll.isShowing())
                check(
                        scroll.getViewport().getViewPosition().y == 0,
                        "Fighter navigation opens every visible section at the top");
            if (child instanceof Container nested) checkTop(nested);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
