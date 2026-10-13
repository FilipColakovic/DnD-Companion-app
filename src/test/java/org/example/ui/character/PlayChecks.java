package org.example.ui.character;

import org.example.domain.character.CharacterState;
import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.PlayState;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellSelection;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.CastingPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public final class PlayChecks {
    private static void check(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static PlaySession session(int level) {
        SpellSelection selection = new SpellSelection();
        selection.setCharacter(level, SorcererRules.Subclass.WILD_MAGIC, Species.INFERNAL_TIEFLING);
        selection.restoreChoices(
                Set.of(
                        "Fire Bolt",
                        "True Strike",
                        "Fireball",
                        "Hold Person",
                        "Shield",
                        "Charm Person"));
        return new PlaySession(selection);
    }

    public static void main(String[] args) throws Exception {
        check(
                Metamagic.limit(1) == 0
                        && Metamagic.limit(2) == 2
                        && Metamagic.limit(10) == 4
                        && Metamagic.limit(17) == 6,
                "2024 Metamagic limits");
        PlaySession play = session(5);
        check(
                play.choose(Metamagic.QUICKENED, true).isEmpty()
                        && play.choose(Metamagic.HEIGHTENED, true).isEmpty(),
                "Select two options");
        check(!play.choose(Metamagic.SUBTLE, true).isEmpty(), "Reject over-limit option");
        check(
                play.cast(spell("Fire Bolt"), 0, false, Set.of(Metamagic.QUICKENED)).isEmpty(),
                "Quickened cantrip");
        PlayState before = play.snapshot();
        check(
                play.cast(spell("Fireball"), 3, false, Set.of()).isEmpty()
                        && play.remainingSlots(3) == 1,
                "Casting after Quickened requires no turn reset");
        check(
                play.cast(spell("Hold Person"), 2, false, Set.of(Metamagic.HEIGHTENED)).isEmpty(),
                "Spend slot and Metamagic points");
        check(
                play.remainingPoints() == 1
                        && play.remainingSlots(2) == 2
                        && play.concentration().equals("Hold Person"),
                "Shared resources and concentration");
        before = play.snapshot();
        check(
                play.cast(spell("Shield"), 1, false, Set.of()).isEmpty()
                        && play.remainingSlots(1) == 3,
                "Consecutive casts spend available slots without a turn check");
        play.shortRest();
        check(
                play.remainingPoints() == 3
                        && play.remainingSlots(2) == 2
                        && play.concentration().equals("Hold Person"),
                "Short Rest restores half level once; retains slots and concentration");
        play.setRemainingPoints(0);
        play.shortRest();
        check(play.remainingPoints() == 0, "Restoration once per Long Rest");
        play.longRest();
        check(
                play.remainingPoints() == 5
                        && play.remainingSlots(2) == 3
                        && play.concentration().isEmpty()
                        && play.metamagic().size() == 2,
                "Long Rest restores resources, retains choices");
        play.setRemainingPoints(0);
        play.shortRest();
        check(play.remainingPoints() == 2, "Long Rest resets Restoration");
        PlaySession low = session(4);
        low.setRemainingPoints(0);
        low.shortRest();
        check(low.remainingPoints() == 0, "No Sorcerous Restoration below 5");
        play.longRest();
        check(play.freeCasts(spell("Hellish Rebuke")) == 1, "Species daily cast");
        check(
                play.cast(spell("Hellish Rebuke"), 1, true, Set.of()).isEmpty()
                        && play.remainingSlots(1) == 4
                        && play.freeCasts(spell("Hellish Rebuke")) == 0,
                "Species cast consumes daily use without slot");
        check(
                play.cast(spell("Shield"), 1, false, Set.of()).isEmpty(),
                "Free cast permits a slotted spell in same turn when actions permit");
        before = play.snapshot();
        check(
                !play.cast(spell("Hellish Rebuke"), 1, true, Set.of()).isEmpty()
                        && before.equals(play.snapshot()),
                "Cannot overspend free casts");
        play.longRest();
        check(play.freeCasts(spell("Hellish Rebuke")) == 1, "Rest restores species cast");
        PlaySession high = session(7);
        high.choose(Metamagic.QUICKENED, true);
        high.choose(Metamagic.HEIGHTENED, true);
        check(
                !high.cast(
                                spell("Fireball"),
                                3,
                                false,
                                Set.of(Metamagic.QUICKENED, Metamagic.HEIGHTENED))
                        .isEmpty(),
                "Two ordinary options require active Sorcery Incarnate");
        high.toggleInnate();
        check(
                high.cast(
                                spell("Fireball"),
                                3,
                                false,
                                Set.of(Metamagic.QUICKENED, Metamagic.HEIGHTENED))
                        .isEmpty(),
                "Level 7 Innate permits two options");
        high.toggleInnate();
        high.toggleInnate();
        high.toggleInnate();
        check(
                high.toggleInnate().isEmpty() && high.remainingPoints() == 1,
                "After two daily uses, level 7 Innate costs 2 points");
        PlaySession cap = session(20);
        cap.choose(Metamagic.QUICKENED, true);
        cap.toggleInnate();
        check(
                cap.cast(spell("Fire Bolt"), 0, false, Set.of(Metamagic.QUICKENED)).isEmpty()
                        && cap.remainingPoints() == 20,
                "Level 20 free Metamagic while Innate active");
        check(
                cap.metamagicCost(Set.of(Metamagic.QUICKENED)) == 0,
                "Level 20 free option is assessed independently for each casting");
        check(
                cap.cast(spell("Fire Bolt"), 0, false, Set.of(Metamagic.QUICKENED)).isEmpty(),
                "Repeated Metamagic casts need no turn reset");
        check(
                Metamagic.TWINNED.applies(spell("Hold Person"))
                        && !Metamagic.TWINNED.applies(spell("Fireball")),
                "2024 Twinned adds creature targets, not damage");
        check(
                Metamagic.HEIGHTENED.applies(spell("Mind Sliver")),
                "Heightened recognizes ability-save summary wording");
        check(
                !Metamagic.TRANSMUTED.applies(spell("Protection from Energy"))
                        && Metamagic.TRANSMUTED.applies(spell("Fireball")),
                "Transmuted requires elemental damage");
        check(
                !Metamagic.SEEKING.applies(spell("Summon Construct"))
                        && Metamagic.SEEKING.applies(spell("Fire Bolt")),
                "Seeking uses caster's attack");
        check(
                CastingPanel.damageSummary(spell("Fireball"), 5, 9).contains("10d6"),
                "Upcast Fireball damage");
        check(
                CastingPanel.damageSummary(spell("Fire Bolt"), 0, 17).contains("4d10"),
                "Cantrip damage progression");
        check(
                !CastingPanel.damageSummary(spell("True Strike"), 0, 1).contains("d6"),
                "True Strike no extra die below level 5");
        play.appearance(true, 18);
        check(
                PlayState.decode(play.snapshot().encode()).equals(play.snapshot()),
                "Complete play-state serialization");
        check(
                PlayState.decode(
                                play.snapshot().encode()
                                        + "\n"
                                        + "slotCast=true\n"
                                        + "leveledCast=true\n"
                                        + "quickened=true\n"
                                        + "freeMeta=true\n")
                        .equals(play.snapshot()),
                "Former turn flags in older saves are ignored without changing resources");
        check(
                !play.snapshot().encode().contains("slotCast=")
                        && !play.snapshot().encode().contains("freeMeta="),
                "New saves omit removed turn state");
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            5,
                                            17,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            Set.of("Fireball"),
                                            Species.HUMAN));
                    var panel = workspace.characterController();
                    panel.playSession().choose(Metamagic.QUICKENED, true);
                    MetamagicDetailPanel detail =
                            new MetamagicDetailPanel(Metamagic.QUICKENED, panel.playSession());
                    check(!detail.isSpellListExpanded(), "Compatible spell list starts collapsed");
                    button(detail, "Show compatible spells ▸").doClick(0);
                    check(detail.isSpellListExpanded(), "Compatible list expands");
                    detail.dispose();
                    CharacterState saved = panel.snapshot();
                    panel.setSaveLocked(true);
                    panel.markSaved();
                    panel.playSession().setRemainingSlots(3, 0);
                    panel.playSession().appearance(true, 18);
                    check(
                            panel.snapshot().play().dark()
                                    && panel.saveStatusText()
                                            .equals("Experimenting · save protected"),
                            "Save status includes resource and theme experiments");
                    preview(workspace, "target/play-dark-preview.png", 1000);
                    workspace.restore(saved);
                    check(
                            panel.snapshot().equals(saved)
                                    && AppStyle.isDark() == saved.play().dark(),
                            "Restore includes resources, Metamagic and appearance");
                    panel.playSession().appearance(false, 14);
                    preview(workspace, "target/play-light-preview.png", 920);
                    CastingPanel casting = new CastingPanel(panel.playSession(), () -> 17);
                    AppStyle.applyAppearance(casting);
                    preview(casting, "target/casting-preview.png", 960);
                    casting.dispose();
                    MetamagicPanel picker = new MetamagicPanel(panel.playSession());
                    preview(picker, "target/metamagic-preview.png", 960);
                    picker.dispose();
                    workspace.restore(
                            new CharacterState(
                                    20,
                                    17,
                                    SorcererRules.Subclass.ABERRANT,
                                    Set.of(),
                                    Species.HUMAN));
                    preview(workspace, "target/play-level20-preview.png", 1000);
                });
        System.out.println(
                "Passed: resource/rest/casting rules, Metamagic eligibility and limits, state"
                        + " serialization, collapsed list, appearance and checkpoint restoration.");
    }

    private static boolean hasText(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && text.equals(l.getText())) return true;
            if (c instanceof Container nested && hasText(nested, text)) return true;
        }
        return false;
    }

    private static JButton button(Container root, String text) {
        for (Component c : root.getComponents()) {
            if (c instanceof JButton b && text.equals(b.getText())) return b;
            if (c instanceof Container nested) {
                JButton found = button(nested, text);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component c : root.getComponents()) if (c instanceof Container child) layout(child);
    }

    private static void preview(JComponent panel, String path, int width) {
        panel.setSize(width, Math.max(740, panel.getPreferredSize().height));
        layout(panel);
        BufferedImage image =
                new BufferedImage(panel.getWidth(), panel.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        panel.printAll(graphics);
        graphics.dispose();
        try {
            ImageIO.write(image, "png", new File(path));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
