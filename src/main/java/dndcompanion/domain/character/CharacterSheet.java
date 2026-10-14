package dndcompanion.domain.character;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Additional single-Sorcerer sheet inputs. Charisma, level and HP retain their existing storage.
 */
public record CharacterSheet(Map<String, String> values) {
    public enum Ability {
        STR,
        DEX,
        CON,
        INT,
        WIS,
        CHA
    }

    public enum Skill {
        ACROBATICS("Acrobatics", Ability.DEX),
        ANIMAL_HANDLING("Animal Handling", Ability.WIS),
        ARCANA("Arcana", Ability.INT),
        ATHLETICS("Athletics", Ability.STR),
        DECEPTION("Deception", Ability.CHA),
        HISTORY("History", Ability.INT),
        INSIGHT("Insight", Ability.WIS),
        INTIMIDATION("Intimidation", Ability.CHA),
        INVESTIGATION("Investigation", Ability.INT),
        MEDICINE("Medicine", Ability.WIS),
        NATURE("Nature", Ability.INT),
        PERCEPTION("Perception", Ability.WIS),
        PERFORMANCE("Performance", Ability.CHA),
        PERSUASION("Persuasion", Ability.CHA),
        RELIGION("Religion", Ability.INT),
        SLEIGHT_OF_HAND("Sleight of Hand", Ability.DEX),
        STEALTH("Stealth", Ability.DEX),
        SURVIVAL("Survival", Ability.WIS);
        public final String label;
        public final Ability ability;

        Skill(String label, Ability ability) {
            this.label = label;
            this.ability = ability;
        }
    }

    public enum Field {
        TEMP_HP(0, 9999, 0),
        HEALER_KIT_USES(0, 999, 0),
        AC_OVERRIDE(0, 99, 0),
        SPEED(0, 500, 30),
        SPENT_HIT_DICE(0, 20, 0),
        DEATH_SUCCESSES(0, 3, 0),
        DEATH_FAILURES(0, 3, 0),
        INSPIRATION(0, 1, 0),
        AUTOMATIC_HP(0, 1, 1),
        HP_DAMAGE(0, 9999, 0),
        HP_ZERO(0, 1, 0);
        public final int minimum, maximum, initial;

        Field(int minimum, int maximum, int initial) {
            this.minimum = minimum;
            this.maximum = maximum;
            this.initial = initial;
        }
    }

    public enum Note {
        BACKGROUND,
        ALIGNMENT,
        LANGUAGES,
        TRAINING,
        BACKGROUND_FEAT,
        CRAFTED_ITEM
    }

    public CharacterSheet {
        values = Map.copyOf(values);
        values.forEach((key, value) -> validate(key, value));
    }

    public static CharacterSheet defaults() {
        return new CharacterSheet(Map.of());
    }

    private static void validate(String key, String value) {
        if (key.startsWith("ability.")) {
            Ability ability = Ability.valueOf(key.substring(8));
            if (ability == Ability.CHA)
                throw new IllegalArgumentException("Charisma is stored with the character");
            range(Integer.parseInt(value), 1, 30);
        } else if (key.startsWith("skill.")) {
            Skill.valueOf(key.substring(6));
            range(Integer.parseInt(value), 0, 2);
        } else if (key.startsWith("field.")) {
            Field field = Field.valueOf(key.substring(6));
            range(Integer.parseInt(value), field.minimum, field.maximum);
        } else if (key.startsWith("note.")) {
            Note.valueOf(key.substring(5));
            if (value.length() > 2000)
                throw new IllegalArgumentException("Sheet notes exceed 2000 characters");
        } else if (key.startsWith("choice.")) {
            CharacterChoices.validateStored(key.substring(7), value);
        } else throw new IllegalArgumentException("Unknown sheet field: " + key);
    }

    private static void range(int value, int min, int max) {
        if (value < min || value > max)
            throw new IllegalArgumentException("Sheet value outside allowed range");
    }

    private CharacterSheet put(String key, String value) {
        Map<String, String> updated = new HashMap<>(values);
        updated.put(key, value);
        return new CharacterSheet(updated);
    }

    public CharacterSheet withAbility(Ability ability, int score) {
        return put("ability." + ability, Integer.toString(score));
    }

    public CharacterSheet withSkill(Skill skill, int tier) {
        return put("skill." + skill, Integer.toString(tier));
    }

    public CharacterSheet withSkill(Skill skill, Proficiency proficiency) {
        return withSkill(skill, proficiency.storedValue());
    }

    public Proficiency proficiency(Skill skill) {
        return Proficiency.fromStoredValue(tier(skill));
    }

    public CharacterSheet withField(Field field, int value) {
        return put("field." + field, Integer.toString(value));
    }

    public CharacterSheet withNote(Note note, String value) {
        return put("note." + note, value);
    }

    public CharacterSheet withChoice(String id, List<String> choices) {
        return put("choice." + id, String.join("|", choices));
    }

    public List<String> choice(String id) {
        String value = values.getOrDefault("choice." + id, "");
        return value.isEmpty() ? List.of() : List.of(value.split("\\|"));
    }

    public int score(Ability ability, int charisma) {
        return ability == Ability.CHA
                ? charisma
                : Integer.parseInt(values.getOrDefault("ability." + ability, "10"));
    }

    public int tier(Skill skill) {
        return Integer.parseInt(values.getOrDefault("skill." + skill, "0"));
    }

    public int tier(Skill skill, Set<Skill> granted) {
        return Math.max(tier(skill), granted.contains(skill) ? 1 : 0);
    }

    public int field(Field field) {
        return Integer.parseInt(
                values.getOrDefault("field." + field, Integer.toString(field.initial)));
    }

    public String note(Note note) {
        return values.getOrDefault(
                "note." + note,
                switch (note) {
                    case LANGUAGES -> "Common";
                    case TRAINING -> "Simple weapons; no armor training";
                    default -> "";
                });
    }

    public static int modifier(int score) {
        return Math.floorDiv(score - 10, 2);
    }

    public int skillBonus(Skill skill, int charisma, int proficiency) {
        return SkillRules.bonus(score(skill.ability, charisma), proficiency, proficiency(skill));
    }

    public int skillBonus(Skill skill, int charisma, int proficiency, Set<Skill> granted) {
        return SkillRules.bonus(score(skill.ability, charisma), proficiency, tier(skill, granted));
    }

    public int armorClass(int charisma) {
        return field(Field.AC_OVERRIDE) > 0
                ? field(Field.AC_OVERRIDE)
                : 10 + modifier(score(Ability.DEX, charisma));
    }

    public int hitDieHealing(int die) {
        range(die, 1, 6);
        return Math.max(1, die + modifier(score(Ability.CON, 10)));
    }

    public CharacterSheet rested() {
        return withNote(Note.CRAFTED_ITEM, "")
                .withField(Field.SPENT_HIT_DICE, 0)
                .withField(Field.TEMP_HP, 0)
                .withField(Field.DEATH_SUCCESSES, 0)
                .withField(Field.DEATH_FAILURES, 0);
    }
}
