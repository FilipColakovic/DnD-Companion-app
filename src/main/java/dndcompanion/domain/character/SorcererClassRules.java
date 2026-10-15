package dndcompanion.domain.character;

import java.util.List;

public final class SorcererClassRules implements ClassRules {
    public Preparation preparation() {
        return Preparation.LEARNED;
    }

    public java.util.Optional<CharacterSheet.Ability> spellcastingAbility() {
        return java.util.Optional.of(CharacterSheet.Ability.CHA);
    }

    public boolean alwaysPrepared(
            dndcompanion.domain.spells.SpellCatalog.Spell spell, ClassProgression p) {
        return dndcompanion.domain.spells.SpellCatalog.alwaysPrepared(
                spell, p.level(), SorcererRules.Subclass.valueOf(p.subclass()));
    }

    public List<Integer> spellSlots(ClassProgression p) {
        return SorcererRules.calculate(p.level(), 17, SorcererRules.Subclass.valueOf(p.subclass()))
                .spellSlots();
    }

    public int cantrips(ClassProgression p) {
        return SorcererRules.calculate(p.level(), 17, SorcererRules.Subclass.valueOf(p.subclass()))
                .cantrips();
    }

    public int prepared(ClassProgression p) {
        return SorcererRules.calculate(p.level(), 17, SorcererRules.Subclass.valueOf(p.subclass()))
                .basePreparedSpells();
    }

    public int hitDie() {
        return 6;
    }
}
