package dndcompanion.domain.character;

import dndcompanion.domain.spells.SpellCatalog;

import java.util.Set;

/** Persist inputs and manual spell choices; calculated stats and subclass grants are derived. */
public record CharacterState(
        int level,
        int charisma,
        SorcererRules.Subclass subclass,
        Set<String> chosenSpells,
        Species species,
        PlayState play,
        String name) {
    public CharacterState(
            int level,
            int charisma,
            SorcererRules.Subclass subclass,
            Set<String> chosenSpells,
            Species species,
            PlayState play) {
        this(level, charisma, subclass, chosenSpells, species, play, "Sorcerer");
    }

    public CharacterState(
            int level,
            int charisma,
            SorcererRules.Subclass subclass,
            Set<String> chosenSpells,
            Species species) {
        this(level, charisma, subclass, chosenSpells, species, PlayState.defaults());
    }

    public CharacterState(
            int level, int charisma, SorcererRules.Subclass subclass, Set<String> chosenSpells) {
        this(level, charisma, subclass, chosenSpells, Species.HUMAN);
    }

    public CharacterState {
        SorcererRules.calculate(level, charisma, subclass);
        java.util.Objects.requireNonNull(species);
        java.util.Objects.requireNonNull(play);
        play = play.withSheet(play.sheet().withAbility(CharacterSheet.Ability.CHA, charisma));
        for (var ability : CharacterSheet.Ability.values())
            play =
                    play.withSheet(
                            play.sheet()
                                    .withAbility(ability, play.sheet().score(ability, charisma)));
        var progression = play.sheet().choice("class.progression");
        if (!progression.isEmpty()
                && (progression.size() != 1
                        || ClassProgression.decode(progression.getFirst()).level() != level))
            throw new IllegalArgumentException(
                    "Only one class matching the character level is supported.");
        name = java.util.Objects.requireNonNull(name).strip();
        if (name.isEmpty() || name.length() > 60) {
            throw new IllegalArgumentException("Character name must contain 1–60 characters.");
        }
        chosenSpells = Set.copyOf(chosenSpells);
        for (String spellName : chosenSpells) {
            if (SpellCatalog.all().stream().noneMatch(spell -> spell.name().equals(spellName))) {
                throw new IllegalArgumentException("Unknown spell: " + spellName);
            }
        }
    }

    public static CharacterState defaults() {
        return new CharacterState(1, 17, SorcererRules.Subclass.ABERRANT, Set.of());
    }

    public java.util.Map<String, String> equippedFeats() {
        return CharacterChoices.feats(play.sheet(), species, play.feats());
    }

    public java.util.List<ClassProgression> classes() {
        return ClassProgression.from(this);
    }

    public ClassId classId() {
        return classes().getFirst().classId();
    }

    public static CharacterState create(String name, ClassId id) {
        CharacterSheet sheet =
                CharacterSheet.defaults()
                        .withField(CharacterSheet.Field.AUTOMATIC_BACKGROUND_ABILITIES, 1);
        if (id == ClassId.FIGHTER)
            sheet =
                    sheet.withChoice("class.progression", java.util.List.of("FIGHTER:1:CHAMPION"))
                            .withAbility(CharacterSheet.Ability.STR, 16)
                            .withAbility(CharacterSheet.Ability.CON, 14);
        else if (id != ClassId.SORCERER)
            throw new IllegalArgumentException("Class is not implemented.");
        return new CharacterState(
                1,
                id == ClassId.SORCERER ? 17 : 10,
                SorcererRules.Subclass.ABERRANT,
                Set.of(),
                Species.HUMAN,
                PlayState.defaults().withSheet(sheet),
                name);
    }
}
