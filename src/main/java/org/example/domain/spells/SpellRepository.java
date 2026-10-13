package org.example.domain.spells;

import java.net.URI;
import java.util.Map;
import java.util.Optional;

/** Storage-independent, immutable view of the bundled spell catalog. */
public interface SpellRepository {
    record Entry(
            String name, int level, boolean sorcererSpell, SpellDetails details, URI reference) {}

    Map<String, Entry> all();

    default Optional<Entry> find(String name) {
        return Optional.ofNullable(all().get(name));
    }

    default Entry require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("Missing spell: " + name));
    }
}
