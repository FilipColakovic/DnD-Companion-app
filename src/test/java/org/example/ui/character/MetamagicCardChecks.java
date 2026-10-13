package org.example.ui.character;

import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellSelection;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.SpellDetailPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** Selection limits, live prepared synergies, drill-down actions and responsive cards. */
public final class MetamagicCardChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    AppStyle.appearance(false, 14);
                    SpellSelection selection = new SpellSelection();
                    selection.setCharacter(5, SorcererRules.Subclass.WILD_MAGIC, Species.HUMAN);
                    selection.restoreChoices(Set.of("Fire Bolt", "Chromatic Orb", "Hold Person"));
                    PlaySession session = new PlaySession(selection);
                    MetamagicPanel panel = new MetamagicPanel(session);
                    JFrame frame = new JFrame();
                    frame.setContentPane(panel);
                    frame.setSize(1000, 900);
                    frame.setVisible(true);
                    try {
                        JScrollPane scroll = (JScrollPane) panel.getComponent(1);
                        JPanel grid = (JPanel) scroll.getViewport().getView();
                        check(
                                grid.getBackground().equals(AppStyle.BACKGROUND)
                                        && scroll.getViewport()
                                                .getBackground()
                                                .equals(AppStyle.BACKGROUND),
                                "Card-grid gaps and viewport match the workspace background");
                        check(
                                grid.getComponentCount() == 10,
                                "All ten options have cards, including Extended");
                        check(
                                grid.getComponent(0) == card(panel, Metamagic.CAREFUL),
                                "Unselected options start in alphabetical order");
                        layout(frame);
                        check(
                                ((GridLayout) grid.getLayout()).getColumns() == 3,
                                "Wide window uses three columns");
                        Container twinned = card(panel, Metamagic.TWINNED);
                        check(
                                findButton(twinned, "Hold Person") != null
                                        && findButton(twinned, "Chromatic Orb") == null,
                                "Twinned chips use audited eligibility");
                        JToggleButton subtle = choose(panel, Metamagic.SUBTLE);
                        subtle.doClick(0);
                        choose(panel, Metamagic.SEEKING).doClick(0);
                        check(
                                grid.getComponent(0) == card(panel, Metamagic.SEEKING)
                                        && grid.getComponent(1) == card(panel, Metamagic.SUBTLE)
                                        && grid.getComponent(2) == card(panel, Metamagic.CAREFUL),
                                "Selected options come first; both groups are alphabetical");
                        check(
                                subtle.isSelected() && subtle.getText().contains("EQUIPPED"),
                                "Equipped button reflects shared selection");
                        choose(panel, Metamagic.TWINNED).doClick(0);
                        check(
                                !choose(panel, Metamagic.TWINNED).isSelected(),
                                "Rejected third option resets its button");
                        subtle.doClick(0);
                        check(
                                !session.metamagic().contains(Metamagic.SUBTLE),
                                "Equipped button removes an option");
                        check(
                                grid.getComponent(0) == card(panel, Metamagic.SEEKING)
                                        && grid.getComponent(1) == card(panel, Metamagic.CAREFUL),
                                "Deselected options return to the alphabetical unselected group");
                        selection.choose(spell("Hold Person"), false);
                        check(
                                findButton(twinned, "Hold Person") == null,
                                "Removing a prepared spell updates chips immediately");
                        selection.choose(spell("Hold Person"), true);
                        check(
                                findButton(twinned, "Hold Person") != null,
                                "Adding a prepared spell updates chips immediately");
                        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC, Species.HUMAN);
                        check(
                                findButton(twinned, "Hold Person") == null,
                                "Dormant spells are excluded from prepared synergies");
                        check(
                                !choose(panel, Metamagic.TWINNED).isEnabled()
                                        && choose(panel, Metamagic.SEEKING).isEnabled(),
                                "Locked options remain removable after lowering level");
                        selection.setCharacter(5, SorcererRules.Subclass.WILD_MAGIC, Species.HUMAN);
                        findButton(twinned, "Hold Person").doClick(0);
                        check(
                                java.util.Arrays.stream(frame.getOwnedWindows())
                                        .anyMatch(
                                                w ->
                                                        w instanceof JDialog d
                                                                && AppStyle.windowContent(d)
                                                                        instanceof
                                                                        SpellDetailPanel),
                                "Spell chip opens full spell information");
                        for (Window dialog : frame.getOwnedWindows()) dialog.dispose();
                        AppStyle.appearance(true, 18);
                        AppStyle.applyAppearance(panel);
                        layout(frame);
                        check(
                                grid.getBackground().equals(AppStyle.BACKGROUND)
                                        && scroll.getViewport()
                                                .getBackground()
                                                .equals(AppStyle.BACKGROUND),
                                "Grid background follows theme changes");
                        check(
                                ((GridLayout) grid.getLayout()).getColumns() == 2,
                                "Larger text reduces the column count");
                        check(!subtle.isFocusPainted(), "Footer has no platform focus outline");
                        frame.setSize(440, 900);
                        layout(frame);
                        check(
                                ((GridLayout) grid.getLayout()).getColumns() == 1,
                                "Narrow window uses one column");
                        check(
                                scroll.getViewport().getViewSize().width
                                        == scroll.getViewport().getExtentSize().width,
                                "Grid never needs horizontal scrolling");
                        AppStyle.appearance(false, 14);
                        AppStyle.applyAppearance(panel);
                        session.choose(Metamagic.SUBTLE, true);
                        frame.setSize(1000, 900);
                        layout(frame);
                        BufferedImage image =
                                new BufferedImage(
                                        panel.getWidth(),
                                        panel.getHeight(),
                                        BufferedImage.TYPE_INT_RGB);
                        Graphics2D graphics = image.createGraphics();
                        panel.printAll(graphics);
                        graphics.dispose();
                        try {
                            ImageIO.write(
                                    image,
                                    "png",
                                    Path.of("target/metamagic-cards-preview.png").toFile());
                        } catch (Exception exception) {
                            throw new RuntimeException(exception);
                        }
                    } finally {
                        frame.dispose();
                        panel.dispose();
                    }
                });
        System.out.println(
                "Passed: all ten cards, limits, live prepared synergies, spell details, themes and"
                        + " responsive columns.");
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static JToggleButton choose(Container panel, Metamagic option) {
        return (JToggleButton) findAccessible(panel, "Choose " + option);
    }

    private static Container card(Container panel, Metamagic option) {
        return choose(panel, option).getParent().getParent();
    }

    private static AbstractButton findAccessible(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (child instanceof AbstractButton button
                    && name.equals(button.getAccessibleContext().getAccessibleName()))
                return button;
            if (child instanceof Container nested) {
                AbstractButton match = findAccessible(nested, name);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static JButton findButton(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton match = findButton(nested, text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents())
            if (child instanceof Container nested) layout(nested);
    }

    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
