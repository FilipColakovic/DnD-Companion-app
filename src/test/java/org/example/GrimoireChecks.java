package org.example;

import org.example.domain.character.CharacterState;
import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.ui.GrimoirePanel;
import org.example.ui.character.MetamagicDetailPanel;
import org.example.ui.character.MetamagicPanel;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.CastingPanel;
import org.example.ui.spells.SpellDetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/** Exercise dashboard controls through their production actions, with one shared character. */
public final class GrimoireChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel dashboard =
                            new GrimoirePanel(
                                    new CharacterState(
                                            3,
                                            17,
                                            SorcererRules.Subclass.ABERRANT,
                                            Set.of("Fire Bolt", "Shield", "Hold Person"),
                                            Species.HUMAN));
                    try {
                        navigation(dashboard);
                        casting(dashboard);
                        characterAndRests(dashboard);
                        metamagic(dashboard);
                        checkpointAndAppearance(dashboard);
                        compactLayout();
                        emptyCharacter();
                        if (!GraphicsEnvironment.isHeadless()) headfulDialogs();
                    } finally {
                        dashboard.dispose();
                    }
                });
        System.out.println(
                "Passed: dashboard navigation, live annotations, spell filters, casting, dice/log"
                    + " actions, rests, Metamagic, checkpoint restoration, appearance, and empty"
                    + " character.");
    }

    private static void navigation(GrimoirePanel dashboard) {
        check("Cast & Combat".equals(dashboard.currentPage()), "Dashboard opens on combat");
        for (String page :
                new String[] {"Metamagic", "Spell Library", "Cast & Combat", "Character"}) {
            if (!"Character".equals(page)) {
                AbstractButton navigation = named(dashboard, AbstractButton.class, "nav-" + page);
                check(navigation.isEnabled(), "Navigation is enabled: " + page);
                navigation.doClick(0);
            } else {
                named(dashboard, AbstractButton.class, "nav-" + GrimoirePanel.CHARACTER).doClick(0);
            }
            check(page.equals(dashboard.currentPage()), "Navigation opens " + page);
        }
        dashboard.showPage("Cast & Combat");
        check(label(dashboard, "sidebar-dc").contains("13"), "Header uses real save DC");
        check(label(dashboard, "sidebar-atk").contains("+5"), "Header uses real spell attack");
        check(
                label(dashboard, "sidebar-sp").replace(" ", "").contains("3/3"),
                "Header shows current points");
        check(label(dashboard, "sidebar-lineage").contains("3"), "Sidebar shows actual level");
        check(
                label(dashboard, "sidebar-lineage").contains("ABERRANT"),
                "Sidebar uses chosen subclass");
        check(
                named(dashboard, JComponent.class, "sidebar-dc").getToolTipText() != null,
                "DC annotation explains its calculation");
        named(dashboard, AbstractButton.class, "nav-" + GrimoirePanel.LIBRARY).doClick(0);
        check(
                "Spell Library".equals(dashboard.currentPage()),
                "Sidebar library opens the full selector");
        dashboard.showPage("Cast & Combat");
        named(dashboard, AbstractButton.class, "nav-" + GrimoirePanel.METAMAGIC).doClick(0);
        check("Metamagic".equals(dashboard.currentPage()), "Sidebar opens Metamagic choices");
        dashboard.showPage("Cast & Combat");
    }

    private static void casting(GrimoirePanel dashboard) {
        PlaySession play = dashboard.playSession();
        CastingPanel combat = dashboard.combatPanel();
        combat.clearLog();
        check(combat.selectSpell("Fire Bolt"), "Selected manual cantrip is available in combat");
        int all = combat.visibleSpellCount();
        button(combat, "Cantrips").doClick(0);
        check(
                combat.visibleSpellCount() > 0 && combat.visibleSpellCount() < all,
                "Cantrip filter reduces arsenal");
        int cantrips = combat.visibleSpellCount();
        button(combat, "Level 1").doClick(0);
        check(
                combat.visibleSpellCount() > cantrips,
                "Selected tiers combine instead of replacing each other");
        button(combat, "Cantrips").doClick(0);
        check(
                combat.visibleSpellCount()
                        == play.selection().catalog().stream()
                                .filter(play.selection()::selected)
                                .filter(play.selection()::available)
                                .filter(s -> s.level() == 1)
                                .count(),
                "Deselecting cantrips keeps level one selected");
        button(combat, "Level 1").doClick(0);
        check(
                combat.visibleSpellCount() == all,
                "Deselecting all tiers restores All automatically");
        buttonStartsWith(combat, "All").doClick(0);
        check(combat.visibleSpellCount() == all, "All filter restores arsenal");

        accessible(dashboard, JButton.class, "Toggle Innate Sorcery").doClick(0);
        check(
                play.innateActive() && label(dashboard, "sidebar-dc").contains("14"),
                "Combat Innate control updates active status and live save DC");
        accessible(dashboard, JButton.class, "Toggle Innate Sorcery").doClick(0);
        check(
                !play.innateActive() && label(dashboard, "sidebar-dc").contains("13"),
                "Combat Innate control ends the bonus");

        check(combat.selectSpell("Shield"), "Prepared level-one spell can be selected");
        button(combat, "Cast").doClick(0);
        check(
                play.remainingSlots(1) == 3 && combat.combatLogText().contains("Shield"),
                "Casting spends selected slot and records real spell");
        var before = play.snapshot();
        button(combat, "Cast").doClick(0);
        check(
                play.remainingSlots(1) == 2,
                "Repeated casts spend another slot without a turn reset");
        check(combat.selectSpell("Hold Person"), "Second-level spell can be selected");
        button(combat, "Cast").doClick(0);
        check(
                play.remainingSlots(2) == 1 && play.concentration().equals("Hold Person"),
                "Concentration cast uses second-level slot");
        check(play.concentration().equals("Hold Person"), "Casting starts concentration");
        play.endConcentration();
        check(play.concentration().isEmpty(), "Drop concentration clears the shared effect");

        check(combat.selectSpell("Fire Bolt"), "Cantrip selection returns for roll checks");
        button(combat, "Quick roll damage").doClick(0);
        check(
                combat.combatLogText().contains("d10"),
                "Damage roll uses Fire Bolt's die instead of fixed d6");
        button(combat, "Crit \u00d72 dice").doClick(0);
        check(combat.combatLogText().contains("2d10"), "Critical attack roll doubles damage dice");
        check(
                combat.visibleDiceSides().equals(java.util.List.of(10, 20)),
                "Fire Bolt shows only its damage and attack dice");
        for (String die : new String[] {"d10", "d20"}) {
            String log = combat.combatLogText();
            button(combat, die).doClick(0);
            check(!log.equals(combat.combatLogText()), "Dice tray rolls " + die);
        }
        check(
                CastingPanel.saveSucceeds(1, 20, 15) && !CastingPanel.saveSucceeds(20, -10, 15),
                "Target saves compare totals without automatic natural-one/twenty outcomes");
        button(combat, "Clear").doClick(0);
        check(combat.combatLogText().isBlank(), "Clear removes all combat log entries");
    }

    private static void characterAndRests(GrimoirePanel dashboard) {
        dashboard.showPage("Character");
        JSlider level = first(dashboard.characterPanel(), JSlider.class);
        level.setValue(5);
        check(
                label(dashboard, "sidebar-lineage").contains("5")
                        && label(dashboard, "sidebar-dc").contains("14"),
                "Level change refreshes sidebar and header calculations");
        dashboard.characterPanel().selectSubclass(SorcererRules.Subclass.WILD_MAGIC);
        dashboard.characterPanel().selectSpecies(Species.WOOD_ELF);
        check(
                label(dashboard, "sidebar-lineage").contains("WILD"),
                "Subclass annotation refreshes live");
        dashboard.characterPanel().setCharacterName("Sylphira");
        check(
                "Sylphira".equals(dashboard.characterPanel().characterName()),
                "Profile edits character name");
        dashboard.showPage("Cast & Combat");
        PlaySession play = dashboard.playSession();
        play.setRemainingPoints(0);
        play.setRemainingSlots(1, 0);
        button(dashboard, "Short Rest").doClick(0);
        check(
                play.remainingPoints() == 2 && play.remainingSlots(1) == 0,
                "Short Rest recovers half level in points while preserving slots");
        check(
                label(dashboard, "sidebar-sp").replace(" ", "").contains("2/5"),
                "Rest updates header points");
        play.setRemainingPoints(0);
        button(dashboard, "Short Rest").doClick(0);
        check(play.remainingPoints() == 0, "Second Short Rest does not repeat Restoration");
        button(dashboard, "Long Rest").doClick(0);
        check(
                play.remainingPoints() == 5 && play.remainingSlots(1) == 4,
                "Long Rest restores points and slots");
        accessible(dashboard, JButton.class, "Toggle Innate Sorcery").doClick(0);
        check(play.innateActive(), "Header Innate button activates shared feature");
        accessible(dashboard, JButton.class, "Toggle Innate Sorcery").doClick(0);
        check(!play.innateActive(), "Header Innate button ends shared feature");
    }

    private static void metamagic(GrimoirePanel dashboard) {
        dashboard.showPage("Metamagic");
        MetamagicPanel picker = first(dashboard, MetamagicPanel.class);
        accessible(picker, JToggleButton.class, "Choose " + Metamagic.SUBTLE).doClick(0);
        accessible(picker, JToggleButton.class, "Choose " + Metamagic.QUICKENED).doClick(0);
        check(
                dashboard
                        .playSession()
                        .metamagic()
                        .containsAll(Set.of(Metamagic.SUBTLE, Metamagic.QUICKENED)),
                "Hub choices update shared Metamagic selection");
        accessible(picker, JToggleButton.class, "Choose " + Metamagic.HEIGHTENED).doClick(0);
        check(
                !dashboard.playSession().metamagic().contains(Metamagic.HEIGHTENED),
                "Hub enforces current-level choice limit");
        MetamagicDetailPanel detail =
                new MetamagicDetailPanel(Metamagic.QUICKENED, dashboard.playSession());
        try {
            check(!detail.isSpellListExpanded(), "Compatible spell list starts collapsed");
            buttonStartsWith(detail, "Show compatible spells").doClick(0);
            check(detail.isSpellListExpanded(), "Compatible spells expand through their control");
            buttonStartsWith(detail, "Hide compatible spells").doClick(0);
            check(!detail.isSpellListExpanded(), "Compatible spells collapse again");
        } finally {
            detail.dispose();
        }
        dashboard.showPage("Cast & Combat");
        CastingPanel combat = dashboard.combatPanel();
        check(
                combat.selectSpell("Fire Bolt"),
                "Combat retains access to selected cantrip after navigation");
        check(
                combat.compatibleChosen(combat.selectedSpell()).contains(Metamagic.QUICKENED),
                "Combat compatibility follows chosen Metamagic");
    }

    private static void checkpointAndAppearance(GrimoirePanel dashboard) {
        AtomicReference<CharacterState> checkpoint = new AtomicReference<>();
        dashboard.setSaveActions(
                locked -> {
                    if (locked) {
                        checkpoint.set(dashboard.snapshot());
                        dashboard.markSaved();
                    }
                },
                () -> dashboard.restore(checkpoint.get()));
        named(dashboard, AbstractButton.class, "save-lock").doClick(0);
        check(
                dashboard.isSaveLocked() && checkpoint.get() != null,
                "Save-lock action commits a checkpoint");
        CharacterState saved = checkpoint.get();
        dashboard.playSession().setRemainingSlots(1, 0);
        dashboard.characterPanel().setCharacterName("Experiment");
        boolean initialDark = dashboard.playSession().snapshot().dark();
        named(dashboard, AbstractButton.class, "dark-mode").doClick(0);
        check(
                dashboard.playSession().snapshot().dark() != initialDark,
                "Theme control updates persisted display preference");
        named(dashboard, JComboBox.class, "text-size").setSelectedItem(18);
        check(
                dashboard.playSession().snapshot().textSize() == 18,
                "Text-size control updates display preference");
        Font enlarged = named(dashboard, JLabel.class, "sidebar-dc").getFont();
        AppStyle.applyAppearance(dashboard);
        AppStyle.applyAppearance(dashboard);
        check(
                enlarged.equals(named(dashboard, JLabel.class, "sidebar-dc").getFont()),
                "Repeated live updates do not keep enlarging fonts");
        check(
                named(dashboard, JLabel.class, "sidebar-dc").getForeground().equals(AppStyle.ACCENT)
                        && named(dashboard, JLabel.class, "sidebar-designation")
                                .getForeground()
                                .equals(AppStyle.INK),
                "Appearance updates preserve annotation color roles");
        check(
                dashboard.characterPanel().saveStatusText().contains("Experiment"),
                "Protected experiments remain tracked");
        named(dashboard, AbstractButton.class, "restore-saved").doClick(0);
        check(
                dashboard.snapshot().equals(saved) && dashboard.isSaveLocked(),
                "Restore returns character, resources, choices, name and display settings,"
                    + " preserving protection");
        dashboard.playSession().appearance(true, 14);
        preview(dashboard, "target/grimoire-dark-preview.png");
        Color dark = dashboard.getBackground();
        dashboard.playSession().appearance(false, 14);
        preview(dashboard, "target/grimoire-light-preview.png");
        Color light = dashboard.getBackground();
        check(
                light.getRed() > dark.getRed() && !AppStyle.isDark(),
                "Light theme changes the dashboard surface");
        dashboard.restore(saved);
    }

    private static void emptyCharacter() {
        GrimoirePanel empty =
                new GrimoirePanel(
                        new CharacterState(
                                1, 17, SorcererRules.Subclass.WILD_MAGIC, Set.of(), Species.HUMAN));
        try {
            check(
                    empty.combatPanel().selectedSpell() == null
                            && empty.combatPanel().visibleSpellCount() == 0,
                    "New character has no fabricated prepared spells");
            check(!button(empty.combatPanel(), "Cast").isEnabled(), "Empty arsenal cannot cast");
            button(empty.combatPanel(), "Choose spells").doClick(0);
            check(
                    "Spell Library".equals(empty.currentPage()),
                    "Empty-character action opens spell selection");
            var library =
                    require(
                            find(
                                    empty,
                                    org.example.ui.spells.SpellLibraryPanel.class,
                                    c -> visibleWithin(c, empty)),
                            "Visible spell library");
            var search =
                    require(
                            find(
                                    library,
                                    JTextField.class,
                                    c ->
                                            "Search library spells"
                                                    .equals(
                                                            c.getAccessibleContext()
                                                                    .getAccessibleName())),
                            "Library search");
            search.setText("Fire Bolt");
            accessible(library, JButton.class, "Choose Fire Bolt").doClick(0);
            empty.showPage("Cast & Combat");
            check(
                    empty.combatPanel().selectSpell("Fire Bolt")
                            && button(empty.combatPanel(), "Cast").isEnabled(),
                    "Library selection immediately enables actual combat casting");
        } finally {
            empty.dispose();
        }
    }

    private static void compactLayout() {
        GrimoirePanel compact =
                new GrimoirePanel(
                        new CharacterState(
                                20,
                                20,
                                SorcererRules.Subclass.ABERRANT,
                                Set.of("Fire Bolt", "Fireball"),
                                Species.HUMAN));
        try {
            compact.playSession().appearance(true, 18);
            preview(compact, "target/grimoire-compact-preview.png", 1100, 780);
            StringBuilder problems = new StringBuilder();
            for (int tier = 1; tier <= 9; tier++) {
                JLabel badge = named(compact, JLabel.class, "sidebar-slot-" + tier);
                if (!fullyContained(badge, compact))
                    problems.append("slot ").append(tier).append(" is outside an ancestor; ");
                if (badge.getPreferredSize().width > badge.getWidth())
                    problems.append("slot ")
                            .append(tier)
                            .append(" text clipped: ")
                            .append(badge.getWidth())
                            .append(" < ")
                            .append(badge.getPreferredSize().width)
                            .append("; ");
            }
            for (String page :
                    new String[] {"Cast & Combat", "Metamagic", "Spell Library", "Character"}) {
                AbstractButton nav = named(compact, AbstractButton.class, "nav-" + page);
                if (nav.getPreferredSize().width > nav.getWidth())
                    problems.append(page)
                            .append(" clipped: ")
                            .append(nav.getWidth())
                            .append(" < ")
                            .append(nav.getPreferredSize().width)
                            .append("; ");
                if (!fullyContained(nav, compact))
                    problems.append(page).append(" is outside an ancestor; ");
            }
            Container sidebar =
                    named(compact, AbstractButton.class, "nav-Cast & Combat")
                            .getParent()
                            .getParent()
                            .getParent();
            if (sidebar.getLayout() instanceof BorderLayout border) {
                Component top = border.getLayoutComponent(BorderLayout.NORTH),
                        bottom = border.getLayoutComponent(BorderLayout.SOUTH);
                if (top != null && bottom != null && top.getBounds().intersects(bottom.getBounds()))
                    problems.append("sidebar navigation and preferences overlap; ");
            }
            check(problems.isEmpty(), "Level-20, 18-point compact layout: " + problems);
        } finally {
            compact.dispose();
        }
    }

    private static boolean fullyContained(Component child, Container root) {
        Rectangle bounds = new Rectangle(0, 0, child.getWidth(), child.getHeight());
        for (Component cursor = child; cursor != root; cursor = cursor.getParent()) {
            Container parent = cursor.getParent();
            if (parent == null) return false;
            bounds = SwingUtilities.convertRectangle(cursor, bounds, parent);
            if (bounds.x < 0
                    || bounds.y < 0
                    || bounds.x + bounds.width > parent.getWidth()
                    || bounds.y + bounds.height > parent.getHeight()) return false;
        }
        return true;
    }

    private static void headfulDialogs() {
        GrimoirePanel dashboard =
                new GrimoirePanel(
                        new CharacterState(
                                3,
                                17,
                                SorcererRules.Subclass.ABERRANT,
                                Set.of("Fire Bolt"),
                                Species.HUMAN));
        JFrame frame = new JFrame("Grimoire integration check");
        try {
            frame.setContentPane(dashboard);
            frame.setSize(1480, 960);
            frame.setVisible(true);
            check(
                    dashboard.combatPanel().selectSpell("Mind Sliver"),
                    "Automatic subclass spell is selectable");
            dashboard.showPage("Metamagic");
            MetamagicPanel picker = first(dashboard, MetamagicPanel.class);
            accessible(picker, JButton.class, "Details for " + Metamagic.QUICKENED).doClick(0);
            JDialog metamagic =
                    java.util.Arrays.stream(frame.getOwnedWindows())
                            .filter(
                                    w ->
                                            w instanceof JDialog d
                                                    && AppStyle.windowContent(d)
                                                            instanceof MetamagicDetailPanel)
                            .map(w -> (JDialog) w)
                            .findFirst()
                            .orElseThrow();
            MetamagicDetailPanel detail = (MetamagicDetailPanel) AppStyle.windowContent(metamagic);
            check(
                    !detail.isSpellListExpanded(),
                    "Real Metamagic detail window opens with collapsed spells");
            buttonStartsWith(detail, "Show compatible spells").doClick(0);
            AbstractButton spell =
                    require(
                            find(
                                    detail,
                                    AbstractButton.class,
                                    c ->
                                            ((AbstractButton) c).getText() != null
                                                    && ((AbstractButton) c)
                                                            .getText()
                                                            .contains("Fire Bolt")),
                            "Compatible Fire Bolt action");
            spell.doClick(0);
            check(
                    java.util.Arrays.stream(metamagic.getOwnedWindows())
                            .anyMatch(
                                    w ->
                                            w instanceof JDialog d
                                                    && d.isVisible()
                                                    && AppStyle.windowContent(d)
                                                            instanceof SpellDetailPanel),
                    "Clicking compatible spell opens its full information");
        } finally {
            frame.dispose();
            dashboard.dispose();
        }
    }

    private static String label(Container root, String name) {
        return named(root, JLabel.class, name).getText();
    }

    private static AbstractButton button(Container root, String text) {
        return require(
                find(root, AbstractButton.class, c -> text.equals(((AbstractButton) c).getText())),
                "Button: " + text);
    }

    private static AbstractButton buttonStartsWith(Container root, String text) {
        return require(
                find(
                        root,
                        AbstractButton.class,
                        c ->
                                ((AbstractButton) c).getText() != null
                                        && ((AbstractButton) c).getText().startsWith(text)),
                "Button prefix: " + text);
    }

    private static <T extends Component> T named(Container root, Class<T> type, String name) {
        return require(find(root, type, c -> name.equals(c.getName())), "Named control: " + name);
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        return require(
                find(
                        root,
                        type,
                        c ->
                                c.getAccessibleContext() != null
                                        && name.equals(
                                                c.getAccessibleContext().getAccessibleName())),
                "Accessible control: " + name);
    }

    private static <T extends Component> T first(Container root, Class<T> type) {
        return require(find(root, type, c -> true), "Component: " + type.getSimpleName());
    }

    private static boolean visibleWithin(Component child, Container root) {
        for (Component cursor = child; cursor != root; cursor = cursor.getParent()) {
            if (cursor == null || !cursor.isVisible()) return false;
        }
        return true;
    }

    private static <T extends Component> T find(
            Container root, Class<T> type, java.util.function.Predicate<Component> matches) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child) && matches.test(child)) return type.cast(child);
            if (child instanceof Container nested) {
                T found = find(nested, type, matches);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T> T require(T value, String message) {
        if (value == null) throw new AssertionError("Missing " + message);
        return value;
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents())
            if (child instanceof Container nested) layout(nested);
    }

    private static void preview(JComponent dashboard, String path) {
        preview(dashboard, path, 1480, 960);
    }

    private static void preview(JComponent dashboard, String path, int width, int height) {
        dashboard.setSize(width, height);
        layout(dashboard);
        BufferedImage image =
                new BufferedImage(
                        dashboard.getWidth(), dashboard.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        dashboard.printAll(graphics);
        graphics.dispose();
        try {
            Files.createDirectories(Path.of(path).getParent());
            ImageIO.write(image, "png", Path.of(path).toFile());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }
}
