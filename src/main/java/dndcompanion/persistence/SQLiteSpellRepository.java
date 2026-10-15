package dndcompanion.persistence;

import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellRepository;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only SQLite catalog. Connections close after one load; lookups use an immutable cache. */
public final class SQLiteSpellRepository implements SpellRepository {
    private final Map<String, Entry> entries;

    public SQLiteSpellRepository(Path database) throws SQLException {
        Path path = database.toAbsolutePath().normalize();
        if (!Files.isRegularFile(path)) {
            throw new SQLException("Spell database is missing: " + path);
        }
        Map<String, Entry> loaded = new LinkedHashMap<>();
        try (Connection connection =
                        DriverManager.getConnection(
                                "jdbc:sqlite:" + path.toUri() + "?mode=ro&immutable=1");
                Statement statement = connection.createStatement()) {
            try (ResultSet version = statement.executeQuery("PRAGMA user_version")) {
                if (!version.next() || version.getInt(1) != 2) {
                    throw new SQLException("Unsupported spell database schema; expected version 2");
                }
            }
            try (ResultSet rows =
                    statement.executeQuery(
                            "SELECT spells.*, spell_checks.attack, spell_checks.saves, (SELECT"
                                + " group_concat(class_id,'|') FROM spell_classes WHERE"
                                + " spell_name=spells.name) AS classes FROM spells LEFT JOIN"
                                + " spell_checks ON spell_checks.spell_name=spells.name ORDER BY"
                                + " spells.level, spells.name")) {
                while (rows.next()) {
                    String name = required(rows, "name");
                    int level = rows.getInt("level");
                    if (rows.wasNull() || level < 0 || level > 9) {
                        throw new SQLException("Invalid spell level: " + name);
                    }
                    boolean sorcerer = flag(rows, "sorcerer_spell");
                    var classes =
                            java.util.Arrays.stream(required(rows, "classes").split("\\|"))
                                    .map(dndcompanion.domain.character.ClassId::valueOf)
                                    .collect(java.util.stream.Collectors.toSet());
                    var saves =
                            text(rows, "saves").isEmpty()
                                    ? java.util.Set
                                            .<dndcompanion.domain.character.CharacterSheet.Ability>
                                                    of()
                                    : java.util.Arrays.stream(text(rows, "saves").split("\\|"))
                                            .map(
                                                    dndcompanion.domain.character.CharacterSheet
                                                                    .Ability
                                                            ::valueOf)
                                            .collect(java.util.stream.Collectors.toSet());
                    var profile =
                            new dndcompanion.domain.spells.SpellProfile(
                                    classes, flag(rows, "attack"), saves);
                    if (sorcerer
                            != classes.contains(dndcompanion.domain.character.ClassId.SORCERER))
                        throw new SQLException("Inconsistent Sorcerer spell membership: " + name);
                    SpellDetails details =
                            new SpellDetails(
                                    required(rows, "school"),
                                    required(rows, "casting_time"),
                                    required(rows, "range"),
                                    text(rows, "components"),
                                    required(rows, "duration"),
                                    flag(rows, "concentration"),
                                    flag(rows, "ritual"),
                                    required(rows, "description"),
                                    text(rows, "higher_levels"),
                                    required(rows, "source"));
                    URI reference;
                    try {
                        reference = URI.create(required(rows, "reference"));
                    } catch (IllegalArgumentException error) {
                        throw new SQLException("Invalid spell reference: " + name, error);
                    }
                    if (!"https".equals(reference.getScheme()) || reference.getHost() == null) {
                        throw new SQLException("Spell reference must be an HTTPS URL: " + name);
                    }
                    if (loaded.putIfAbsent(
                                    name,
                                    new Entry(name, level, sorcerer, details, reference, profile))
                            != null) {
                        throw new SQLException("Duplicate spell: " + name);
                    }
                }
            }
        }
        if (loaded.isEmpty()) {
            throw new SQLException("Spell database contains no spells");
        }
        entries = java.util.Collections.unmodifiableMap(loaded);
    }

    private static String text(ResultSet rows, String column) throws SQLException {
        String value = rows.getString(column);
        if (value == null) {
            throw new SQLException("Missing spell field: " + column);
        }
        return value;
    }

    private static String required(ResultSet rows, String column) throws SQLException {
        String value = text(rows, column);
        if (value.isBlank()) {
            throw new SQLException("Empty spell field: " + column);
        }
        return value;
    }

    private static boolean flag(ResultSet rows, String column) throws SQLException {
        int value = rows.getInt(column);
        if (rows.wasNull() || value < 0 || value > 1) {
            throw new SQLException("Invalid spell flag: " + column);
        }
        return value == 1;
    }

    @Override
    public Map<String, Entry> all() {
        return entries;
    }

    /**
     * Works from an IDE, compiled classes, or a packaged JAR, independent of the working directory.
     */
    private static SpellRepository bundled;

    public static synchronized SpellRepository bundled() {
        if (bundled == null) {
            bundled = Bundled.load();
        }
        return bundled;
    }

    private static final class Bundled {
        private static SpellRepository load() {
            Path temporary = null;
            try (var resource =
                    SQLiteSpellRepository.class.getResourceAsStream("/spells/spells.db")) {
                if (resource == null) {
                    throw new IOException(
                            "Missing bundled /spells/spells.db; copy application resources into the"
                                    + " classpath");
                }
                temporary = Files.createTempFile("dnd-spells-", ".db");
                Files.copy(resource, temporary, StandardCopyOption.REPLACE_EXISTING);
                return new SQLiteSpellRepository(temporary);
            } catch (IOException | SQLException error) {
                throw new IllegalStateException("Could not load the bundled spell catalog", error);
            } finally {
                if (temporary != null) {
                    try {
                        Files.deleteIfExists(temporary);
                    } catch (IOException error) {
                        temporary.toFile().deleteOnExit();
                    }
                }
            }
        }
    }
}
