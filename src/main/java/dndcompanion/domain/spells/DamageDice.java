package dndcompanion.domain.spells;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.random.RandomGenerator;

/**
 * Reviewed single-component damage profiles. Conditional and multipart effects have explanatory
 * notes instead.
 */
public final class DamageDice {
    private record Definition(
            int count, int sides, int modifier, String type, int perSlot, String scope) {}

    private static final Map<String, Definition> PROFILES = profiles();

    private static Map<String, Definition> profiles() {
        Map<String, Definition> values = new HashMap<>();
        add(values, "Acid Splash", 1, 6, 0, "Acid", 0, "failed save, per creature");
        add(values, "Word of Radiance", 1, 6, 0, "Radiant", 0, "failed save, per chosen creature");
        add(
                values,
                "Sacred Flame",
                1,
                8,
                0,
                "Radiant",
                0,
                "failed save; target gains no cover benefit");
        add(values, "Starry Wisp", 1, 8, 0, "Radiant", 0, "on a hit");
        add(values, "Produce Flame", 1, 8, 0, "Fire", 0, "on a hit");
        add(values, "Guiding Bolt", 4, 6, 0, "Radiant", 1, "on a hit");
        add(values, "Inflict Wounds", 2, 10, 0, "Necrotic", 1, "failed save; half on success");
        add(
                values,
                "Thorn Whip",
                1,
                6,
                0,
                "Piercing",
                0,
                "on a hit; pull eligible target up to 10 feet");
        add(values, "Arms of Hadar", 2, 6, 0, "Necrotic", 1, "failed save; half on success");
        add(values, "Blight", 8, 8, 0, "Necrotic", 1, "failed save; half on success");
        add(values, "Burning Hands", 3, 6, 0, "Fire", 1, "failed save; half on success");
        add(
                values,
                "Chain Lightning",
                10,
                8,
                0,
                "Lightning",
                0,
                "failed save, per target; half on success");
        add(values, "Chill Touch", 1, 10, 0, "Necrotic", 0, "on a hit");
        add(
                values,
                "Chromatic Orb",
                3,
                8,
                0,
                "chosen type",
                1,
                "per hit; leaps require matching dice");
        add(values, "Circle of Death", 8, 8, 0, "Necrotic", 2, "failed save; half on success");
        add(
                values,
                "Cloud of Daggers",
                4,
                4,
                0,
                "Slashing",
                2,
                "per damage trigger; once per creature per turn");
        add(values, "Cloudkill", 5, 8, 0, "Poison", 1, "failed save, per trigger; half on success");
        add(values, "Cone of Cold", 8, 8, 0, "Cold", 1, "failed save; half on success");
        add(values, "Disintegrate", 10, 6, 40, "Force", 3, "failed save");
        add(values, "Dissonant Whispers", 3, 6, 0, "Psychic", 1, "failed save; half on success");
        add(
                values,
                "Dragon's Breath",
                3,
                6,
                0,
                "chosen type",
                1,
                "per breath, failed save; half on success");
        add(values, "Finger of Death", 7, 8, 30, "Necrotic", 0, "failed save; half on success");
        add(values, "Fire Bolt", 1, 10, 0, "Fire", 0, "on a hit");
        add(values, "Fire Storm", 7, 10, 0, "Fire", 0, "failed save; half on success");
        add(values, "Fireball", 8, 6, 0, "Fire", 1, "failed save; half on success");
        add(
                values,
                "Flaming Sphere",
                2,
                6,
                0,
                "Fire",
                1,
                "failed save, per trigger; half on success");
        add(values, "Hellish Rebuke", 2, 10, 0, "Fire", 1, "failed save; half on success");
        add(
                values,
                "Incendiary Cloud",
                10,
                8,
                0,
                "Fire",
                0,
                "failed save, per trigger; half on success");
        add(
                values,
                "Insect Plague",
                4,
                10,
                0,
                "Piercing",
                1,
                "failed save, per trigger; half on success");
        add(values, "Lightning Bolt", 8, 6, 0, "Lightning", 1, "failed save; half on success");
        add(values, "Magic Missile", 1, 4, 1, "Force", 0, "per dart");
        add(values, "Mind Sliver", 1, 6, 0, "Psychic", 0, "failed save");
        add(values, "Mind Spike", 3, 8, 0, "Psychic", 1, "failed save; half on success");
        add(
                values,
                "Otiluke's Freezing Sphere",
                10,
                6,
                0,
                "Cold",
                1,
                "failed save; half on success");
        add(
                values,
                "Phantasmal Force",
                2,
                8,
                0,
                "Psychic",
                0,
                "per damaging phantasm turn; target must be in reach");
        add(values, "Poison Spray", 1, 12, 0, "Poison", 0, "on a hit");
        add(values, "Ray of Frost", 1, 8, 0, "Cold", 0, "on a hit");
        add(values, "Ray of Sickness", 2, 8, 0, "Poison", 1, "on a hit");
        add(values, "Scorching Ray", 2, 6, 0, "Fire", 0, "per ray");
        add(values, "Shatter", 3, 8, 0, "Thunder", 1, "failed save; half on success");
        add(values, "Shocking Grasp", 1, 8, 0, "Lightning", 0, "on a hit");
        add(
                values,
                "Sorcerous Burst",
                1,
                8,
                0,
                "chosen type",
                0,
                "on a hit; extra dice capped by spellcasting modifier");
        add(values, "Sunbeam", 6, 8, 0, "Radiant", 0, "failed save, per beam; half on success");
        add(values, "Sunburst", 12, 6, 0, "Radiant", 0, "failed save; half on success");
        add(values, "Synaptic Static", 8, 6, 0, "Psychic", 0, "failed save; half on success");
        add(values, "Thunderclap", 1, 6, 0, "Thunder", 0, "failed save");
        add(values, "Thunderwave", 2, 8, 0, "Thunder", 1, "failed save; half on success");
        add(
                values,
                "Vampiric Touch",
                3,
                6,
                0,
                "Necrotic",
                1,
                "on a hit; heal half Necrotic damage dealt");
        add(
                values,
                "Wall of Fire",
                5,
                8,
                0,
                "Fire",
                1,
                "per damage trigger; initial save for half");
        return Map.copyOf(values);
    }

    private static void add(
            Map<String, Definition> values,
            String name,
            int count,
            int sides,
            int modifier,
            String type,
            int perSlot,
            String scope) {
        values.put(name, new Definition(count, sides, modifier, type, perSlot, scope));
    }

    private DamageDice() {}

    public record Formula(
            int count, int sides, int modifier, String type, boolean exploding, String scope) {
        public String notation() {
            return count
                    + "d"
                    + sides
                    + (modifier == 0 ? "" : " + " + modifier)
                    + (type.isBlank() ? "" : " " + type)
                    + (exploding ? " (exploding)" : "")
                    + (scope.isBlank() ? "" : " · " + scope);
        }

        public Roll roll(RandomGenerator random, boolean critical, int maximumExtraDice) {
            return roll(random, critical, maximumExtraDice, 1);
        }

        public Roll roll(
                RandomGenerator random, boolean critical, int maximumExtraDice, int minimumDie) {
            if (minimumDie < 1 || minimumDie > sides)
                throw new IllegalArgumentException("Invalid die minimum");
            int initial = count * (critical ? 2 : 1), extra = 0, pending = initial;
            List<Integer> values = new ArrayList<>();
            while (pending-- > 0) {
                int value = Math.max(minimumDie, random.nextInt(1, sides + 1));
                values.add(value);
                if (exploding && value == sides && extra < maximumExtraDice) {
                    pending++;
                    extra++;
                }
            }
            return new Roll(
                    List.copyOf(values),
                    values.stream().mapToInt(Integer::intValue).sum() + modifier,
                    notation(),
                    critical);
        }
    }

    public record Roll(List<Integer> dice, int total, String formula, boolean critical) {}

    public static Optional<Formula> profile(
            SpellCatalog.Spell spell, int effectiveLevel, int characterLevel) {
        Definition base = PROFILES.get(spell.name());
        if (base == null) {
            return Optional.empty();
        }
        int count = base.count();
        if (spell.level() == 0) {
            count *=
                    1
                            + (characterLevel >= 5 ? 1 : 0)
                            + (characterLevel >= 11 ? 1 : 0)
                            + (characterLevel >= 17 ? 1 : 0);
        } else {
            count += base.perSlot() * Math.max(0, effectiveLevel - spell.level());
        }
        return Optional.of(
                new Formula(
                        count,
                        base.sides(),
                        base.modifier(),
                        base.type(),
                        spell.name().equals("Sorcerous Burst"),
                        base.scope()));
    }
}
