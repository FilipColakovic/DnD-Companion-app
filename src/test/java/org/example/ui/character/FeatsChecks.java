package org.example.ui.character;

import org.example.domain.character.CharacterState;
import org.example.domain.character.FeatCatalog;
import org.example.domain.character.PlayState;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** Feat navigation, filters, prerequisite validation, previews and checkpoint persistence. */
public final class FeatsChecks {
    public static void main(String[] args) throws Exception {
        check(FeatCatalog.all().size() == 75, "Complete PHB-only catalog");
        for (var expected :
                Map.of(
                                FeatCatalog.Category.ORIGIN,
                                10,
                                FeatCatalog.Category.GENERAL,
                                43,
                                FeatCatalog.Category.FIGHTING_STYLE,
                                10,
                                FeatCatalog.Category.EPIC_BOON,
                                12)
                        .entrySet())
            check(
                    FeatCatalog.all().stream()
                                    .filter(f -> f.category() == expected.getKey())
                                    .count()
                            == expected.getValue(),
                    "Correct category count: " + expected.getKey());
        check(
                FeatCatalog.all().stream()
                        .allMatch(f -> f.sourceUrl().endsWith("?expansion=32231")),
                "Every feat links to 2024 PHB");
        var defaults = CharacterState.defaults();
        var grouped = FeatListModel.ordered(FeatCatalog.all(), defaults, true);
        check(
                grouped.subList(0, 10).stream()
                        .allMatch(
                                feat ->
                                        FeatListModel.group(feat, defaults)
                                                == FeatListModel.Group.AVAILABLE),
                "Level-one origin feats precede locked feats");
        check(
                grouped.subList(10, 75).stream()
                        .allMatch(
                                feat ->
                                        FeatListModel.group(feat, defaults)
                                                == FeatListModel.Group.LOCKED),
                "Remaining level-one entries are grouped as locked");
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel dashboard =
                            new GrimoirePanel(
                                    new CharacterState(
                                            3,
                                            17,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            Set.of("Fire Bolt", "Hold Person"),
                                            Species.HUMAN));
                    JFrame frame = new JFrame();
                    frame.setContentPane(dashboard);
                    frame.setSize(1480, 1000);
                    frame.setVisible(true);
                    try {
                        dashboard.showPage(GrimoirePanel.FEATS);
                        layout(frame);
                        FeatsPanel panel = find(dashboard, FeatsPanel.class, c -> true);
                        check(panel.visibleFeatNames().size() == 75, "All feats initially visible");
                        check(
                                accessible(panel, JToggleButton.class, "Feat category All")
                                                .getText()
                                                .endsWith("75")
                                        && accessible(
                                                        panel,
                                                        JToggleButton.class,
                                                        "Feat category Origin")
                                                .getText()
                                                .endsWith("10"),
                                "Tabs show catalog counts");
                        panel.inspect("War Caster");
                        JButton equip = accessible(panel, JButton.class, "Equip War Caster");
                        check(!equip.isEnabled(), "Level 3 cannot equip War Caster");
                        var before = dashboard.snapshot();
                        accessible(panel, JSpinner.class, "Feat preview level").setValue(4);
                        check(
                                dashboard.snapshot().equals(before),
                                "Milestone preview changes no saved inputs or choices");
                        dashboard.characterController().setLevel(4);
                        accessible(panel, JButton.class, "Equip War Caster").doClick(0);
                        check(
                                dashboard.playSession().feats().containsKey("War Caster"),
                                "Inspector equips an eligible feat");
                        check(
                                panel.visibleFeatNames().getFirst().equals("War Caster"),
                                "Equipped feat moves to top");
                        JTextArea notes =
                                accessible(panel, JTextArea.class, "Notes for War Caster");
                        notes.setText("Chosen ability: CHA\nApply +1 on character sheet.");
                        accessible(panel, JButton.class, "Equip War Caster").doClick(0);
                        check(
                                dashboard
                                        .snapshot()
                                        .play()
                                        .feats()
                                        .get("War Caster")
                                        .contains("CHA"),
                                "Feat notes enter character snapshot");
                        check(
                                PlayState.decode(dashboard.snapshot().play().encode())
                                        .equals(dashboard.snapshot().play()),
                                "Feat choices and multiline notes round-trip");
                        find(
                                        panel,
                                        JButton.class,
                                        c ->
                                                ((JButton) c)
                                                        .getText()
                                                        .startsWith("Show choices & notes"))
                                .doClick(0);
                        find(
                                        panel,
                                        JButton.class,
                                        c ->
                                                ((JButton) c)
                                                        .getText()
                                                        .startsWith("Show milestone preview"))
                                .doClick(0);
                        notes.setText("Unsaved draft kept through resource refreshes.");
                        notes.setCaretPosition(9);
                        JSpinner preview = accessible(panel, JSpinner.class, "Feat preview level");
                        preview.setValue(7);
                        layout(frame);
                        JScrollPane inspectorScroll =
                                (JScrollPane)
                                        find(panel, JSplitPane.class, c -> true)
                                                .getRightComponent();
                        inspectorScroll.getViewport().setViewPosition(new Point(0, 30));
                        Point scrollPosition = inspectorScroll.getViewport().getViewPosition();
                        dashboard.playSession().setRemainingPoints(0);
                        layout(frame);
                        check(
                                accessible(panel, JTextArea.class, "Notes for War Caster") == notes
                                        && notes.getText().startsWith("Unsaved draft")
                                        && notes.getCaretPosition() == 9,
                                "Resource refresh preserves the notes editor, draft and cursor");
                        check(
                                accessible(panel, JSpinner.class, "Feat preview level") == preview
                                        && preview.getValue().equals(7),
                                "Resource refresh preserves preview control and level");
                        check(
                                inspectorScroll
                                        .getViewport()
                                        .getViewPosition()
                                        .equals(scrollPosition),
                                "Resource refresh preserves inspector scroll position");
                        dashboard.playSession().setRemainingPoints(4);
                        JTextField search = accessible(panel, JTextField.class, "Search feats");
                        search.setText("tele");
                        check(
                                panel.visibleFeatNames()
                                                .containsAll(
                                                        java.util.List.of(
                                                                "Telekinetic",
                                                                "Telepathic",
                                                                "Boon of Dimensional Travel"))
                                        && !panel.visibleFeatNames().contains("War Caster"),
                                "Search filters names and benefits");
                        search.setText("");
                        accessible(panel, JToggleButton.class, "Feat category Epic Boon")
                                .doClick(0);
                        check(panel.visibleFeatNames().size() == 12, "Epic Boon category filter");
                        find(
                                        panel,
                                        JCheckBox.class,
                                        c ->
                                                ((JCheckBox) c)
                                                        .getText()
                                                        .equals("Available at my level"))
                                .doClick(0);
                        check(
                                panel.visibleFeatNames().isEmpty(),
                                "Unavailable Epic Boons are filtered out");
                        find(panel, JButton.class, c -> ((JButton) c).getText().equals("Reset"))
                                .doClick(0);
                        check(
                                panel.visibleFeatNames().size() == 75,
                                "Reset restores the entire list");
                        JToggleButton origin =
                                accessible(panel, JToggleButton.class, "Feat category Origin");
                        JToggleButton boon =
                                accessible(panel, JToggleButton.class, "Feat category Epic Boon");
                        origin.doClick(0);
                        boon.doClick(0);
                        check(
                                panel.visibleFeatNames().size() == 22,
                                "Category buttons combine their results");
                        origin.doClick(0);
                        boon.doClick(0);
                        check(
                                panel.visibleFeatNames().size() == 75
                                        && accessible(
                                                        panel,
                                                        JToggleButton.class,
                                                        "Feat category All")
                                                .isSelected(),
                                "Deselecting every category returns to All");
                        JPanel alert = accessible(panel, JPanel.class, "Feat card Alert");
                        alert.dispatchEvent(
                                new java.awt.event.MouseEvent(
                                        alert,
                                        java.awt.event.MouseEvent.MOUSE_CLICKED,
                                        System.currentTimeMillis(),
                                        0,
                                        4,
                                        4,
                                        1,
                                        false,
                                        java.awt.event.MouseEvent.BUTTON1));
                        check(
                                accessible(panel, JTextArea.class, "Notes for Alert") != null,
                                "Clicking the card background updates the inspector");
                        panel.inspect("Archery");
                        check(
                                !accessible(panel, JButton.class, "Equip Archery").isEnabled(),
                                "Single-class Sorcerer lacks Fighting Style prerequisite");
                        panel.inspect("Athlete");
                        check(
                                !accessible(panel, JButton.class, "Equip Athlete").isEnabled(),
                                "Untracked ability prerequisite requires confirmation");
                        var checkpoint = dashboard.snapshot();
                        dashboard.setSaveLocked(true);
                        panel.inspect("War Caster");
                        accessible(panel, JTextArea.class, "Notes for War Caster")
                                .setText("Draft discarded by restore");
                        dashboard
                                .playSession()
                                .equipFeat(FeatCatalog.named("Alert"), "Background", 17, true);
                        dashboard.restore(checkpoint);
                        check(
                                dashboard
                                        .snapshot()
                                        .play()
                                        .feats()
                                        .equals(checkpoint.play().feats()),
                                "Checkpoint restoration discards experimental feats");
                        panel.inspect("War Caster");
                        check(
                                accessible(panel, JTextArea.class, "Notes for War Caster")
                                        .getText()
                                        .equals(checkpoint.play().feats().get("War Caster")),
                                "Inspector reflects restored notes");
                        check(
                                panel.visibleFeatNames().getFirst().equals("War Caster"),
                                "Restored recorded feat stays in the first group");
                        dashboard.showPage(GrimoirePanel.CHARACTER);
                        check(
                                find(
                                                dashboard,
                                                JButton.class,
                                                c ->
                                                        ((JButton) c)
                                                                .getText()
                                                                .startsWith("War Caster ·"))
                                        != null,
                                "Visible Character info lists selected feats");
                        dashboard.showPage(GrimoirePanel.FEATS);
                        panel.inspect("War Caster");
                        find(
                                        panel,
                                        JButton.class,
                                        c -> ((JButton) c).getText().equals("Open details window"))
                                .doClick(0);
                        check(
                                Arrays.stream(frame.getOwnedWindows()).anyMatch(Window::isVisible),
                                "Feat details open in a real window");
                        for (Window dialog : frame.getOwnedWindows()) dialog.dispose();
                        find(
                                        panel,
                                        JButton.class,
                                        c ->
                                                ((JButton) c)
                                                        .getText()
                                                        .startsWith("Hide choices & notes"))
                                .doClick(0);
                        find(
                                        panel,
                                        JButton.class,
                                        c ->
                                                ((JButton) c)
                                                        .getText()
                                                        .startsWith("Hide milestone preview"))
                                .doClick(0);
                        for (boolean dark : new boolean[] {false, true}) {
                            for (int textSize : new int[] {12, 14, 16, 18}) {
                                dashboard.playSession().appearance(dark, textSize);
                                layout(frame);
                                layout(frame);
                                preview(
                                        dashboard,
                                        "target/feats-"
                                                + (dark ? "dark" : "light")
                                                + "-"
                                                + textSize
                                                + ".png");
                            }
                        }
                        dashboard.playSession().appearance(false, 14);
                        layout(frame);
                        preview(dashboard, "target/feats-light.png");
                        dashboard.playSession().appearance(false, 18);
                        frame.setSize(980, 900);
                        layout(frame);
                        check(
                                find(panel, JSplitPane.class, c -> true).getOrientation()
                                        == JSplitPane.VERTICAL_SPLIT,
                                "Narrow layout stacks library and inspector");
                        layout(frame);
                        preview(dashboard, "target/feats-narrow-18.png");
                        dashboard.characterController().setLevel(1);
                        panel.inspect("War Caster");
                        check(
                                dashboard.playSession().feats().containsKey("War Caster"),
                                "Lowering level retains recorded feat choices");
                        find(
                                        panel,
                                        JButton.class,
                                        c -> ((JButton) c).getText().equals("Remove feat"))
                                .doClick(0);
                        check(
                                !dashboard.playSession().feats().containsKey("War Caster"),
                                "Ineligible recorded feats remain removable");
                    } finally {
                        frame.dispose();
                        dashboard.dispose();
                    }
                });
        System.out.println(
                "Passed: 75 PHB feats, navigation, filters, prerequisites, notes, previews,"
                    + " details, theme layout and checkpoint restoration.");
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        return find(root, type, c -> name.equals(c.getAccessibleContext().getAccessibleName()));
    }

    private static <T extends Component> T find(
            Container root, Class<T> type, Predicate<Component> predicate) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && predicate.test(child)) return type.cast(child);
            if (child instanceof Container container) {
                T result = find(container, type, predicate);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container nested) layout(nested);
    }

    private static void preview(JComponent panel, String file) {
        BufferedImage image =
                new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", Path.of(file).toFile());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
