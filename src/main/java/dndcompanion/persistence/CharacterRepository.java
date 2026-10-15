package dndcompanion.persistence;

import dndcompanion.domain.character.CharacterState;

import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface CharacterRepository {
    record Entry(
            UUID id,
            CharacterState state,
            boolean locked,
            boolean archived,
            String campaign,
            Instant created,
            Instant updated,
            Instant lastPlayed) {}

    record Preferences(boolean dark, int textSize) {
        public Preferences {
            if (textSize < 12 || textSize > 18)
                throw new IllegalArgumentException("Text size must be 12–18.");
        }
    }

    List<Entry> list() throws SQLException;

    Entry load(UUID id) throws SQLException;

    UUID create(CharacterState state) throws SQLException;

    void save(UUID id, CharacterState state, boolean locked) throws SQLException;

    void lock(UUID id, boolean locked) throws SQLException;

    void metadata(UUID id, boolean archived, String campaign) throws SQLException;

    void delete(UUID id) throws SQLException;

    void played(UUID id) throws SQLException;

    Preferences preferences() throws SQLException;

    void preferences(Preferences preferences) throws SQLException;
}
