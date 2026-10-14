package dndcompanion.persistence;

import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellRepository;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Exact migration checks, repository lifetime, invalid catalogs and packaged resource loading. */
public final class SpellRepositoryChecks {
    public static void main(String[] args) throws Exception {
        SpellRepository repository = SQLiteSpellRepository.bundled();
        check(repository == SQLiteSpellRepository.bundled(), "Bundled catalog is cached");
        check(repository.all().size() == 219, "All Sorcerer, species and feat spell entries load");
        check(repository.find("Unknown spell").isEmpty(), "Unknown spells have no invented data");
        try {
            repository.all().clear();
            throw new AssertionError("Catalog must be immutable");
        } catch (UnsupportedOperationException expected) {
        }
        for (var spell : SpellCatalog.all()) {
            var entry = repository.require(spell.name());
            check(
                    entry.level() == spell.level()
                            && entry.sorcererSpell() == spell.sorcererSpell(),
                    "Spell metadata matches");
            check(
                    entry.details().equals(SpellDetails.forSpell(spell)),
                    "UI details use repository");
            check(entry.reference().equals(spell.reference()), "Reference URLs use repository");
        }
        if (args.length > 0 && args[0].equals("--bundled-only")) {
            System.out.println(
                    "Passed: bundled spell database loads outside the project directory from a"
                            + " JAR.");
            return;
        }
        List<String> source =
                Files.readAllLines(
                        Path.of("scripts/data/spell-catalog.tsv"), StandardCharsets.UTF_8);
        List<String> actual = new ArrayList<>();
        actual.add(source.getFirst());
        for (var entry : repository.all().values()) {
            var d = entry.details();
            actual.add(
                    String.join(
                            "\t",
                            Arrays.stream(
                                            new String[] {
                                                entry.name(),
                                                "" + entry.level(),
                                                "" + entry.sorcererSpell(),
                                                d.school(),
                                                d.castingTime(),
                                                d.range(),
                                                d.components(),
                                                d.duration(),
                                                "" + d.concentration(),
                                                "" + d.ritual(),
                                                d.description(),
                                                d.higherLevels(),
                                                d.source(),
                                                entry.reference().toString()
                                            })
                                    .map(SpellRepositoryChecks::escape)
                                    .toList()));
        }
        check(
                source.equals(actual),
                "Every migrated field, Unicode character, newline and reference matches the"
                        + " exported data");
        Path directory = Files.createTempDirectory(Path.of("target"), "spell-repository-");
        Path copy = directory.resolve("spell catalog #1.db");
        try {
            Files.copy(Path.of("src/main/resources/spells/spells.db"), copy);
            byte[] before = Files.readAllBytes(copy);
            SpellRepository independent = new SQLiteSpellRepository(copy);
            check(
                    Arrays.equals(before, Files.readAllBytes(copy)),
                    "Loading never changes the database");
            Files.delete(copy);
            check(
                    independent.require("Web").details().description().contains("web"),
                    "Cached lookups need no open file or connection");
            Path missing = directory.resolve("missing.db");
            expectFailure(missing, "Missing database is rejected");
            check(!Files.exists(missing), "Reading a missing database never creates one");
            Files.copy(Path.of("src/main/resources/spells/spells.db"), copy);
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + copy);
                    Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA user_version=999");
            }
            expectFailure(copy, "Unsupported schemas are rejected");
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + copy);
                    Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA user_version=1");
                statement.executeUpdate("DELETE FROM spells");
            }
            expectFailure(copy, "Empty catalogs are rejected");
        } finally {
            Files.deleteIfExists(copy);
            Files.delete(directory);
        }
        System.out.println(
                "Passed: exact migration of 219 entries, cached immutable lookups, read-only files,"
                        + " missing/unsupported/empty databases and reference URLs.");
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\")
                .replace("\t", "\\t")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static void expectFailure(Path path, String message) throws Exception {
        try {
            new SQLiteSpellRepository(path);
            throw new AssertionError(message);
        } catch (SQLException expected) {
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
