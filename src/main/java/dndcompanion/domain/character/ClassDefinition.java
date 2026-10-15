package dndcompanion.domain.character;

import dndcompanion.domain.character.CharacterSheet.Ability;

import java.util.List;
import java.util.Set;

public record ClassDefinition(
        ClassId id,
        int hitDie,
        Set<Ability> savingThrows,
        List<String> skills,
        int skillCount,
        Set<String> armorTraining,
        boolean martialWeapons) {
    public static ClassDefinition forClass(ClassId id) {
        return switch (id) {
            case SORCERER ->
                    new ClassDefinition(
                            id,
                            6,
                            Set.of(Ability.CON, Ability.CHA),
                            List.of(
                                    "Arcana",
                                    "Deception",
                                    "Insight",
                                    "Intimidation",
                                    "Persuasion",
                                    "Religion"),
                            2,
                            Set.of(),
                            false);
            case FIGHTER ->
                    new ClassDefinition(
                            id,
                            10,
                            Set.of(Ability.STR, Ability.CON),
                            List.of(
                                    "Acrobatics",
                                    "Animal Handling",
                                    "Athletics",
                                    "History",
                                    "Insight",
                                    "Intimidation",
                                    "Persuasion",
                                    "Perception",
                                    "Survival"),
                            2,
                            Set.of("Light", "Medium", "Heavy", "Shield"),
                            true);
            default ->
                    throw new IllegalArgumentException("Class is not implemented: " + id.label());
        };
    }
}
