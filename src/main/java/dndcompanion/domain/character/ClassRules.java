package dndcompanion.domain.character;

import java.util.List;

/**
 * Class-neutral progression; the old Stats type is an explicit presentation compatibility adapter.
 */
public interface ClassRules {
    enum Preparation {
        NONE,
        LEARNED,
        DAILY_PREPARED,
        SPELLBOOK
    }

    List<Integer> spellSlots(ClassProgression progression);

    int cantrips(ClassProgression progression);

    int prepared(ClassProgression progression);

    int hitDie();

    default Preparation preparation() {
        return Preparation.NONE;
    }

    default java.util.Optional<CharacterSheet.Ability> spellcastingAbility() {
        return java.util.Optional.empty();
    }

    default boolean alwaysPrepared(
            dndcompanion.domain.spells.SpellCatalog.Spell spell, ClassProgression p) {
        return false;
    }

    static ClassRules forClass(ClassId id) {
        return switch (id) {
            case SORCERER -> new SorcererClassRules();
            case FIGHTER -> new FighterRules();
            default -> throw new IllegalArgumentException("Class is not implemented.");
        };
    }

    static int proficiency(int level) {
        return 2 + (level - 1) / 4;
    }

    static SorcererRules.Stats compatibility(ClassProgression progression, int charisma) {
        if (progression.classId() == ClassId.SORCERER)
            return SorcererRules.calculate(
                    progression.level(),
                    charisma,
                    SorcererRules.Subclass.valueOf(progression.subclass()));
        int modifier = CharacterSheet.modifier(charisma), pb = proficiency(progression.level());
        return new SorcererRules.Stats(
                modifier,
                pb,
                8 + pb + modifier,
                pb + modifier,
                0,
                0,
                0,
                0,
                forClass(progression.classId()).spellSlots(progression));
    }
}
