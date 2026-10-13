package org.example.domain.character;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

/** Single-class 2024 Sorcerer with the four Player's Handbook subclasses. */
public final class SorcererRules {
    private SorcererRules() {}

    public enum Subclass {
        ABERRANT("Aberrant Sorcery"),
        CLOCKWORK("Clockwork Sorcery"),
        DRACONIC("Draconic Sorcery"),
        WILD_MAGIC("Wild Magic Sorcery");

        private final String displayName;
        Subclass(String displayName) { this.displayName = displayName; }
        @Override public String toString() { return displayName; }
    }

    private static final int[] PREPARED = {
        2, 4, 6, 7, 9, 10, 11, 12, 14, 15, 16, 16, 17, 17, 18, 18, 19, 20, 21, 22
    };
    private static final int[][] SLOTS = {
        {2,0,0,0,0,0,0,0,0}, {3,0,0,0,0,0,0,0,0},
        {4,2,0,0,0,0,0,0,0}, {4,3,0,0,0,0,0,0,0},
        {4,3,2,0,0,0,0,0,0}, {4,3,3,0,0,0,0,0,0},
        {4,3,3,1,0,0,0,0,0}, {4,3,3,2,0,0,0,0,0},
        {4,3,3,3,1,0,0,0,0}, {4,3,3,3,2,0,0,0,0},
        {4,3,3,3,2,1,0,0,0}, {4,3,3,3,2,1,0,0,0},
        {4,3,3,3,2,1,1,0,0}, {4,3,3,3,2,1,1,0,0},
        {4,3,3,3,2,1,1,1,0}, {4,3,3,3,2,1,1,1,0},
        {4,3,3,3,2,1,1,1,1}, {4,3,3,3,3,1,1,1,1},
        {4,3,3,3,3,2,1,1,1}, {4,3,3,3,3,2,2,1,1}
    };

    public record Stats(int charismaModifier, int proficiencyBonus, int spellSaveDc,
                        int spellAttack, int sorceryPoints, int cantrips,
                        int basePreparedSpells, int subclassPreparedSpells,
                        List<Integer> spellSlots) {
        public Stats { spellSlots = List.copyOf(spellSlots); }
        public int preparedSpells() { return basePreparedSpells + subclassPreparedSpells; }
        public int totalSpellSlots() {
            return spellSlots.stream().mapToInt(Integer::intValue).sum();
        }
    }

    public static Stats calculate(int level, int charisma) {
        return calculate(level, charisma, Subclass.ABERRANT);
    }

    public static Stats calculate(int level, int charisma, Subclass subclass) {
        Objects.requireNonNull(subclass, "Subclass is required.");
        if (level < 1 || level > 20) {
            throw new IllegalArgumentException("Sorcerer level must be between 1 and 20.");
        }
        if (charisma < 1 || charisma > 30) {
            throw new IllegalArgumentException("Charisma must be between 1 and 30.");
        }
        int modifier = Math.floorDiv(charisma - 10, 2);
        int proficiency = 2 + (level - 1) / 4;
        int bonusSpells = level < 3 || subclass == Subclass.WILD_MAGIC
                ? 0 : level < 5 ? 4 : level < 7 ? 6 : level < 9 ? 8 : 10;
        int cantrips = (level < 4 ? 4 : level < 10 ? 5 : 6)
                + (level >= 3 && subclass == Subclass.ABERRANT ? 1 : 0);
        return new Stats(modifier, proficiency, 8 + proficiency + modifier,
                proficiency + modifier, level == 1 ? 0 : level, cantrips,
                PREPARED[level - 1], bonusSpells,
                IntStream.of(SLOTS[level - 1]).boxed().toList());
    }
}
