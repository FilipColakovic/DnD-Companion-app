package dndcompanion.ui.character;

import dndcompanion.domain.character.BackgroundCatalog;
import dndcompanion.domain.character.CharacterChoices;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Skill;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.PlayState;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

/**
 * PHB backgrounds, independently owned grants, Character reminders and the sidebar decision marker.
 */
public final class BackgroundChecks {
    public static void main(String[] args) throws Exception {
        check(BackgroundCatalog.all().size() == 16, "All sixteen PHB backgrounds");
        for (var background : BackgroundCatalog.all()) {
            check(
                    background.abilities().size() == 3 && background.skills().size() == 2,
                    "Background has three ability options and two fixed skills");
            check(
                    FeatCatalog.named(background.feat()).category() == FeatCatalog.Category.ORIGIN,
                    "Background grants an Origin feat");
            check(
                    CharacterChoices.backgroundChoices(background, CharacterSheet.defaults())
                                    .stream()
                                    .filter(choice -> choice.id().endsWith(".abilities"))
                                    .findFirst()
                                    .orElseThrow()
                                    .options()
                                    .size()
                            == 7,
                    "Seven legal +2/+1 or +1/+1/+1 allocations");
        }
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            4,
                                            17,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            Set.of(),
                                            Species.HUMAN));
                    JFrame frame = new JFrame();
                    frame.setContentPane(workspace);
                    frame.setSize(1480, 1000);
                    frame.setVisible(true);
                    try {
                        PlaySession session = workspace.playSession();
                        JLabel marker = (JLabel) named(workspace, "pending-character-decisions");
                        check(
                                marker.isVisible() && marker.getText().equals("!"),
                                "Sidebar flags outstanding decisions");
                        complete(session);
                        check(
                                !marker.isVisible(),
                                "Completing all active choices hides the marker");
                        workspace.showBackgrounds();
                        layout(frame);
                        check(
                                accessible(workspace, JToggleButton.class, "Catalog Backgrounds")
                                        .isSelected(),
                                "Backgrounds are available inside Feats");
                        accessible(workspace, JButton.class, "Choose background Noble").doClick(0);
                        check(
                                BackgroundCatalog.selected(session.sheet())
                                        .orElseThrow()
                                        .name()
                                        .equals("Noble"),
                                "Card selects a background");
                        check(
                                session.feats().containsKey("Skilled")
                                        && session.grantedSkills()
                                                .containsAll(
                                                        Set.of(Skill.HISTORY, Skill.PERSUASION)),
                                "Noble grants its feat and fixed skills");
                        check(
                                marker.isVisible(),
                                "Selecting an unconfigured background restores the red marker");
                        layout(frame);
                        layout(frame);
                        check(
                                marker.getWidth() > 0 && marker.getHeight() > 0,
                                "Visible marker occupies space beside Character");
                        preview(workspace, "target/backgrounds-pending.png");
                        accessible(workspace, JButton.class, "Select background Noble").doClick(0);
                        check(
                                BackgroundCatalog.selected(session.sheet()).isEmpty(),
                                "Inspector removes the current background");
                        accessible(workspace, JButton.class, "Choose background Noble").doClick(0);
                        button(workspace, "Complete choices on Character").doClick(0);
                        check(
                                workspace.currentPage().equals(GrimoirePanel.CHARACTER),
                                "Inspector sends decisions to Character");
                        check(
                                named(workspace, "choice-reminder-background.Noble.feat.Skilled")
                                        != null,
                                "Skilled skill choices are on Character");
                        complete(session);
                        check(!marker.isVisible(), "Confirmed background choices clear the marker");
                        check(
                                session.sheet()
                                                .skillBonus(
                                                        Skill.HISTORY,
                                                        17,
                                                        2,
                                                        session.grantedSkills())
                                        == 2,
                                "Background skills calculate proficiency without stacking");
                        session.equipFeat(
                                FeatCatalog.named("Skilled"), "Background feat notes", 17, true);
                        check(
                                !session.snapshot().feats().containsKey("Skilled"),
                                "Editing background-feat notes does not create an unrelated manual"
                                        + " feat");
                        session.equipFeat(
                                FeatCatalog.named("War Caster"), "Manual milestone", 17, true);
                        check(marker.isVisible(), "War Caster requires an ability choice");
                        session.setSheet(
                                session.sheet()
                                        .withChoice("feat.War Caster.ability", List.of("CHA")));
                        var noble = workspace.snapshot();
                        check(
                                PlayState.decode(noble.play().encode()).equals(noble.play()),
                                "Background and decisions round-trip in play-state properties");
                        session.selectBackground(BackgroundCatalog.named("Sage"));
                        check(
                                !session.feats().containsKey("Skilled")
                                        && session.feats().containsKey("War Caster"),
                                "Background replacement deactivates only its own feat");
                        check(
                                session.requiredChoices().stream()
                                        .anyMatch(
                                                choice ->
                                                        choice.id()
                                                                .equals(
                                                                        "background.Sage.feat.Magic"
                                                                            + " Initiate.cantrips")),
                                "Background Magic Initiate creates scoped spell choices");
                        check(
                                session.requiredChoices().stream()
                                        .noneMatch(
                                                choice ->
                                                        choice.id()
                                                                .equals(
                                                                        "background.Sage.feat.Magic"
                                                                            + " Initiate.list")),
                                "Sage fixes the Wizard list");
                        session.setSheet(
                                session.sheet()
                                        .withChoice("human.feat", List.of("Skilled"))
                                        .withChoice(
                                                "feat.Skilled",
                                                List.of("Stealth", "Survival", "Insight")));
                        session.selectBackground(BackgroundCatalog.named("Noble"));
                        session.setSheet(
                                session.sheet()
                                        .withChoice(
                                                "background.Noble.feat.Skilled",
                                                List.of("Acrobatics", "Athletics", "Medicine")));
                        check(
                                session.grantedSkills()
                                        .containsAll(
                                                Set.of(
                                                        Skill.STEALTH,
                                                        Skill.SURVIVAL,
                                                        Skill.INSIGHT,
                                                        Skill.ACROBATICS,
                                                        Skill.ATHLETICS,
                                                        Skill.MEDICINE)),
                                "Human and background Skilled instances have separate selections");
                        session.selectBackground(BackgroundCatalog.named("Sage"));
                        check(
                                session.grantedSkills().contains(Skill.STEALTH)
                                        && !session.grantedSkills().contains(Skill.MEDICINE),
                                "Replacing a background preserves Human Skilled choices");
                        session.equipFeat(FeatCatalog.named("Skill Expert"), "", 17, true);
                        workspace.showPage(GrimoirePanel.CHARACTER);
                        check(
                                named(workspace, "choice-reminder-feat.Skill Expert.skill") != null,
                                "Skill Expert decisions appear on Character");
                        accessible(
                                        workspace,
                                        JCheckBox.class,
                                        "feat.Skill Expert.skill option Performance")
                                .doClick(0);
                        accessible(workspace, JButton.class, "Confirm feat.Skill Expert.skill")
                                .doClick(0);
                        accessible(
                                        workspace,
                                        JCheckBox.class,
                                        "feat.Skill Expert.expertise option Performance")
                                .doClick(0);
                        accessible(workspace, JButton.class, "Confirm feat.Skill Expert.expertise")
                                .doClick(0);
                        check(
                                session.skillBonus(Skill.PERFORMANCE, 17) == 8,
                                "Selected expertise doubles proficiency and uses War Caster's"
                                        + " effective CHA");
                        session.removeFeat("Skill Expert");
                        check(
                                session.skillBonus(Skill.PERFORMANCE, 17) == 4,
                                "Removing the feat removes its own skill and expertise grants");
                        session.equipFeat(FeatCatalog.named("Keen Mind"), "", 17, true);
                        accessible(workspace, JCheckBox.class, "feat.Keen Mind option Arcana")
                                .doClick(0);
                        accessible(workspace, JButton.class, "Confirm feat.Keen Mind").doClick(0);
                        check(
                                session.skillTier(Skill.ARCANA) == 2,
                                "Keen Mind promotes an existing proficiency");
                        session.equipFeat(FeatCatalog.named("Observant"), "", 17, true);
                        accessible(workspace, JCheckBox.class, "feat.Observant option Perception")
                                .doClick(0);
                        accessible(workspace, JButton.class, "Confirm feat.Observant").doClick(0);
                        check(
                                session.skillTier(Skill.PERCEPTION) == 1,
                                "Observant grants proficiency when untrained");
                        workspace.restore(noble);
                        check(
                                !marker.isVisible() && !session.feats().containsKey("Keen Mind"),
                                "Restoration updates the marker and removes experimental"
                                        + " decisions");
                        workspace.showBackgrounds();
                        JTextField search =
                                accessible(workspace, JTextField.class, "Search backgrounds");
                        search.setText("Musician");
                        check(
                                accessible(workspace, JPanel.class, "Background card Entertainer")
                                                != null
                                        && accessible(
                                                        workspace,
                                                        JPanel.class,
                                                        "Background card Noble")
                                                == null,
                                "Background search filters by Origin feat");
                        search.setText("");
                        for (boolean dark : new boolean[] {false, true})
                            for (int size : new int[] {14, 18}) {
                                session.appearance(dark, size);
                                layout(frame);
                                layout(frame);
                                preview(
                                        workspace,
                                        "target/backgrounds-"
                                                + (dark ? "dark" : "light")
                                                + "-"
                                                + size
                                                + ".png");
                            }
                        frame.setSize(980, 900);
                        layout(frame);
                        layout(frame);
                        preview(workspace, "target/backgrounds-narrow.png");
                    } finally {
                        frame.dispose();
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: sixteen PHB backgrounds, scoped grants and decisions, skill-selecting"
                        + " feats, live sidebar marker, persistence, restore, search and themes.");
    }

    private static void complete(PlaySession session) {
        for (int pass = 0; pass < 5 && session.pendingDecisions() > 0; pass++)
            for (var choice : session.requiredChoices())
                if (!choice.complete(session.sheet()) && choice.options().size() >= choice.count())
                    session.setSheet(
                            session.sheet()
                                    .withChoice(
                                            choice.id(),
                                            choice.options().subList(0, choice.count())));
    }

    private static void layout(Container container) {
        container.doLayout();
        for (Component child : container.getComponents())
            if (child instanceof Container nested) layout(nested);
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

    private static JButton button(Container root, String text) {
        for (Component child : root.getComponents()) {
            if (child instanceof JButton button && text.equals(button.getText())) return button;
            if (child instanceof Container nested) {
                JButton found = button(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)
                    && name.equals(child.getAccessibleContext().getAccessibleName()))
                return type.cast(child);
            if (child instanceof Container nested) {
                T found = accessible(nested, type, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void preview(JComponent panel, String path) {
        BufferedImage image =
                new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        panel.printAll(g);
        g.dispose();
        try {
            ImageIO.write(image, "png", Path.of(path).toFile());
        } catch (Exception error) {
            throw new RuntimeException(error);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
