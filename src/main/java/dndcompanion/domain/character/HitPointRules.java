package dndcompanion.domain.character;

/** Fixed-value PHB 2024 HP for the current single-class Sorcerer. */
public final class HitPointRules {
    private HitPointRules() {}

    public static Calculation calculate(
            ClassProgression progression,
            int constitution,
            Species species,
            boolean tough,
            boolean fortitude) {
        if (progression.classId() == ClassId.SORCERER)
            return calculate(
                    progression.level(),
                    constitution,
                    species,
                    SorcererRules.Subclass.valueOf(progression.subclass()),
                    tough,
                    fortitude);
        int level = progression.level(), modifier = CharacterSheet.modifier(constitution);
        int first = 10 + modifier,
                later = Math.max(1, 6 + modifier),
                base = first + (level - 1) * later;
        int dwarf = species == Species.DWARF ? level : 0,
                feat = tough ? 2 * level : 0,
                boon = fortitude && level >= 19 ? 40 : 0;
        return new Calculation(
                base,
                dwarf,
                0,
                feat,
                boon,
                base + dwarf + feat + boon,
                "Fighter: "
                        + first
                        + " at level 1 + "
                        + (level - 1)
                        + " × "
                        + later
                        + " + "
                        + dwarf
                        + " Dwarf + "
                        + feat
                        + " Tough + "
                        + boon
                        + " Fortitude");
    }

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
