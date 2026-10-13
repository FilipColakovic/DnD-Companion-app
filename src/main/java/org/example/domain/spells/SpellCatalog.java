package org.example.domain.spells;

import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;

import java.net.URI;
import java.util.*;

/** PHB 2024 spell names/levels, checked against the licensed Roll20 compendium. */
public final class SpellCatalog {
    private SpellCatalog() {}

    public record Spell(String name, int level, boolean sorcererSpell) {
        public String levelName() { return level == 0 ? "Cantrip" : "Level " + level; }
        public URI reference() {
            return org.example.persistence.SQLiteSpellRepository.bundled().require(name).reference();
        }
    }
    private record Grant(SorcererRules.Subclass subclass, int unlockLevel, String name) {}

    private static final List<Grant> GRANTS = new ArrayList<>();
    private static final List<Spell> SPELLS;
    static {
        Map<String, Spell> spells = new LinkedHashMap<>();
        for (var entry : org.example.persistence.SQLiteSpellRepository.bundled().all().values()) {
            spells.put(entry.name(), new Spell(entry.name(), entry.level(), entry.sorcererSpell()));
        }
        grant(spells, SorcererRules.Subclass.ABERRANT, 3, 0, "Mind Sliver");
        grant(spells, SorcererRules.Subclass.ABERRANT, 3, 1, "Arms of Hadar|Dissonant Whispers");
        grant(spells, SorcererRules.Subclass.ABERRANT, 3, 2, "Calm Emotions|Detect Thoughts");
        grant(spells, SorcererRules.Subclass.ABERRANT, 5, 3, "Hunger of Hadar|Sending");
        grant(spells, SorcererRules.Subclass.ABERRANT, 7, 4, "Evard's Black Tentacles|Summon Aberration");
        grant(spells, SorcererRules.Subclass.ABERRANT, 9, 5, "Rary's Telepathic Bond|Telekinesis");
        grant(spells, SorcererRules.Subclass.CLOCKWORK, 3, 1, "Alarm|Protection from Evil and Good");
        grant(spells, SorcererRules.Subclass.CLOCKWORK, 3, 2, "Aid|Lesser Restoration");
        grant(spells, SorcererRules.Subclass.CLOCKWORK, 5, 3, "Dispel Magic|Protection from Energy");
        grant(spells, SorcererRules.Subclass.CLOCKWORK, 7, 4, "Freedom of Movement|Summon Construct");
        grant(spells, SorcererRules.Subclass.CLOCKWORK, 9, 5, "Greater Restoration|Wall of Force");
        grant(spells, SorcererRules.Subclass.DRACONIC, 3, 1, "Chromatic Orb|Command");
        grant(spells, SorcererRules.Subclass.DRACONIC, 3, 2, "Alter Self|Dragon's Breath");
        grant(spells, SorcererRules.Subclass.DRACONIC, 5, 3, "Fear|Fly");
        grant(spells, SorcererRules.Subclass.DRACONIC, 7, 4, "Arcane Eye|Charm Monster");
        grant(spells, SorcererRules.Subclass.DRACONIC, 9, 5, "Legend Lore|Summon Dragon");
        for (Species species : Species.values()) {
            for (Species.Grant grant : species.grants()) {
                requireGrant(spells, grant.name(), grant.spellLevel());
            }
        }
        SPELLS = spells.values().stream().sorted(Comparator.comparingInt(Spell::level)
                .thenComparing(Spell::name)).toList();
    }
    private static void grant(Map<String, Spell> spells, SorcererRules.Subclass subclass,
                              int unlock, int level, String names) {
        for (String name : names.split("\\|")) {
            requireGrant(spells, name, level);
            GRANTS.add(new Grant(subclass, unlock, name));
        }
    }
    private static void requireGrant(Map<String, Spell> spells, String name, int level) {
        Spell spell = spells.get(name);
        if (spell == null || spell.level() != level) {
            throw new IllegalStateException("Missing or mismatched spell grant in catalog: " + name);
        }
    }
    public static List<Spell> all() { return SPELLS; }
    public static List<Spell> forSubclass(SorcererRules.Subclass subclass) {
        return SPELLS.stream().filter(spell -> spell.sorcererSpell() || unlockLevel(spell, subclass) > 0).toList();
    }
    public static int unlockLevel(Spell spell, SorcererRules.Subclass subclass) {
        return GRANTS.stream().filter(grant -> grant.subclass() == subclass && grant.name().equals(spell.name()))
                .mapToInt(Grant::unlockLevel).findFirst().orElse(0);
    }
    public static boolean alwaysPrepared(Spell spell, int level, SorcererRules.Subclass subclass) {
        int unlock = unlockLevel(spell, subclass);
        return unlock > 0 && level >= unlock;
    }
}
