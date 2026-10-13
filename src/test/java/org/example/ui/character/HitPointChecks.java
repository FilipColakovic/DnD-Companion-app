package org.example.ui.character;

import org.example.domain.character.BackgroundCatalog;
import org.example.domain.character.CharacterSheet.Ability;
import org.example.domain.character.CharacterState;
import org.example.domain.character.FeatCatalog;
import org.example.domain.character.HitPointRules;
import org.example.domain.character.PlaySession;
import org.example.domain.character.PlayState;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellSelection;
import org.example.persistence.CharacterStore;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SwingUtilities;

/**
 * Fixed Sorcerer HP, live bonuses, damage preservation, manual overrides and save compatibility.
 */
public final class HitPointChecks {
    public static void main(String[] args) throws Exception {
        for (int level = 1; level <= 20; level++) {
            check(
                    HitPointRules.calculate(
                                            level,
                                            14,
                                            Species.HUMAN,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            false,
                                            false)
                                    .maximum()
                            == 8 + (level - 1) * 6,
                    "Fixed d6 HP and retroactive CON across all levels");
            check(
                    HitPointRules.calculate(
                                            level,
                                            1,
                                            Species.HUMAN,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            false,
                                            false)
                                    .maximum()
                            == level,
                    "Later levels gain at least one HP");
            int expected =
                    6
                            + (level - 1) * 4
                            + level
                            + level * 2
                            + (level >= 3 ? level : 0)
                            + (level >= 19 ? 40 : 0);
            check(
                    HitPointRules.calculate(
                                            level,
                                            10,
                                            Species.DWARF,
                                            SorcererRules.Subclass.DRACONIC,
                                            true,
                                            true)
                                    .maximum()
                            == expected,
                    "Species, subclass and feat HP bonuses combine once");
        }
        SpellSelection selection = new SpellSelection();
        selection.setCharacter(3, SorcererRules.Subclass.DRACONIC, Species.HUMAN);
        PlaySession session = new PlaySession(selection);
        check(
                session.automaticHp() && session.maxHitPoints() == 17 && session.hitPoints() == 17,
                "New characters start with calculated full HP");
        session.setSheet(session.sheet().withAbility(Ability.CON, 14));
        check(session.maxHitPoints() == 23, "CON changes recalculate HP for every level");
        session.setCurrentHitPoints(13);
        selection.setCharacter(4, SorcererRules.Subclass.DRACONIC, Species.DWARF);
        check(
                session.maxHitPoints() == 34 && session.hitPoints() == 24,
                "Level/species changes preserve ten points of damage");
        session.equipFeat(FeatCatalog.named("Tough"), "", 17, true);
        check(
                session.maxHitPoints() == 42 && session.hitPoints() == 32,
                "Tough updates maximum and preserves damage");
        session.selectBackground(BackgroundCatalog.named("Farmer"));
        check(session.maxHitPoints() == 42, "Multiple sources of Tough do not double-count");
        session.removeFeat("Tough");
        check(session.maxHitPoints() == 42, "Removing manual Tough retains Farmer's Tough");
        session.selectBackground(null);
        check(session.maxHitPoints() == 34, "Removing Farmer deactivates its HP bonus");
        session.setCurrentHitPoints(0);
        selection.setCharacter(5, SorcererRules.Subclass.DRACONIC, Species.DWARF);
        check(session.hitPoints() == 0, "Changing maximum cannot revive a character at zero HP");
        session.longRest();
        check(
                session.hitPoints() == session.maxHitPoints(),
                "Recorded Long Rest clears automatic damage");
        session.setCurrentHitPoints(30);
        PlayState automatic = session.snapshot();
        check(
                PlayState.decode(automatic.encode()).equals(automatic),
                "Calculated HP damage round-trips in the existing properties format");
        PlaySession restored = new PlaySession(selection);
        restored.restore(PlayState.decode(automatic.encode()));
        check(
                restored.automaticHp()
                        && restored.hitPoints() == 30
                        && restored.maxHitPoints() == 42,
                "Restoration recovers automatic mode and current HP");
        session.setHitPoints(7, 40);
        session.setSheet(session.sheet().withAbility(Ability.CON, 18));
        check(
                !session.automaticHp() && session.maxHitPoints() == 40 && session.hitPoints() == 7,
                "Manual maximum remains stable when CON changes");
        session.setAutomaticHp(true);
        check(
                session.maxHitPoints() == 52 && session.hitPoints() == 19,
                "Enabling calculation preserves the existing 33 damage");
        session.setAutomaticHp(false);
        check(
                session.maxHitPoints() == 52 && session.hitPoints() == 19,
                "Disabling calculation captures current maximum and HP");
        session.setAutomaticHp(true);
        session.setCurrentHitPoints(1);
        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC, Species.HUMAN);
        check(
                session.hitPoints() == 0,
                "Reducing maximum below outstanding damage clamps HP at zero");
        selection.setCharacter(5, SorcererRules.Subclass.DRACONIC, Species.DWARF);
        check(
                session.hitPoints() == 0,
                "Increasing maximum after the clamp does not revive the character");
        session.longRest();
        PlaySession legacy = new PlaySession(selection);
        legacy.restore(PlayState.decode("hp=7\nmaxHp=22\n"));
        check(
                !legacy.automaticHp() && legacy.hitPoints() == 7 && legacy.maxHitPoints() == 22,
                "Legacy manual HP is preserved");
        legacy.restore(PlayState.defaults());
        check(
                legacy.automaticHp() && legacy.hitPoints() == legacy.maxHitPoints(),
                "Legacy unset HP becomes calculated HP without changing the stored legacy state");
        Path directory = Files.createTempDirectory(Path.of("target"), "automatic-hp-checks-");
        try {
            CharacterStore store = new CharacterStore(directory.resolve("character.db"));
            session.setAutomaticHp(true);
            session.setCurrentHitPoints(12);
            var state =
                    new CharacterState(
                            5,
                            17,
                            SorcererRules.Subclass.DRACONIC,
                            Set.of(),
                            Species.DWARF,
                            session.snapshot());
            store.save(state);
            var loaded = store.load();
            restored.restore(loaded.play());
            check(
                    loaded.equals(state) && restored.hitPoints() == 12,
                    "SQLite retains automatic HP and wounds");
        } finally {
            Files.deleteIfExists(directory.resolve("character.db"));
            Files.deleteIfExists(directory);
        }
        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel workspace =
                            new GrimoirePanel(
                                    new CharacterState(
                                            3,
                                            17,
                                            SorcererRules.Subclass.WILD_MAGIC,
                                            Set.of(),
                                            Species.HUMAN));
                    try {
                        workspace.showPage(GrimoirePanel.CHARACTER);
                        JCheckBox automaticBox =
                                accessible(workspace, JCheckBox.class, "Automatic maximum HP");
                        JSpinner
                                maximum =
                                        accessible(workspace, JSpinner.class, "Maximum hit points"),
                                current =
                                        accessible(workspace, JSpinner.class, "Current hit points");
                        check(
                                automaticBox.isSelected()
                                        && !maximum.isEnabled()
                                        && maximum.getValue().equals(14),
                                "Character shows a calculated, read-only maximum");
                        accessible(workspace, JSpinner.class, "CON score").setValue(14);
                        check(maximum.getValue().equals(20), "CON edits update the live maximum");
                        current.setValue(8);
                        automaticBox.doClick(0);
                        check(
                                maximum.isEnabled() && current.getValue().equals(8),
                                "Manual toggle preserves current HP");
                        maximum.setValue(30);
                        automaticBox.doClick(0);
                        check(
                                maximum.getValue().equals(20) && current.getValue().equals(0),
                                "Returning to calculation respects outstanding damage");
                    } finally {
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: fixed HP at levels 1–20, CON and HP bonuses, wounds and zero HP,"
                    + " manual/automatic modes, live controls, SQLite and legacy restoration.");
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

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
