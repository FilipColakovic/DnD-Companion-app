package dndcompanion.persistence;

import dndcompanion.application.ApplicationController;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.ClassId;
import dndcompanion.domain.character.SorcererRules;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public final class RosterStoreChecks {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target"), "roster-store-");
        Path legacy = directory.resolve("character.db");
        CharacterStore old = new CharacterStore(legacy);
        CharacterState original =
                new CharacterState(4, 18, SorcererRules.Subclass.ABERRANT, Set.of("Shield"));
        old.save(original, true);
        byte[] before = Files.readAllBytes(legacy);
        var repository = new SQLiteCharacterRepository(directory.resolve("characters.db"));
        repository.importLegacy(legacy);
        check(
                java.util.Arrays.equals(before, Files.readAllBytes(legacy)),
                "Legacy DB remains byte-for-byte unchanged");
        check(repository.list().size() == 1, "Legacy imported exactly once");
        var imported = repository.list().getFirst();
        check(
                imported.locked() && imported.state().equals(original),
                "Legacy character and lock preserved");
        repository.importLegacy(legacy);
        check(repository.list().size() == 1, "Migration is idempotent");
        var preferencesFirst =
                new SQLiteCharacterRepository(directory.resolve("preferences-first.db"));
        preferencesFirst.preferences(new CharacterRepository.Preferences(true, 18));
        preferencesFirst.importLegacy(legacy);
        check(
                preferencesFirst
                        .preferences()
                        .equals(new CharacterRepository.Preferences(true, 18)),
                "Legacy import never overwrites global preferences already initialized");
        var fighter = CharacterState.create("Champion", ClassId.FIGHTER);
        try (var controller = new dndcompanion.application.CharacterController(fighter)) {
            var session = controller.playSession();
            var wind = dndcompanion.domain.character.ClassActions.fighter().getFirst();
            session.useClassAction(wind, 10, new java.util.Random(1), 0, 10);
            fighter = controller.snapshot();
        }
        var second = repository.create(fighter);
        check(!second.equals(imported.id()), "Distinct UUIDs");
        repository.metadata(second, true, "Campaign A");
        repository.save(second, ApplicationController.rename(fighter, "Renamed"), true);
        check(
                repository.load(second).archived()
                        && repository.load(second).campaign().equals("Campaign A"),
                "Save preserves roster metadata");
        check(
                repository.load(imported.id()).state().equals(original),
                "Saving Fighter cannot overwrite Sorcerer");
        repository.lock(second, false);
        check(!repository.load(second).locked(), "Unlock does not replace snapshot");
        var codec = new CharacterJson();
        check(codec.decode(codec.encode(fighter)).equals(fighter), "Fighter JSON round trip");
        check(codec.decode(codec.encode(original)).equals(original), "Sorcerer JSON round trip");
        try {
            codec.decode(codec.encode(original).replace("\"version\" : 1", "\"version\" : 999"));
            throw new AssertionError("Accepted unsupported version");
        } catch (IllegalArgumentException expected) {
        }
        try {
            codec.decode(
                    codec.encode(fighter)
                            .replace("\"classId\" : \"FIGHTER\"", "\"classId\" : \"WIZARD\""));
            throw new AssertionError("Accepted unsupported class");
        } catch (IllegalArgumentException expected) {
        }
        repository.preferences(new CharacterRepository.Preferences(true, 18));
        var copy = repository.create(fighter);
        check(
                !repository.load(copy).locked() && !repository.load(copy).archived(),
                "Copies/imports start unlocked and unarchived");
        check(
                repository
                                        .load(copy)
                                        .state()
                                        .play()
                                        .speciesUses()
                                        .get("resource:fighter.secondWind")
                                == 1
                        && repository.load(imported.id()).state().play().speciesUses().isEmpty(),
                "Copies preserve resources; different character resources remain isolated");
        repository.played(copy);
        check(repository.load(copy).lastPlayed() != null, "Last played recorded");
        repository.delete(copy);
        check(repository.list().size() == 2, "Delete targets only one character");
        check(
                new SQLiteCharacterRepository(directory.resolve("characters.db"))
                        .preferences()
                        .equals(new CharacterRepository.Preferences(true, 18)),
                "Global preferences survive reopen");
        repository.delete(imported.id());
        repository.importLegacy(legacy);
        check(repository.list().size() == 1, "Deleted migrated character is not resurrected");
        check(
                java.util.Arrays.equals(before, Files.readAllBytes(legacy)),
                "All roster operations leave legacy DB untouched");
        System.out.println(
                "Passed: UUID isolation, read-only migration, locks, archives, campaigns, JSON"
                        + " validation, copies, deletion, preferences and reopen.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
