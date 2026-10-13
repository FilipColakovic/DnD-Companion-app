package org.example.domain.character;

/** Pure skill calculations shared by the sheet and session. */
public final class SkillRules {
    private SkillRules() {}
    public static int bonus(int score, int proficiencyBonus, int tier) {
        if (tier < 0 || tier > 2) throw new IllegalArgumentException("Proficiency tier must be 0–2");
        return CharacterSheet.modifier(score) + tier * proficiencyBonus;
    }
}
