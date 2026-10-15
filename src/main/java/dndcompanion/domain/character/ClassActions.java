package dndcompanion.domain.character;

import java.util.List;

public final class ClassActions {
    private ClassActions() {}

    public record Action(String id, String label, int level, String pool, String trigger) {}

    public static List<Action> fighter() {
        return List.of(
                new Action(
                        "secondWind",
                        "Second Wind",
                        1,
                        "fighter.secondWind",
                        "Bonus Action: heal 1d10 + Fighter level."),
                new Action(
                        "actionSurge",
                        "Action Surge",
                        2,
                        "fighter.actionSurge",
                        "Once this turn: gain one action other than the Magic action."),
                new Action(
                        "tacticalMind",
                        "Tactical Mind",
                        2,
                        "fighter.secondWind",
                        "After a failed ability check: add 1d10. Spend Second Wind only if the new"
                                + " total succeeds."),
                new Action(
                        "indomitable",
                        "Indomitable",
                        9,
                        "fighter.indomitable",
                        "After a failed save: reroll with a bonus equal to Fighter level. Add your"
                                + " normal save modifier too."),
                new Action(
                        "heroicWarrior",
                        "Heroic Warrior",
                        10,
                        "",
                        "At your turn's start during combat, gain Heroic Inspiration if you have"
                                + " none."),
                new Action(
                        "heroicRally",
                        "Heroic Rally",
                        18,
                        "",
                        "At your turn's start, if bloodied and above 0 HP, regain 5 + Constitution"
                                + " modifier HP."));
    }
}
