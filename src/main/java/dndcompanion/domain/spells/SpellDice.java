package dndcompanion.domain.spells;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/** Caster dice only: target saves, penalized enemy rolls, and summon stat blocks are excluded. */
public final class SpellDice {
    private SpellDice() {}

    public static List<Integer> forSpell(
            SpellCatalog.Spell spell, int slotLevel, int characterLevel) {
        if (spell == null) return List.of();
        Set<Integer> dice = new TreeSet<>();
        if (SpellMechanics.hasAttack(spell)) dice.add(20);
        DamageDice.profile(spell, slotLevel, characterLevel)
                .ifPresent(profile -> dice.add(profile.sides()));
        HealingDice.profile(spell, slotLevel, characterLevel)
                .ifPresent(profile -> dice.add(profile.sides()));
        switch (spell.name()) {
            case "Bigby's Hand" -> {
                dice.add(6);
                dice.add(8);
            }
            case "Ice Knife", "Ice Storm" -> {
                dice.add(6);
                dice.add(10);
            }
            case "Prismatic Spray" -> {
                dice.add(6);
                dice.add(8);
            }
            case "Teleport" -> {
                dice.add(10);
                dice.add(100);
            }
            case "Witch Bolt" -> dice.add(12);
            case "Fire Shield" -> dice.add(8);
            case "Vitriolic Sphere", "False Life", "Time Stop" -> dice.add(4);
            case "Blink",
                    "Mirror Image",
                    "Arcane Vigor",
                    "Delayed Blast Fireball",
                    "Hunger of Hadar",
                    "Meteor Swarm",
                    "Evard's Black Tentacles",
                    "Dimension Door",
                    "Earthquake" ->
                    dice.add(6);
            case "Power Word Kill" -> dice.add(12);
            case "Dispel Magic" -> dice.add(20);
            case "True Strike" -> {
                if (characterLevel >= 5) dice.add(6);
            }
            case "Toll the Dead" -> {
                dice.add(8);
                dice.add(12);
            }
            default -> {}
        }
        return List.copyOf(dice);
    }

    public static String note(SpellCatalog.Spell spell) {
        return switch (spell.name()) {
            case "Blink" ->
                    "Roll 1d6 at the end of your turn; 4–6 shifts you to the Ethereal Plane.";
            case "Mirror Image" ->
                    "When hit, roll a d6 per remaining duplicate; any 3+ destroys a duplicate"
                            + " instead.";
            case "False Life" -> "Temporary HP: 2d4 + 4, plus 5 per slot level above 1.";
            case "Arcane Vigor" ->
                    "Roll spent Hit Point Dice (d6 for a single-class Sorcerer), then add your"
                            + " spellcasting modifier.";
            case "Time Stop" -> "Roll 1d4 + 1 for your extra turns.";
            case "Dispel Magic" ->
                    "Only unresolved higher-level spells require a d20 + spellcasting ability"
                            + " modifier check.";
            case "Cure Wounds", "Healing Word" ->
                    "Roll healing with the selected spellcasting ability; Healer rerolls each"
                            + " initial 1 once.";
            default -> SpellMechanics.damageNote(spell);
        };
    }
}
