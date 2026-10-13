package org.example.domain.character;

import org.example.application.CharacterController;
import org.example.domain.spells.SpellCatalog;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/** Refactor contracts: rejected operations are atomic and successful casts notify once. */
public final class CleanupRulesChecks {
    public static void main(String[] args) {
        try (var controller =
                new CharacterController(
                        new CharacterState(
                                5,
                                17,
                                SorcererRules.Subclass.WILD_MAGIC,
                                Set.of("Fireball"),
                                Species.HUMAN))) {
            var session = controller.playSession();
            var spell =
                    SpellCatalog.all().stream()
                            .filter(s -> s.name().equals("Fireball"))
                            .findFirst()
                            .orElseThrow();
            session.choose(Metamagic.QUICKENED, true);
            session.setRemainingPoints(0);
            var notifications = new AtomicInteger();
            session.addListener(notifications::incrementAndGet);
            var before = session.snapshot();
            check(
                    session.cast(spell, 3, false, Set.of(Metamagic.QUICKENED))
                            .equals("Not enough Sorcery Points."),
                    "Precise failure message");
            check(
                    before.equals(session.snapshot()) && notifications.get() == 0,
                    "Failed cast spends nothing and does not notify");
            check(
                    !session.cast(spell, -1, false, Set.of()).isEmpty(),
                    "Invalid slot is rejected safely");
            check(before.equals(session.snapshot()), "Invalid slot preserves resources");
            check(session.cast(spell, 3, false, Set.of()).isEmpty(), "Valid cast succeeds");
            check(
                    notifications.get() == 1 && session.remainingSlots(3) == 1,
                    "Valid cast spends one slot and notifies once");
            var original = controller.snapshot();
            controller.markSaved();
            controller.setSaveLocked(true);
            controller.setCharacterName("Experiment");
            check(
                    controller.saveStatusText().startsWith("Experimenting"),
                    "Checkpoint status tracks nonvisual edits");
            controller.restore(original);
            check(
                    controller.snapshot().equals(original) && controller.isSaveLocked(),
                    "Restore preserves checkpoint and protection");
        }
        for (int score = 1; score <= 30; score++)
            for (int proficiency = 2; proficiency <= 6; proficiency++)
                for (int tier = 0; tier <= 2; tier++) {
                    check(
                            SkillRules.bonus(score, proficiency, tier)
                                    == Math.floorDiv(score - 10, 2) + proficiency * tier,
                            "Shared skill calculation");
                }
        System.out.println(
                "Passed: atomic cast failures, single notifications, controller checkpoints and"
                    + " shared skill calculations.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
