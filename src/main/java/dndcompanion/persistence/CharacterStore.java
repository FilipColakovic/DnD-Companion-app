package dndcompanion.persistence;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlayState;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;

/** One local character, saved atomically so a failed write leaves the previous save intact. */
public final class CharacterStore {
    private final Path path;

    public CharacterStore(Path path) throws IOException, SQLException {
        this.path = path.toAbsolutePath().normalize();
        Files.createDirectories(this.path.getParent());
        try (Connection connection = connect();
                Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS character_state (
                        id INTEGER PRIMARY KEY CHECK (id = 1),
                        level INTEGER NOT NULL CHECK (level BETWEEN 1 AND 20),
                        charisma INTEGER NOT NULL CHECK (charisma BETWEEN 1 AND 30),
                        subclass TEXT NOT NULL CHECK (subclass IN ('ABERRANT', 'CLOCKWORK', 'DRACONIC', 'WILD_MAGIC'))
                    )
                    """);
            boolean hasSpecies = false;
            boolean hasSaveLock = false;
            boolean hasName = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(character_state)")) {
                while (columns.next()) {
                    hasSpecies |= columns.getString("name").equals("species");
                    hasSaveLock |= columns.getString("name").equals("save_locked");
                    hasName |= columns.getString("name").equals("character_name");
                }
            }
            if (!hasSpecies) {
                statement.executeUpdate(
                        "ALTER TABLE character_state ADD COLUMN species TEXT NOT NULL DEFAULT"
                                + " 'HUMAN'");
            }
            if (!hasSaveLock) {
                statement.executeUpdate(
                        "ALTER TABLE character_state ADD COLUMN save_locked INTEGER NOT NULL"
                                + " DEFAULT 0");
            }
            if (!hasName) {
                statement.executeUpdate(
                        "ALTER TABLE character_state ADD COLUMN character_name TEXT NOT NULL"
                                + " DEFAULT 'Sorcerer'");
            }
            statement.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS chosen_spells (
                        character_id INTEGER NOT NULL REFERENCES character_state(id),
                        spell_name TEXT NOT NULL,
                        PRIMARY KEY (character_id, spell_name)
                    )
                    """);
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS play_state (id INTEGER PRIMARY KEY REFERENCES"
                            + " character_state(id), data TEXT NOT NULL)");
        }
    }

    public Path path() {
        return path;
    }

    private Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + path);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 3000");
        } catch (SQLException error) {
            connection.close();
            throw error;
        }
        return connection;
    }

    public CharacterState load() throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try (Statement statement = connection.createStatement();
                    ResultSet result =
                            statement.executeQuery(
                                    "SELECT level, charisma, subclass, species, character_name FROM"
                                            + " character_state WHERE id = 1")) {
                if (!result.next()) {
                    return CharacterState.defaults();
                }
                int level = result.getInt("level");
                int charisma = result.getInt("charisma");
                String subclass = result.getString("subclass");
                String species = result.getString("species");
                String name = result.getString("character_name");
                var spells = new HashSet<String>();
                try (Statement spellStatement = connection.createStatement();
                        ResultSet spellRows =
                                spellStatement.executeQuery(
                                        "SELECT spell_name FROM chosen_spells WHERE character_id ="
                                                + " 1")) {
                    while (spellRows.next()) {
                        spells.add(spellRows.getString(1));
                    }
                }
                try {
                    PlayState play = PlayState.defaults();
                    try (Statement playStatement = connection.createStatement();
                            ResultSet playRows =
                                    playStatement.executeQuery(
                                            "SELECT data FROM play_state WHERE id = 1")) {
                        if (playRows.next()) {
                            play = PlayState.decode(playRows.getString(1));
                        }
                    }
                    return new CharacterState(
                            level,
                            charisma,
                            SorcererRules.Subclass.valueOf(subclass),
                            spells,
                            Species.valueOf(species),
                            play,
                            name);
                } catch (IllegalArgumentException error) {
                    throw new SQLException("The saved character contains invalid data.", error);
                }
            }
        }
    }

    public void save(CharacterState state) throws SQLException {
        save(state, false);
    }

    public boolean isSaveLocked() throws SQLException {
        try (Connection connection = connect();
                Statement statement = connection.createStatement();
                ResultSet result =
                        statement.executeQuery(
                                "SELECT save_locked FROM character_state WHERE id = 1")) {
            return result.next() && result.getBoolean(1);
        }
    }

    /** Change only the save preference; unlocking does not commit experimental character inputs. */
    public void setSaveLocked(boolean locked) throws SQLException {
        try (Connection connection = connect();
                PreparedStatement statement =
                        connection.prepareStatement(
                                "UPDATE character_state SET save_locked = ? WHERE id = 1")) {
            statement.setBoolean(1, locked);
            statement.executeUpdate();
        }
    }

    public void save(CharacterState state, boolean locked) throws SQLException {
        try (Connection connection = connect()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement character =
                        connection.prepareStatement(
                                """
                                INSERT INTO character_state (id, level, charisma, subclass, species, save_locked, character_name) VALUES (1, ?, ?, ?, ?, ?, ?)
                                ON CONFLICT(id) DO UPDATE SET level = excluded.level,
                                    charisma = excluded.charisma, subclass = excluded.subclass, species = excluded.species,
                                    save_locked = excluded.save_locked, character_name = excluded.character_name
                                """)) {
                    character.setInt(1, state.level());
                    character.setInt(2, state.charisma());
                    character.setString(3, state.subclass().name());
                    character.setString(4, state.species().name());
                    character.setBoolean(5, locked);
                    character.setString(6, state.name());
                    character.executeUpdate();
                }
                try (Statement clear = connection.createStatement()) {
                    clear.executeUpdate("DELETE FROM chosen_spells WHERE character_id = 1");
                }
                try (PreparedStatement spell =
                        connection.prepareStatement(
                                "INSERT INTO chosen_spells (character_id, spell_name) VALUES"
                                        + " (1, ?)")) {
                    for (String name : state.chosenSpells()) {
                        spell.setString(1, name);
                        spell.addBatch();
                    }
                    spell.executeBatch();
                }
                try (PreparedStatement play =
                        connection.prepareStatement(
                                "INSERT INTO play_state (id, data) VALUES (1, ?) ON CONFLICT(id) DO"
                                        + " UPDATE SET data = excluded.data")) {
                    play.setString(1, state.play().encode());
                    play.executeUpdate();
                }
                connection.commit();
            } catch (SQLException error) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackError) {
                    error.addSuppressed(rollbackError);
                }
                throw error;
            }
        }
    }
}
