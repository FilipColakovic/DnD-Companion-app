package dndcompanion.domain.character;

import java.util.List;

/** 2024 Fighter/Champion progression; trigger-dependent effects remain player confirmed. */
public final class FighterRules implements ClassRules {
    public List<Integer> spellSlots(ClassProgression p) {
        return java.util.Collections.nCopies(9, 0);
    }

    public int cantrips(ClassProgression p) {
        return 0;
    }

    public int prepared(ClassProgression p) {
        return 0;
    }

    public int hitDie() {
        return 10;
    }

    public static int secondWind(int level) {
        return level >= 10 ? 4 : level >= 4 ? 3 : 2;
    }

    public static int actionSurge(int level) {
        return level >= 17 ? 2 : level >= 2 ? 1 : 0;
    }

    public static int indomitable(int level) {
        return level >= 17 ? 3 : level >= 13 ? 2 : level >= 9 ? 1 : 0;
    }

    public static int masteryCount(int level) {
        return level >= 16 ? 6 : level >= 10 ? 5 : level >= 4 ? 4 : 3;
    }

    public static int attacks(int level) {
        return level >= 20 ? 4 : level >= 11 ? 3 : level >= 5 ? 2 : 1;
    }

    public static int criticalThreshold(int level) {
        return level >= 15 ? 18 : level >= 3 ? 19 : 20;
    }

    public static List<Integer> featLevels() {
        return List.of(4, 6, 8, 12, 14, 16, 19);
    }

    public static String features(int level) {
        String text =
                "Fighter · "
                        + attacks(level)
                        + " attack(s) per Attack action.\nWeapon Mastery: "
                        + masteryCount(level)
                        + " weapons.\n"
                        + "Second Wind: Bonus Action, heal 1d10 + Fighter level. Short Rest"
                        + " restores one use.";
        if (level >= 2)
            text +=
                    "\n"
                            + "Action Surge: additional action except Magic; once per turn.\n"
                            + "Tactical Mind: on a failed ability check add 1d10; spend Second Wind"
                            + " only if the check succeeds.";
        if (level >= 3)
            text +=
                    "\nChampion: weapon/unarmed critical on "
                            + criticalThreshold(level)
                            + "–20; advantage on initiative and STR (Athletics). After a critical,"
                            + " move half speed without opportunity attacks.";
        if (level >= 5)
            text +=
                    "\n"
                            + "Tactical Shift: Second Wind also lets you move half speed without"
                            + " opportunity attacks.";
        if (level >= 9)
            text +=
                    "\n"
                        + "Indomitable: reroll a failed save, adding Fighter level; use the new"
                        + " roll.\n"
                        + "Tactical Master: replace a mastered weapon's property with Push, Sap or"
                        + " Slow for an attack.";
        if (level >= 10)
            text +=
                    "\n"
                        + "Heroic Warrior: during combat, gain Heroic Inspiration at your turn's"
                        + " start if you have none.";
        if (level >= 13)
            text +=
                    "\n"
                        + "Studied Attacks: after missing, advantage on your next attack against"
                        + " that creature before your next turn ends.";
        if (level >= 18)
            text +=
                    "\n"
                        + "Survivor: advantage on death saves; natural 18–20 counts as 20. At your"
                        + " turn's start, if bloodied and above 0 HP, heal 5 + CON modifier.";
        return text;
    }
}
