package org.example.domain.character;

import org.example.domain.spells.SpellCatalog;

import java.util.Set;

/** Persist inputs and manual spell choices; calculated stats and subclass grants are derived. */
public record CharacterState(int level, int charisma, SorcererRules.Subclass subclass,
                             Set<String> chosenSpells, Species species, PlayState play, String name) {
    public CharacterState(int level, int charisma, SorcererRules.Subclass subclass, Set<String> chosenSpells, Species species, PlayState play) {
        this(level, charisma, subclass, chosenSpells, species, play, "Sorcerer");
    }
    public CharacterState(int level, int charisma, SorcererRules.Subclass subclass, Set<String> chosenSpells, Species species) {
        this(level, charisma, subclass, chosenSpells, species, PlayState.defaults());
    }
    public CharacterState(int level, int charisma, SorcererRules.Subclass subclass, Set<String> chosenSpells) {
        this(level, charisma, subclass, chosenSpells, Species.HUMAN);
    }
    public CharacterState {
        SorcererRules.calculate(level, charisma, subclass);
        java.util.Objects.requireNonNull(species);
        java.util.Objects.requireNonNull(play);
        name = java.util.Objects.requireNonNull(name).strip();
        if (name.isEmpty() || name.length() > 60) { throw new IllegalArgumentException("Character name must contain 1–60 characters."); }
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
    public java.util.Map<String,String> equippedFeats(){return CharacterChoices.feats(play.sheet(),species,play.feats());}
}
