package dndcompanion.domain.spells;

import dndcompanion.persistence.SQLiteSpellRepository;

public record SpellDetails(
        String school,
        String castingTime,
        String range,
        String components,
        String duration,
        boolean concentration,
        boolean ritual,
        String description,
        String higherLevels,
        String source) {
    public static SpellDetails forSpell(SpellCatalog.Spell spell) {
        return SQLiteSpellRepository.bundled().require(spell.name()).details();
    }

    public String shortDescription() {
        String plain =
                description
                        .replaceAll("(?s)<[^>]*>", " ")
                        .replaceAll("[*#|_]", "")
                        .replaceAll("\\s+", " ")
                        .strip();
        int end = plain.indexOf(". ");
        String first = end > 0 ? plain.substring(0, end + 1) : plain;
        if (first.length() <= 135) {
            return first;
        }
        int split = first.lastIndexOf(' ', 130);
        return first.substring(0, split > 0 ? split : 130) + "…";
    }
}
