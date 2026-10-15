package dndcompanion.domain.spells;

import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.ClassId;
import dndcompanion.persistence.SQLiteSpellRepository;

import java.util.Set;
import java.util.stream.Collectors;

/** Reviewed mechanics and class membership, separate from presentation and rulebook prose. */
public record SpellProfile(Set<ClassId> classes, boolean attack, Set<Ability> saves) {
    public SpellProfile {
        classes = Set.copyOf(classes);
        saves = Set.copyOf(saves);
    }

    public static SpellProfile forSpell(SpellCatalog.Spell spell) {
        return SQLiteSpellRepository.bundled().require(spell.name()).profile();
    }

    public String saveLabel() {
        return saves.stream()
                .sorted()
                .map(
                        a ->
                                a.name().charAt(0)
                                        + a.name().substring(1).toLowerCase(java.util.Locale.ROOT))
                .collect(Collectors.joining("/"));
    }
}
