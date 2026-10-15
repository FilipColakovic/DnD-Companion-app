package dndcompanion.persistence;

import dndcompanion.domain.character.CharacterState;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Independent UUID saves. Legacy import never opens the original database for writing. */
public final class SQLiteCharacterRepository implements CharacterRepository {
    private final Path path;
    private final CharacterJson json = new CharacterJson();

    public SQLiteCharacterRepository(Path path) throws IOException, SQLException {
        this.path = path.toAbsolutePath().normalize();
        Files.createDirectories(this.path.getParent());
        try (var connection = connect();
                var statement = connection.createStatement()) {
            try (var version = statement.executeQuery("PRAGMA user_version")) {
                if (version.next() && version.getInt(1) > 1)
                    throw new SQLException(
                            "Character database was created by a newer app version.");
            }
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS characters (id TEXT PRIMARY KEY, snapshot TEXT NOT"
                        + " NULL, locked INTEGER NOT NULL, archived INTEGER NOT NULL, campaign TEXT"
                        + " NOT NULL, created TEXT NOT NULL, updated TEXT NOT NULL, last_played"
                        + " TEXT)");
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS app_settings (key TEXT PRIMARY KEY, value TEXT NOT"
                            + " NULL)");
            statement.executeUpdate("PRAGMA user_version = 1");
        }
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=3000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    @Override
    public List<Entry> list() throws SQLException {
        List<Entry> entries = new ArrayList<>();
        try (var connection = connect();
                var statement = connection.createStatement();
                var rows =
                        statement.executeQuery(
                                "SELECT * FROM characters ORDER BY last_played DESC, created DESC,"
                                        + " id")) {
            while (rows.next()) entries.add(entry(rows));
        }
        return List.copyOf(entries);
    }

    @Override
    public Entry load(UUID id) throws SQLException {
        try (var connection = connect();
                var statement =
                        connection.prepareStatement("SELECT * FROM characters WHERE id=?")) {
            statement.setString(1, id.toString());
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("Character no longer exists.");
                return entry(rows);
            }
        }
    }

    private Entry entry(ResultSet row) throws SQLException {
        try {
            String played = row.getString("last_played");
            return new Entry(
                    UUID.fromString(row.getString("id")),
                    json.decode(row.getString("snapshot")),
                    row.getBoolean("locked"),
                    row.getBoolean("archived"),
                    row.getString("campaign"),
                    Instant.parse(row.getString("created")),
                    Instant.parse(row.getString("updated")),
                    played == null ? null : Instant.parse(played));
        } catch (RuntimeException error) {
            throw new SQLException("Saved character contains invalid data.", error);
        }
    }

    @Override
    public UUID create(CharacterState state) throws SQLException {
        UUID id = UUID.randomUUID();
        try (var connection = connect()) {
            insert(connection, id, state, false);
        }
        return id;
    }

    private void insert(Connection connection, UUID id, CharacterState state, boolean locked)
            throws SQLException {
        try (var statement =
                connection.prepareStatement(
                        "INSERT INTO characters VALUES (?,?,?,0,'',?,?,NULL)")) {
            String now = Instant.now().toString();
            statement.setString(1, id.toString());
            statement.setString(2, json.encode(state));
            statement.setBoolean(3, locked);
            statement.setString(4, now);
            statement.setString(5, now);
            statement.executeUpdate();
        }
    }

    @Override
    public void save(UUID id, CharacterState state, boolean locked) throws SQLException {
        update(
                "UPDATE characters SET snapshot=?, locked=?, updated=? WHERE id=?",
                statement -> {
                    statement.setString(1, json.encode(state));
                    statement.setBoolean(2, locked);
                    statement.setString(3, Instant.now().toString());
                    statement.setString(4, id.toString());
                });
    }

    @Override
    public void metadata(UUID id, boolean archived, String campaign) throws SQLException {
        String clean = campaign.strip();
        if (clean.length() > 80)
            throw new IllegalArgumentException("Campaign names must be at most 80 characters.");
        update(
                "UPDATE characters SET archived=?, campaign=?, updated=? WHERE id=?",
                statement -> {
                    statement.setBoolean(1, archived);
                    statement.setString(2, clean);
                    statement.setString(3, Instant.now().toString());
                    statement.setString(4, id.toString());
                });
    }

    @Override
    public void lock(UUID id, boolean locked) throws SQLException {
        update(
                "UPDATE characters SET locked=? WHERE id=?",
                statement -> {
                    statement.setBoolean(1, locked);
                    statement.setString(2, id.toString());
                });
    }

    @Override
    public void delete(UUID id) throws SQLException {
        update(
                "DELETE FROM characters WHERE id=?",
                statement -> statement.setString(1, id.toString()));
    }

    @Override
    public void played(UUID id) throws SQLException {
        update(
                "UPDATE characters SET last_played=? WHERE id=?",
                statement -> {
                    statement.setString(1, Instant.now().toString());
                    statement.setString(2, id.toString());
                });
    }

    private interface Binder {
        void bind(java.sql.PreparedStatement statement) throws SQLException;
    }

    private void update(String sql, Binder binder) throws SQLException {
        try (var connection = connect();
                var statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            if (statement.executeUpdate() != 1)
                throw new SQLException("Character no longer exists.");
        }
    }

    @Override
    public Preferences preferences() throws SQLException {
        try (var connection = connect()) {
            return new Preferences(
                    Boolean.parseBoolean(readSetting(connection, "dark", "false")),
                    Integer.parseInt(readSetting(connection, "textSize", "14")));
        }
    }

    @Override
    public void preferences(Preferences preferences) throws SQLException {
        try (var connection = connect()) {
            connection.setAutoCommit(false);
            try {
                setting(connection, "dark", Boolean.toString(preferences.dark()));
                setting(connection, "textSize", Integer.toString(preferences.textSize()));
                connection.commit();
            } catch (SQLException error) {
                connection.rollback();
                throw error;
            }
        }
    }

    private String readSetting(Connection connection, String key, String fallback)
            throws SQLException {
        try (var statement =
                connection.prepareStatement("SELECT value FROM app_settings WHERE key=?")) {
            statement.setString(1, key);
            try (var rows = statement.executeQuery()) {
                return rows.next() ? rows.getString(1) : fallback;
            }
        }
    }

    private void setting(Connection connection, String key, String value) throws SQLException {
        try (var statement =
                connection.prepareStatement(
                        "INSERT INTO app_settings VALUES (?,?) ON CONFLICT(key) DO UPDATE SET"
                                + " value=excluded.value")) {
            statement.setString(1, key);
            statement.setString(2, value);
            statement.executeUpdate();
        }
    }

    public void importLegacy(Path legacy) throws SQLException, IOException {
        if (!Files.isRegularFile(legacy)) return;
        try (var connection = connect()) {
            connection.setAutoCommit(false);
            try {
                if (!readSetting(connection, "legacy.imported", "").isEmpty()) return;
                Path copy =
                        Files.createTempDirectory(path.getParent(), ".legacy-read-")
                                .resolve("character.db");
                try {
                    // Read-only SQLite backup includes a possible WAL without modifying its source.
                    try (var source =
                                    DriverManager.getConnection(
                                            "jdbc:sqlite:"
                                                    + legacy.toAbsolutePath().toUri()
                                                    + "?mode=ro");
                            var statement = source.prepareStatement("VACUUM INTO ?")) {
                        statement.setString(1, copy.toString());
                        statement.execute();
                    }
                    CharacterStore old = new CharacterStore(copy);
                    CharacterState state = old.load();
                    UUID id = UUID.randomUUID();
                    insert(connection, id, state, old.isSaveLocked());
                    if (readSetting(connection, "dark", "").isEmpty())
                        setting(connection, "dark", Boolean.toString(state.play().dark()));
                    if (readSetting(connection, "textSize", "").isEmpty())
                        setting(connection, "textSize", Integer.toString(state.play().textSize()));
                    setting(connection, "legacy.imported", id.toString());
                    connection.commit();
                } finally {
                    Files.deleteIfExists(copy);
                    Files.deleteIfExists(copy.getParent());
                }
            } catch (SQLException | IOException | RuntimeException error) {
                connection.rollback();
                throw error;
            }
        }
    }
}
