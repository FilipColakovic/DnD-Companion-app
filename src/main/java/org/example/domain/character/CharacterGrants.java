package org.example.domain.character;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Effective proficiency grants derived from the active sources, never persisted as manual skills.
 */
public record CharacterGrants(
        Set<CharacterSheet.Skill> skills, Set<CharacterSheet.Skill> expertise, List<String> tools) {
    public CharacterGrants {
        skills = Set.copyOf(skills);
        expertise = Set.copyOf(expertise);
        tools = List.copyOf(tools);
    }

    public static CharacterGrants calculate(
            CharacterSheet sheet, Species species, Set<String> manualFeats) {
        Set<String> toolSources = new HashSet<>(manualFeats);
        if (species == Species.HUMAN) toolSources.addAll(sheet.choice(ChoiceIds.HUMAN_FEAT));
        return new CharacterGrants(
                CharacterChoices.skills(sheet, species, manualFeats),
                CharacterChoices.expertise(sheet, species, manualFeats),
                CharacterChoices.grantedTools(sheet, toolSources));
    }

    public int tier(CharacterSheet sheet, CharacterSheet.Skill skill) {
        return Math.max(sheet.tier(skill, skills), expertise.contains(skill) ? 2 : 0);
    }
}
