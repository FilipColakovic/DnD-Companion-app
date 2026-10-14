package dndcompanion.domain.character;

import dndcompanion.domain.character.CharacterSheet.Ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;

/** Deterministic equipment-dependent math; caller supplies combat circumstances and dice. */
public final class FeatCombatRules {
    private FeatCombatRules() {}

    public record Context(
            boolean critical,
            boolean attackAction,
            boolean twoHanded,
            boolean lightExtra,
            boolean thrown,
            boolean unarmed) {}

    public record WeaponRoll(
            int attackBonus, List<Integer> dice, int modifier, int total, String notes) {}

    public static WeaponRoll weapon(
            PlaySession session, int charisma, Context context, RandomGenerator random) {
        var sheet = session.sheet();
        Set<String> feats = session.feats().keySet();
        var weapon = EquipmentRules.weapon(sheet);
        boolean ranged = weapon.properties().contains("Ranged") || context.thrown();
        int str = session.effectiveScore(Ability.STR, charisma),
                dex = session.effectiveScore(Ability.DEX, charisma);
        int score =
                !context.unarmed()
                                && (ranged && !context.thrown()
                                        || weapon.properties().contains("Finesse"))
                        ? Math.max(weapon.properties().contains("Finesse") ? str : dex, dex)
                        : str;
        int modifier = CharacterSheet.modifier(score),
                pb = 2 + (session.selection().level() - 1) / 4;
        int attack =
                modifier
                        + (context.unarmed()
                                        || !weapon.martial()
                                        || feats.contains("Martial Weapon Training")
                                ? pb
                                : 0)
                        + (!context.unarmed() && ranged && feats.contains("Archery") ? 2 : 0);
        int sides =
                context.unarmed()
                        ? feats.contains("Unarmed Fighting")
                                ? (EquipmentRules.shield(sheet)
                                                || !weapon.properties().contains("Unarmed")
                                                || sheet.choice("equipment.offhand").stream()
                                                        .anyMatch(n -> !n.equals("None"))
                                        ? 6
                                        : 8)
                                : feats.contains("Tavern Brawler") ? 4 : 1
                        : weapon.die();
        if (!context.unarmed() && context.twoHanded() && weapon.properties().contains("Versatile"))
            sides += 2;
        int count = !context.unarmed() && weapon.properties().contains("2d6") ? 2 : 1;
        if (context.critical() && sides > 1) count *= 2;
        if (!context.unarmed()
                && context.critical()
                && weapon.damage().equals("Piercing")
                && feats.contains("Piercer")) count++;
        List<Integer> dice = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int die = sides == 1 ? 1 : random.nextInt(1, sides + 1);
            if (context.unarmed() && feats.contains("Tavern Brawler") && die == 1 && sides > 1)
                die = random.nextInt(1, sides + 1);
            if (!context.unarmed()
                    && !ranged
                    && context.twoHanded()
                    && feats.contains("Great Weapon Fighting")
                    && (weapon.properties().contains("Two-Handed")
                            || weapon.properties().contains("Versatile"))) die = Math.max(3, die);
            dice.add(die);
        }
        if (context.lightExtra()
                && !feats.contains("Two-Weapon Fighting")
                && !(feats.contains("Crossbow Expert") && weapon.name().equals("Hand Crossbow")))
            modifier = Math.min(0, modifier);
        if (!context.unarmed()
                && !ranged
                && !context.twoHanded()
                && feats.contains("Dueling")
                && sheet.choice("equipment.offhand").stream().allMatch(n -> n.equals("None")))
            modifier += 2;
        if (!context.unarmed() && context.thrown() && feats.contains("Thrown Weapon Fighting"))
            modifier += 2;
        if (!context.unarmed()
                && context.attackAction()
                && weapon.properties().contains("Heavy")
                && feats.contains("Great Weapon Master")) modifier += pb;
        String notes = (context.unarmed() ? "Bludgeoning" : weapon.damage());
        if (feats.contains("Boon of Irresistible Offense"))
            notes += "; ignores physical resistance";
        if (!context.unarmed()
                && feats.contains("Weapon Master")
                && (!weapon.martial() || feats.contains("Martial Weapon Training"))
                && sheet.choice("feat.Weapon Master.weapon").contains(weapon.name()))
            notes += "; mastery " + weapon.mastery();
        if (feats.contains("Grappler")) notes += "; advantage against creatures grappled by you";
        if (feats.contains("Sharpshooter") && ranged)
            notes +=
                    "; ignores half/three-quarters cover, long-range and adjacent-enemy"
                            + " disadvantage";
        if (feats.contains("Crossbow Expert") && weapon.name().contains("Crossbow"))
            notes += "; ignore Loading and adjacent-enemy disadvantage";
        return new WeaponRoll(
                attack,
                List.copyOf(dice),
                modifier,
                dice.stream().mapToInt(Integer::intValue).sum() + modifier,
                notes);
    }

    public static int incoming(
            PlaySession session, String type, int damage, boolean attack, boolean dimLight) {
        if (damage < 0) throw new IllegalArgumentException("Damage cannot be negative");
        Set<String> feats = session.feats().keySet();
        int reduced = damage;
        if (attack
                && feats.contains("Heavy Armor Master")
                && EquipmentRules.worn(session.sheet()).category().equals("Heavy")
                && Set.of("Bludgeoning", "Piercing", "Slashing").contains(type))
            reduced = Math.max(0, reduced - (2 + (session.selection().level() - 1) / 4));
        boolean resistance =
                feats.contains("Boon of Energy Resistance")
                                && session.sheet()
                                        .choice("feat.Boon of Energy Resistance.damage")
                                        .contains(type)
                        || feats.contains("Boon of the Night Spirit")
                                && dimLight
                                && !Set.of("Psychic", "Radiant").contains(type);
        if (resistance) reduced /= 2;
        return reduced;
    }
}
