package dndcompanion.domain.character;

/** Pure skill calculations shared by the sheet and session. */
public final class SkillRules {
    private SkillRules() {}

    public static int bonus(int score, int proficiencyBonus, int tier) {
        return bonus(score, proficiencyBonus, Proficiency.fromStoredValue(tier));
    }

    public static int bonus(int score, int proficiencyBonus, Proficiency proficiency) {
        return CharacterSheet.modifier(score) + proficiency.storedValue() * proficiencyBonus;
    }
}
