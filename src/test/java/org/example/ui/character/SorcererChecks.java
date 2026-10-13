package org.example.ui.character;

import org.example.domain.character.SorcererRules;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;

/** Dependency-free rules and Swing interaction checks. Run with -Djava.awt.headless=true. */
public final class SorcererChecks {
    public static void main(String[] args) throws Exception {
        int[] totals = {2, 3, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22};
        int[] prepared = {
            2, 4, 10, 11, 15, 16, 19, 20, 24, 25, 26, 26, 27, 27, 28, 28, 29, 30, 31, 32
        };
        int[] cantrips = {4, 4, 5, 6, 6, 6, 6, 6, 6, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7};
        for (int level = 1; level <= 20; level++) {
            var stats = SorcererRules.calculate(level, 17);
            check(stats.totalSpellSlots() == totals[level - 1], "Slot total at " + level);
            check(stats.preparedSpells() == prepared[level - 1], "Prepared spells at " + level);
            check(stats.cantrips() == cantrips[level - 1], "Cantrips at " + level);
            check(stats.sorceryPoints() == (level == 1 ? 0 : level), "Sorcery points");
            for (int charisma = 1; charisma <= 30; charisma++) {
                var result = SorcererRules.calculate(level, charisma);
                int modifier = (int) Math.floor((charisma - 10) / 2.0);
                check(result.charismaModifier() == modifier, "Charisma rounding");
                check(result.spellSaveDc() == 8 + result.spellAttack(), "DC and attack relation");
            }
        }
        var first = SorcererRules.calculate(1, 17);
        check(first.spellSaveDc() == 13 && first.spellAttack() == 5, "Screenshot defaults");
        var last = SorcererRules.calculate(20, 20);
        check(last.spellSaveDc() == 19 && last.spellAttack() == 11, "Level 20 proficiency");
        check(
                last.spellSlots().equals(java.util.List.of(4, 3, 3, 3, 3, 2, 2, 1, 1)),
                "Level 20 slots");
        check(SorcererRules.Subclass.values().length == 4, "Only four PHB 2024 subclasses");
        for (var subclass : SorcererRules.Subclass.values()) {
            for (int level = 1; level <= 20; level++) {
                var stats = SorcererRules.calculate(level, 17, subclass);
                int bonus =
                        level < 3 || subclass == SorcererRules.Subclass.WILD_MAGIC
                                ? 0
                                : new int[] {4, 6, 8, 10}[Math.min(3, (level - 3) / 2)];
                check(stats.subclassPreparedSpells() == bonus, "Subclass prepared spells");
                int expectedCantrips =
                        cantrips[level - 1]
                                - (level >= 3 && subclass != SorcererRules.Subclass.ABERRANT
                                        ? 1
                                        : 0);
                check(stats.cantrips() == expectedCantrips, "Subclass cantrips");
                check(
                        stats.spellSlots().equals(SorcererRules.calculate(level, 17).spellSlots()),
                        "Subclass preserves slots");
            }
        }
        for (int[] invalid : new int[][] {{0, 17}, {21, 17}, {1, 0}, {1, 31}}) {
            try {
                SorcererRules.calculate(invalid[0], invalid[1]);
                throw new AssertionError("Invalid inputs accepted");
            } catch (IllegalArgumentException expected) {
            }
        }
        SwingUtilities.invokeAndWait(
                () -> {
                    SorcererPanel panel = new SorcererPanel();
                    panel.setSize(888, 660);
                    layout(panel);
                    check(hasText(panel, "13") && hasText(panel, "+5"), "Initial UI stats");
                    SubclassSelector selector = find(panel, SubclassSelector.class, null);
                    JPopupMenu menu = selector.createMenu();
                    check(menu.getComponentCount() == 4, "Four subclass menu options");
                    check(
                            menu.getPreferredSize().width >= selector.getWidth(),
                            "Menu spans the selector width");
                    int selectedOptions = 0;
                    for (Component component : menu.getComponents()) {
                        JRadioButtonMenuItem item = (JRadioButtonMenuItem) component;
                        check(
                                item.getFont().getSize() == 16
                                        && item.getPreferredSize().height >= 44,
                                "Readable subclass menu options");
                        if (item.isSelected()) {
                            selectedOptions++;
                        }
                    }
                    check(selectedOptions == 1, "One subclass marked as selected");
                    ((JRadioButtonMenuItem) menu.getComponent(3)).doClick(0);
                    check(
                            selector.getText().contains("Wild Magic"),
                            "Menu click updates the main selector");
                    check(
                            ((JRadioButtonMenuItem) selector.createMenu().getComponent(3))
                                    .isSelected(),
                            "Reopened menu marks the chosen subclass");
                    panel.selectSubclass(SorcererRules.Subclass.ABERRANT);
                    menu = selector.createMenu();
                    menu.setSize(menu.getPreferredSize());
                    layout(menu);
                    BufferedImage menuImage =
                            new BufferedImage(
                                    menu.getWidth(), menu.getHeight(), BufferedImage.TYPE_INT_RGB);
                    Graphics2D menuGraphics = menuImage.createGraphics();
                    menuGraphics.setColor(Color.WHITE);
                    menuGraphics.fillRect(0, 0, menu.getWidth(), menu.getHeight());
                    menu.printAll(menuGraphics);
                    menuGraphics.dispose();
                    try {
                        ImageIO.write(
                                menuImage, "png", new File("target/subclass-menu-preview.png"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                    int initialHeight = panel.getPreferredSize().height;
                    check(
                            find(panel, JScrollPane.class, null) == null,
                            "Calculator has no scroll pane");
                    find(panel, JSlider.class, null).setValue(20);
                    check(hasText(panel, "32") && hasText(panel, "22"), "Slider refresh");
                    check(
                            panel.getPreferredSize().height > initialHeight,
                            "Content grows for more slot rows");
                    panel.selectSubclass(SorcererRules.Subclass.WILD_MAGIC);
                    check(
                            hasText(panel, "22") && hasText(panel, "6"),
                            "Wild Magic selection refreshes counts");
                    panel.selectSubclass(SorcererRules.Subclass.CLOCKWORK);
                    check(
                            hasText(panel, "32") && hasText(panel, "6"),
                            "Clockwork selection refreshes counts");
                    panel.selectSubclass(SorcererRules.Subclass.DRACONIC);
                    check(
                            hasText(panel, "32") && hasText(panel, "6"),
                            "Draconic selection refreshes counts");
                    panel.selectSubclass(SorcererRules.Subclass.ABERRANT);
                    check(hasText(panel, "7"), "Aberrant adds Mind Sliver");
                    if (!GraphicsEnvironment.isHeadless()) {
                        JFrame frame = new JFrame();
                        try {
                            SorcererPanel hosted = new SorcererPanel();
                            frame.setContentPane(hosted);
                            frame.pack();
                            hosted.fitWindowToContent();
                            int startHeight = frame.getHeight();
                            find(hosted, JSlider.class, null).setValue(20);
                            frame.validate();
                            check(
                                    frame.getHeight() > startHeight,
                                    "Frame grows automatically at high levels");
                            check(
                                    frame.getContentPane().getHeight()
                                            >= hosted.getPreferredSize().height,
                                    "Frame fits all content");
                            check(
                                    frame.getMinimumSize().height
                                            >= hosted.getPreferredSize().height,
                                    "Minimum frame size prevents clipping");
                        } finally {
                            frame.dispose();
                        }
                    }
                    panel.setSize(888, Math.max(660, panel.getPreferredSize().height));
                    layout(panel);
                    savePreview(panel, "target/calculator-level20-preview.png");
                    JButton plus = find(panel, JButton.class, "+");
                    JButton minus = find(panel, JButton.class, "−");
                    for (int i = 0; i < 20; i++) {
                        plus.doClick(0);
                    }
                    check(!plus.isEnabled() && hasText(panel, "30"), "Upper Charisma bound");
                    for (int i = 0; i < 40; i++) {
                        minus.doClick(0);
                    }
                    check(
                            !minus.isEnabled() && hasText(panel, "1") && hasText(panel, "+1"),
                            "Lower Charisma bound");
                    for (int i = 0; i < 16; i++) {
                        plus.doClick(0);
                    }
                    find(panel, JSlider.class, null).setValue(1);
                    panel.setSize(888, Math.max(660, panel.getPreferredSize().height));
                    layout(panel);
                    savePreview(panel, "target/calculator-preview.png");
                });
        System.out.println(
                "Passed: 20 levels, 600 ability combinations, four subclasses, input bounds, and"
                    + " Swing control updates."
                        + (GraphicsEnvironment.isHeadless()
                                ? ""
                                : " Automatic frame growth also passed."));
    }

    private static void savePreview(SorcererPanel panel, String path) {
        BufferedImage preview =
                new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = preview.createGraphics();
        panel.printAll(g);
        g.dispose();
        try {
            ImageIO.write(preview, "png", new File(path));
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents()) {
            if (child instanceof Container nested) {
                layout(nested);
            }
        }
    }

    private static boolean hasText(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JLabel label && text.equals(label.getText())) {
                return true;
            }
            if (child instanceof Container nested && hasText(nested, text)) {
                return true;
            }
        }
        return false;
    }

    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)
                    && (text == null
                            || child instanceof JButton button && text.equals(button.getText()))) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T result = find(nested, type, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }
}
