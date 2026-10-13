package org.example.persistence;

import org.example.application.CharacterController;
import org.example.domain.character.BackgroundCatalog;
import org.example.domain.character.CharacterSheet;
import org.example.domain.character.CharacterState;
import org.example.domain.character.FeatCatalog;
import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellSelection;
import org.example.ui.spells.SpellLibraryPanel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Set;

import javax.swing.SwingUtilities;

public final class CharacterStoreChecks {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target"), "character-store-checks-");
        Path database = directory.resolve("character.db");
        try {
            CharacterStore store = new CharacterStore(database);
            check(store.load().equals(CharacterState.defaults()), "First launch uses defaults");
            CharacterState first =
                    new CharacterState(
                            8,
                            30,
                            SorcererRules.Subclass.CLOCKWORK,
                            Set.of("Fire Bolt", "Fireball", "Shield"));
            store.save(first);
            check(
                    new CharacterStore(database).load().equals(first),
                    "Inputs and spells persist across reopening");
            CharacterState named =
                    new CharacterState(
                            first.level(),
                            first.charisma(),
                            first.subclass(),
                            first.chosenSpells(),
                            first.species(),
                            first.play(),
                            "Sylphira");
            store.save(named);
            check(
                    new CharacterStore(database).load().equals(named),
                    "Character profile name persists across reopening");
            store.save(first);
            SpellSelection playSelection = new SpellSelection();
            playSelection.setCharacter(8, SorcererRules.Subclass.CLOCKWORK, Species.HUMAN);
            playSelection.restoreChoices(first.chosenSpells());
            PlaySession session = new PlaySession(playSelection);
            session.setHitPoints(7, 22);
            session.setSheet(
                    session.sheet()
                            .withAbility(CharacterSheet.Ability.DEX, 14)
                            .withSkill(CharacterSheet.Skill.ARCANA, 2)
                            .withNote(CharacterSheet.Note.BACKGROUND, "Sage")
                            .withChoice(
                                    "sorcerer.skills", java.util.List.of("Deception", "Persuasion"))
                            .withChoice("human.skill", java.util.List.of("Perception"))
                            .withChoice("human.feat", java.util.List.of("Skilled")));
            check(
                    session.equipFeat(
                                    FeatCatalog.named("War Caster"),
                                    "CHA +1\nChosen at level 4",
                                    30,
                                    true)
                            .isEmpty(),
                    "Equip a PHB feat for persistence");
            session.selectBackground(BackgroundCatalog.named("Artisan"));
            session.setSheet(
                    session.sheet()
                            .withChoice(
                                    "background.Artisan.tool", java.util.List.of("Smith's Tools")));
            session.choose(Metamagic.QUICKENED, true);
            session.cast(
                    SpellCatalog.all().stream()
                            .filter(s -> s.name().equals("Fireball"))
                            .findFirst()
                            .orElseThrow(),
                    3,
                    false,
                    Set.of(Metamagic.QUICKENED));
            session.shortRest();
            session.appearance(true, 18);
            CharacterState played =
                    new CharacterState(
                            8,
                            30,
                            SorcererRules.Subclass.CLOCKWORK,
                            first.chosenSpells(),
                            Species.HUMAN,
                            session.snapshot());
            store.save(played, true);
            check(
                    new CharacterStore(database).load().equals(played) && store.isSaveLocked(),
                    "Resources, rest usage, Metamagic, turn state and appearance persist atomically"
                            + " in checkpoint");
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.execute(
                        "CREATE TRIGGER fail_play_update BEFORE UPDATE ON play_state BEGIN SELECT"
                                + " RAISE(ABORT, 'Test play save failure'); END");
            }
            try {
                store.save(first, false);
                throw new AssertionError("Play save failure not raised");
            } catch (SQLException expected) {
            }
            check(
                    store.load().equals(played) && store.isSaveLocked(),
                    "Play write failure rolls back character, choices and lock");
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER fail_play_update");
            }
            CharacterState lower =
                    new CharacterState(
                            1, 9, SorcererRules.Subclass.WILD_MAGIC, Set.of("Fireball", "Shield"));
            store.save(lower);
            check(
                    store.load().equals(lower),
                    "New save replaces removed choices and previous inputs");
            SpellSelection restored = new SpellSelection();
            restored.setCharacter(lower.level(), lower.subclass());
            restored.restoreChoices(lower.chosenSpells());
            var fireball =
                    SpellCatalog.all().stream()
                            .filter(spell -> spell.name().equals("Fireball"))
                            .findFirst()
                            .orElseThrow();
            check(
                    restored.selected(fireball) && !restored.available(fireball),
                    "Dormant choices survive reload");

            // A failure after the inputs and old choices have been modified must roll back both
            // tables.
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.execute(
                        """
                        CREATE TRIGGER fail_spell_insert BEFORE INSERT ON chosen_spells
                        WHEN NEW.spell_name = 'Fireball'
                        BEGIN SELECT RAISE(ABORT, 'Test save failure'); END
                        """);
            }
            try {
                store.save(first, true);
                throw new AssertionError("Injected write failure was not raised");
            } catch (SQLException expected) {
            }
            check(
                    store.load().equals(lower),
                    "Failed save keeps previous inputs and choices intact");
            check(!store.isSaveLocked(), "Failed checkpoint save does not change save protection");
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.execute("DROP TRIGGER fail_spell_insert");
            }

            SwingUtilities.invokeAndWait(
                    () -> {
                        CharacterController panel = new CharacterController(first);
                        check(
                                panel.snapshot().equals(first),
                                "Main window restores inputs and manual choices");
                        SpellLibraryPanel spellbook =
                                new SpellLibraryPanel(panel.playSession(), panel::snapshot);
                        check(
                                panel.snapshot().chosenSpells().equals(first.chosenSpells()),
                                "Opening the spellbook does not add automatic grants to the save");
                        panel.selectSubclass(SorcererRules.Subclass.DRACONIC);
                        check(
                                panel.snapshot().chosenSpells().equals(first.chosenSpells()),
                                "Subclass changes preserve manual choices");
                        spellbook.dispose();
                        panel.close();
                    });
            CharacterState empty =
                    new CharacterState(20, 1, SorcererRules.Subclass.ABERRANT, Set.of());
            store.save(empty);
            check(store.load().equals(empty), "Empty spell selection clears the database");
            CharacterState speciesState =
                    new CharacterState(
                            5,
                            19,
                            SorcererRules.Subclass.DRACONIC,
                            Set.of("Shield"),
                            Species.INFERNAL_TIEFLING);
            store.save(speciesState);
            check(
                    new CharacterStore(database).load().equals(speciesState),
                    "Species and lineage persist across reopening");
            // Recreate the previous schema to exercise migration of a real existing save.
            try (var connection =
                            DriverManager.getConnection(
                                    "jdbc:sqlite:" + database.toAbsolutePath());
                    var statement = connection.createStatement()) {
                statement.execute("ALTER TABLE character_state DROP COLUMN species");
                statement.execute("ALTER TABLE character_state DROP COLUMN save_locked");
            }
            CharacterState migrated = new CharacterStore(database).load();
            check(
                    migrated.species() == Species.HUMAN
                            && migrated.level() == 5
                            && migrated.charisma() == 19
                            && migrated.chosenSpells().equals(Set.of("Shield")),
                    "Legacy migration preserves inputs and spell choices");
            check(!store.isSaveLocked(), "Legacy saves default to normal saving");
            System.out.println(
                    "Passed: SQLite reopen, replacement, dormant choices, rollback, empty choices,"
                            + " and UI restoration.");
        } finally {
            Files.deleteIfExists(database);
            Files.delete(directory);
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
