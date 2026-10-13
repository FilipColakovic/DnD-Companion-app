package org.example.domain.spells;

import java.util.Random;

/** Checks actual spell dice, conditional fallbacks, critical modifiers, and capped explosions. */
public final class DamageDiceChecks {
    public static void main(String[] args) {
        var fireball = profile("Fireball", 5, 9);
        check(fireball.count() == 10 && fireball.sides() == 6, "Fireball upcasts to 10d6");
        var bolt = profile("Fire Bolt", 0, 17);
        check(bolt.count() == 4 && bolt.sides() == 10, "Cantrips scale with character level");
        var orb = profile("Chromatic Orb", 3, 5);
        check(orb.count() == 5 && orb.sides() == 8, "Chromatic Orb keeps d8s when upcast");
        var missile = profile("Magic Missile", 4, 7);
        check(
                missile.count() == 1
                        && missile.modifier() == 1
                        && missile.scope().equals("per dart"),
                "Magic Missile labels its per-dart roll");
        check(
                profile("Scorching Ray", 4, 7).scope().equals("per ray"),
                "Scorching Ray labels each ray's damage");
        var critical = missile.roll(new Random(19), true, 0);
        check(
                critical.dice().size() == 2
                        && critical.total()
                                == critical.dice().stream().mapToInt(Integer::intValue).sum() + 1,
                "A critical doubles dice and adds the modifier once");
        for (String complex :
                new String[] {
                    "Ice Knife",
                    "Vitriolic Sphere",
                    "Witch Bolt",
                    "True Strike",
                    "Alter Self",
                    "Summon Aberration",
                    "Shield"
                }) {
            check(
                    DamageDice.profile(spell(complex), Math.max(1, spell(complex).level()), 9)
                            .isEmpty(),
                    "Complex or non-damage spell falls back: " + complex);
        }
        var burst = profile("Sorcerous Burst", 0, 1);
        Random allEights =
                new Random() {
                    @Override
                    public int nextInt(int origin, int bound) {
                        return bound - 1;
                    }
                };
        var exploding = burst.roll(allEights, false, 3);
        check(
                exploding.dice().size() == 4 && exploding.total() == 32,
                "Sorcerous Burst explodes only to the modifier cap");
        check(
                burst.roll(allEights, false, 0).dice().size() == 1,
                "No bonus exploding dice when the modifier is zero");
        System.out.println(
                "Passed: spell damage profiles, upcasting, cantrip scaling, per-instance labels,"
                    + " critical modifiers, and capped exploding dice.");
    }

    private static DamageDice.Formula profile(String name, int slot, int level) {
        return DamageDice.profile(spell(name), slot, level)
                .orElseThrow(() -> new AssertionError("Missing profile: " + name));
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
