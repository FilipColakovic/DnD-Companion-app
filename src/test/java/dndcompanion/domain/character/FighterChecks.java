package dndcompanion.domain.character;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterSheet.Skill;

import java.util.List;
import java.util.Random;
import java.util.Set;

public final class FighterChecks {
    public static void main(String[] args) {
        for (int level = 1; level <= 20; level++) {
            try (var controller =
                    new CharacterController(CharacterState.create("Fighter", ClassId.FIGHTER))) {
                controller.setLevel(level);
                var session = controller.playSession();
                check(
                        session.maxHitPoints() == 12 + (level - 1) * 8,
                        "Fighter d10/fixed HP: " + level);
                check(
                        session.maxPoints() == 0
                                && session.resources().stream()
                                        .noneMatch(p -> p.id().startsWith("sorcerer")),
                        "No Sorcerer resources");
                check(
                        java.util.stream.IntStream.rangeClosed(1, 9)
                                .allMatch(n -> session.maxSlots(n) == 0),
                        "No class spell slots");
                check(
                        session.selection().limit(true) == 0
                                && session.selection().limit(false) == 0,
                        "No manual Sorcerer learning");
                check(
                        session.saveProficiency(Ability.STR)
                                && session.saveProficiency(Ability.CON)
                                && !session.saveProficiency(Ability.CHA),
                        "Fighter saves");
                check(
                        session.requiredChoices().stream()
                                .noneMatch(c -> c.id().equals("sorcerer.skills")),
                        "Class-specific decisions");
                int mastery = FighterRules.masteryCount(level);
                check(
                        session.requiredChoices().stream()
                                .anyMatch(
                                        c ->
                                                c.id().equals("fighter.mastery")
                                                        && c.count() == mastery),
                        "Mastery progression");
                check(
                        !session.toggleInnate().isEmpty()
                                && !session.choose(Metamagic.SUBTLE, true).isEmpty(),
                        "Sorcerer features rejected");
                var snapshot = controller.snapshot();
                try (var restored = new CharacterController(snapshot)) {
                    check(
                            restored.snapshot().equals(snapshot)
                                    && !restored.playSession().sorcerer(),
                            "Fighter restoration");
                }
            }
        }
        try (var controller =
                new CharacterController(CharacterState.create("Champion", ClassId.FIGHTER))) {
            controller.setLevel(18);
            var session = controller.playSession();
            session.setSheet(
                    session.sheet()
                            .withChoice("fighter.skills", List.of("Athletics", "Perception"))
                            .withChoice("fighter.style", List.of("Defense"))
                            .withChoice("equipment.armor", List.of("Chain Mail")));
            check(session.skillTier(Skill.ATHLETICS) == 1, "Fighter skills grant proficiency");
            check(
                    session.armorClass(10) == 17 && session.equipmentCastingError().isEmpty(),
                    "All armor training and Defense style");
            session.setCurrentHitPoints(10);
            int[] notifications = {0};
            session.addListener(() -> notifications[0]++);
            check(
                    session.spendHitDie(10, new Random(1)).error().isEmpty(),
                    "Spend Fighter d10 Hit Die");
            check(
                    notifications[0] == 1
                            && session.sheet().field(CharacterSheet.Field.SPENT_HIT_DICE) == 1,
                    "Healing and spending a Hit Die are one completed operation");
            var wind = action("secondWind");
            int before = session.hitPoints();
            check(
                    session.useClassAction(wind, 10, new Random(1), 0, 10).error().isEmpty()
                            && session.hitPoints() > before,
                    "Second Wind heals");
            check(session.resource("fighter.secondWind").remaining() == 3, "Second Wind spent");
            session.shortRest();
            check(
                    session.resource("fighter.secondWind").remaining() == 4,
                    "Short Rest restores one Second Wind");
            check(
                    session.useClassAction(action("tacticalMind"), 10, new Random(1), 0, 100)
                            .error()
                            .isEmpty(),
                    "Tactical Mind roll");
            check(
                    session.resource("fighter.secondWind").remaining() == 4,
                    "Failed Tactical Mind retains use");
            session.useClassAction(action("actionSurge"), 10, new Random(1), 0, 10);
            session.useClassAction(action("indomitable"), 10, new Random(1), 2, 10);
            session.shortRest();
            check(
                    session.resource("fighter.actionSurge").remaining() == 2
                            && session.resource("fighter.indomitable").remaining() == 2,
                    "Correct per-pool rest recovery");
            session.longRest();
            check(
                    session.resource("fighter.indomitable").remaining() == 3,
                    "Long Rest restores Indomitable");
            check(
                    FighterRules.criticalThreshold(3) == 19
                            && FighterRules.criticalThreshold(15) == 18
                            && FighterRules.attacks(20) == 4,
                    "Champion critical and Extra Attack progression");
            check(
                    session.equipFeat(FeatCatalog.named("Telekinetic"), "", 10, true).isEmpty(),
                    "Fighter can acquire a spell-granting feat");
            check(
                    session.equipFeat(FeatCatalog.named("Fey-Touched"), "", 10, true).isEmpty(),
                    "Acquire a daily spell source");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Telekinetic.ability", List.of("INT"))
                            .withChoice("feat.Fey-Touched.ability", List.of("WIS"))
                            .withChoice("feat.Fey-Touched.spell", List.of("Bless")));
            var hand =
                    dndcompanion.domain.spells.SpellCatalog.all().stream()
                            .filter(s -> s.name().equals("Mage Hand"))
                            .findFirst()
                            .orElseThrow();
            var step =
                    dndcompanion.domain.spells.SpellCatalog.all().stream()
                            .filter(s -> s.name().equals("Misty Step"))
                            .findFirst()
                            .orElseThrow();
            check(
                    session.castFromSource(hand, 0, "Cantrip", Set.of()).isEmpty(),
                    "Champion can cast a granted cantrip in trained armor");
            check(
                    session.castFromSource(step, 2, "Fey-Touched", Set.of()).isEmpty(),
                    "Champion can use a feat's free cast without class spell slots");
            var spent = session.snapshot();
            check(
                    !session.castFromSource(step, 2, "Fey-Touched", Set.of()).isEmpty()
                            && session.snapshot().equals(spent),
                    "Failed repeated free cast spends nothing");
            check(
                    session.maxPoints() == 0 && session.maxSlots(2) == 0,
                    "Granted casting never invents Sorcerer resources");
        }
        System.out.println(
                "Passed: Fighter levels 1–20, Champion, resources/rests, training, choices, casting"
                        + " isolation, healing and save restoration.");
    }

    private static ClassActions.Action action(String id) {
        return ClassActions.fighter().stream()
                .filter(a -> a.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
