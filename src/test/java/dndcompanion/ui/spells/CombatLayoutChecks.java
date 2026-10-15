package dndcompanion.ui.spells;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
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
                                            Set.of("Web", "Shield", "Friends"),
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
                        combat.selectSpell("Friends");
                        check(
                                find(combat, JTextArea.class, "combat-spell-rolls")
                                        .getText()
                                        .equals("DC 13 Wis"),
                                "Friends shows its Wisdom save without an attack modifier");
                        check(
                                find(combat, JTextArea.class, "combat-spell-summary")
                                        .getText()
                                        .contains("Charmed"),
                                "Compact Friends summary retains the effect of the saving throw");
                        check(
                                findButton(combat, "Full details") != null,
                                "Complete spell entry remains accessible from the summary");
                        combat.selectSpell("Web");
                        check(!combat.isDiceTrayVisible(), "Web has no casting dice tray");
                        check(
                                dossier.getHeight() > 180 && dossier.getHeight() < 500,
                                "Spell dossier remains compact above Metamagic controls");
                        check(findButton(combat, "End Turn") == null, "No End Turn button remains");
                        var session = workspace.playSession();
                        session.choose(Metamagic.SUBTLE, true);
                        session.choose(Metamagic.EXTENDED, true);
                        CombatMetamagic modifiers =
                                find(combat, CombatMetamagic.class, "combat-metamagic");
                        JCheckBox subtle = accessible(combat, "Cast with Subtle Spell");
                        subtle.doClick(0);
                        check(
                                modifiers.selected().equals(Set.of(Metamagic.SUBTLE)),
                                "Modifier selected for cast");
                        session.setRemainingPoints(2);
                        check(subtle.isSelected(), "Resource refresh preserves modifier selection");
                        combat.selectSpell("Shield");
                        check(
                                modifiers.selected().isEmpty(),
                                "Changing spell clears cast modifiers");
                        subtle.doClick(0);
                        int slotsBefore = session.remainingSlots(2);
                        findButton(combat, "Cast").doClick(0);
                        check(
                                session.remainingPoints() == 1
                                        && session.remainingSlots(2) == slotsBefore - 1,
                                "Cast spends selected modifier cost and spell slot");
                        combat.selectSpell("Web");
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
                        for (boolean dark : new boolean[] {false, true}) {
                            for (int size : new int[] {12, 14, 18}) {
                                session.appearance(dark, size);
                                AppStyle.appearance(dark, size);
                                AppStyle.applyAppearance(workspace);
                                for (int width : new int[] {1480, 1000}) {
                                    frame.setSize(width, 960);
                                    frame.validate();
                                    layout(workspace);
                                    frame.validate();
                                    layout(workspace);
                                    var grid =
                                            find(
                                                    combat,
                                                    CombatArsenalGrid.class,
                                                    "combat-arsenal-grid");
                                    if (width == 1480)
                                        check(
                                                ((java.awt.GridLayout) grid.getLayout())
                                                                .getColumns()
                                                        == 2,
                                                "Wide combat layout has two spell columns");
                                    BufferedImage rendered =
                                            new BufferedImage(
                                                    workspace.getWidth(),
                                                    workspace.getHeight(),
                                                    BufferedImage.TYPE_INT_RGB);
                                    Graphics2D g = rendered.createGraphics();
                                    workspace.printAll(g);
                                    g.dispose();
                                    try {
                                        ImageIO.write(
                                                rendered,
                                                "png",
                                                Path.of(
                                                                "target/combat-style-"
                                                                        + (dark ? "dark" : "light")
                                                                        + "-"
                                                                        + size
                                                                        + "-"
                                                                        + width
                                                                        + ".png")
                                                        .toFile());
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }
                    } finally {
                        frame.dispose();
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: responsive two-column combat cards, relevant dossier, live Metamagic"
                    + " costs, preserved selection, casting resources and 12 theme/width/text-size"
                    + " previews.");
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) if (c instanceof Container nested) layout(nested);
    }

    private static JCheckBox accessible(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof JCheckBox box
                    && name.equals(box.getAccessibleContext().getAccessibleName())) return box;
            if (child instanceof Container nested) {
                JCheckBox found = accessible(nested, name);
                if (found != null) return found;
            }
        }
        return null;
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
