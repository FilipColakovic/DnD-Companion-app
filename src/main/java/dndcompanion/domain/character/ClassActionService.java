package dndcompanion.domain.character;

import dndcompanion.domain.character.PlaySession.FeatResult;

import java.util.HashMap;
import java.util.Map;

/**
 * Stages class feature effects; the session commits and notifies only after validation succeeds.
 */
final class ClassActionService {
    private final PlaySession session;
    CharacterSheet sheet;
    final Map<String, Integer> spent;
    int hitPoints;

    ClassActionService(PlaySession session) {
        this.session = session;
        sheet = session.sheet();
        spent = new HashMap<>(session.snapshot().speciesUses());
        hitPoints = session.hitPoints();
    }

    FeatResult execute(
            ClassActions.Action action,
            int charisma,
            java.util.random.RandomGenerator random,
            int original,
            int dc) {
        if (session.sorcerer()
                || !ClassActions.fighter().contains(action)
                || session.selection().level() < action.level())
            return new FeatResult("This class feature is not available.", "");
        if (!action.pool().isBlank() && session.resource(action.pool()).remaining() == 0)
            return new FeatResult("No uses remain until a rest.", "");
        if (action.id().equals("heroicWarrior")
                && sheet.field(CharacterSheet.Field.INSPIRATION) != 0)
            return new FeatResult("You already have Heroic Inspiration.", "");
        if (action.id().equals("heroicRally")
                && (hitPoints == 0 || hitPoints > session.maxHitPoints() / 2))
            return new FeatResult(
                    "Heroic Rally requires at least 1 HP and no more than half maximum HP.", "");
        String event = action.label();
        boolean spend = !action.pool().isBlank();
        int healing = 0;
        switch (action.id()) {
            case "secondWind" -> {
                int die = random.nextInt(1, 11);
                healing = die + session.selection().level();
                event +=
                        ": d10 ["
                                + die
                                + "] + "
                                + session.selection().level()
                                + " = "
                                + healing
                                + " healing"
                                + (session.selection().level() >= 5
                                        ? "; Tactical Shift: move half speed without opportunity"
                                                + " attacks"
                                        : "");
            }
            case "tacticalMind" -> {
                if (dc <= original)
                    return new FeatResult("Enter a failed check total and a higher DC.", "");
                int die = random.nextInt(1, 11);
                spend = original + die >= dc;
                event +=
                        ": "
                                + original
                                + " + d10 ["
                                + die
                                + "] = "
                                + (original + die)
                                + (spend
                                        ? "; success, Second Wind spent"
                                        : "; still failed, Second Wind retained");
            }
            case "indomitable" -> {
                int die = random.nextInt(1, 21);
                event +=
                        ": d20 ["
                                + die
                                + "] + normal modifier "
                                + original
                                + " + Fighter level "
                                + session.selection().level()
                                + " = "
                                + (die + original + session.selection().level());
            }
            case "heroicWarrior" -> sheet = sheet.withField(CharacterSheet.Field.INSPIRATION, 1);
            case "heroicRally" ->
                    healing =
                            Math.max(
                                    0,
                                    5
                                            + CharacterSheet.modifier(
                                                    session.effectiveScore(
                                                            CharacterSheet.Ability.CON, charisma)));
            default -> event += ": " + action.trigger();
        }
        if (spend) spent.merge("resource:" + action.pool(), 1, Integer::sum);
        if (healing > 0) {
            int current = Math.min(session.maxHitPoints(), hitPoints + healing);
            if (session.automaticHp())
                sheet =
                        sheet.withField(
                                        CharacterSheet.Field.HP_DAMAGE,
                                        session.maxHitPoints() - current)
                                .withField(CharacterSheet.Field.HP_ZERO, current == 0 ? 1 : 0);
            else hitPoints = current;
        }
        return new FeatResult("", event);
    }
}
