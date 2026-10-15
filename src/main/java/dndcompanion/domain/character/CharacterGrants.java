package dndcompanion.domain.character;

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
        if (species == Species.HUMAN)
            for (String feat : sheet.choice(ChoiceIds.HUMAN_FEAT))
                if (!CharacterChoices.scopedHumanFeat(sheet, feat)) toolSources.add(feat);
        var tools = new java.util.TreeSet<>(CharacterChoices.grantedTools(sheet, toolSources));
        if (species == Species.HUMAN
                && sheet.choice(ChoiceIds.HUMAN_FEAT).contains("Skilled")
                && CharacterChoices.scopedHumanFeat(sheet, "Skilled"))
            sheet.choice("human.feat.Skilled").stream()
                    .filter(value -> value.startsWith("Tool: "))
                    .map(value -> value.substring(6))
                    .forEach(tools::add);
        return new CharacterGrants(
                CharacterChoices.skills(sheet, species, manualFeats),
                CharacterChoices.expertise(sheet, species, manualFeats),
                List.copyOf(tools));
    }

    public int tier(CharacterSheet sheet, CharacterSheet.Skill skill) {
        return Math.max(sheet.tier(skill, skills), expertise.contains(skill) ? 2 : 0);
    }

    public static Set<String> sources(
            CharacterSheet sheet, Species species, Set<String> feats, CharacterSheet.Skill skill) {
        Set<String> sources = new HashSet<>();
        if (sheet.tier(skill) > 0) sources.add("manual:" + skill);
        if (feats.contains("Boon of Skill")) sources.add("feat:Boon of Skill");
        BackgroundCatalog.selected(sheet)
                .ifPresent(
                        background -> {
                            if (background.skills().contains(skill))
                                sources.add("background:" + background.name());
                        });
        for (var choice : CharacterChoices.required(sheet, species, feats)) {
            if (choice.complete(sheet)
                    && sheet.choice(choice.id()).contains(skill.label)
                    && !choice.id().endsWith(".expertise")
                    && !sheet.choice(choice.id() + ".tier").contains("Expertise"))
                sources.add("choice:" + choice.id() + ":" + skill);
        }
        return Set.copyOf(sources);
    }
}
