package org.example.domain.character;

/** Fixed-value PHB 2024 HP for the current single-class Sorcerer. */
public final class HitPointRules {
    private HitPointRules() {}

    public record Calculation(
            int base,
            int dwarf,
            int draconic,
            int tough,
            int fortitude,
            int maximum,
            String explanation) {}

    public static Calculation calculate(
            int level,
            int constitution,
            Species species,
            SorcererRules.Subclass subclass,
            boolean tough,
            boolean fortitude) {
        if (level < 1 || level > 20 || constitution < 1 || constitution > 30)
            throw new IllegalArgumentException("Invalid level or Constitution");
        int modifier = CharacterSheet.modifier(constitution);
        int first = 6 + modifier,
                later = Math.max(1, 4 + modifier),
                base = first + (level - 1) * later;
        int dwarf = species == Species.DWARF ? level : 0,
                draconic = subclass == SorcererRules.Subclass.DRACONIC && level >= 3 ? level : 0,
                feat = tough ? level * 2 : 0,
                boon = fortitude && level >= 19 ? 40 : 0;
        String explanation =
                "Sorcerer: "
                        + first
                        + " at level 1 + "
                        + (level - 1)
                        + " × "
                        + later
                        + " (CON "
                        + String.format("%+d", modifier)
                        + ")";
        if (dwarf > 0) explanation += " + " + dwarf + " Dwarven Toughness";
        if (draconic > 0) explanation += " + " + draconic + " Draconic Resilience";
        if (feat > 0) explanation += " + " + feat + " Tough";
        if (boon > 0) explanation += " + 40 Boon of Fortitude";
        return new Calculation(
                base,
                dwarf,
                draconic,
                feat,
                boon,
                base + dwarf + draconic + feat + boon,
                explanation);
    }
}
