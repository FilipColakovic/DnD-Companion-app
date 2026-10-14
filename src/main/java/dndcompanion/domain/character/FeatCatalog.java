package dndcompanion.domain.character;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** PHB 2024 only. Original short summaries; source links provide complete published rules. */
public final class FeatCatalog {
    public enum Category {
        ORIGIN("Origin"),
        GENERAL("General"),
        FIGHTING_STYLE("Fighting Style"),
        EPIC_BOON("Epic Boon");
        private final String label;

        Category(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }

    public record Feat(
            String name,
            Category category,
            String prerequisite,
            String abilityIncrease,
            String summary) {
        public int minimumLevel() {
            return category == Category.GENERAL ? 4 : category == Category.EPIC_BOON ? 19 : 1;
        }

        public String sourceUrl() {
            return "https://roll20.net/compendium/dnd5e/Feats%3A"
                    + URLEncoder.encode(name, StandardCharsets.UTF_8).replace("+", "%20")
                    + "?expansion=32231";
        }

        public boolean repeatable() {
            return Set.of(
                            "Ability Score Improvement",
                            "Elemental Adept",
                            "Magic Initiate",
                            "Skilled")
                    .contains(name);
        }

        public String requirements(int level, int charisma) {
            if (level < minimumLevel())
                return "Requires level " + minimumLevel() + " (current: " + level + ")";
            if (category == Category.FIGHTING_STYLE)
                return "Requires the Fighting Style feature; single-class Sorcerers do not have"
                        + " it.";
            if (name.equals("Actor") && charisma < 13)
                return "Requires Charisma 13 (current: " + charisma + ")";
            return "";
        }

        public boolean needsConfirmation(int charisma) {
            if (prerequisite.isEmpty() || prerequisite.equals("Spellcasting")) return false;
            if (prerequisite.equals("CHA 13")) return false;
            if (prerequisite.contains("CHA") && charisma >= 13) return false;
            return true;
        }
    }

    private static final List<Feat> ALL =
            FeatCatalog.DATA
                    .lines()
                    .filter(s -> !s.isBlank())
                    .map(
                            line -> {
                                String[] values = line.split("\\|", -1);
                                return new Feat(
                                        values[0],
                                        Category.valueOf(values[1]),
                                        values[2],
                                        values[3],
                                        values[4]);
                            })
                    .sorted(Comparator.comparing(Feat::name))
                    .toList();

    public static List<Feat> all() {
        return ALL;
    }

    public static Feat named(String name) {
        return ALL.stream()
                .filter(f -> f.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown PHB 2024 feat: " + name));
    }

    public static int nextMilestone(int level) {
        return java.util.stream.IntStream.of(4, 8, 12, 16, 19)
                .filter(n -> n > level)
                .findFirst()
                .orElse(0);
    }

    private FeatCatalog() {}

    private static final String DATA =
"""
Ability Score Improvement|GENERAL||Any +2, or two different abilities +1; maximum 20|Improve ability scores. This feat can be taken again at later eligible milestones.
Actor|GENERAL|CHA 13|CHA +1; maximum 20|Impersonate people more convincingly and imitate voices or other creature sounds.
Alert|ORIGIN|||Add proficiency to initiative and optionally exchange initiative with a willing ally.
Archery|FIGHTING_STYLE|Fighting Style feature||Ranged weapon attacks receive a +2 attack bonus.
Athlete|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|Improve climbing, jumping and recovering from being prone.
Blind Fighting|FIGHTING_STYLE|Fighting Style feature||Gain Blindsight within 10 feet.
Boon of Combat Prowess|EPIC_BOON||Any +1; maximum 30|Turn an attack miss into a hit; this benefit refreshes at the start of your next turn.
Boon of Dimensional Travel|EPIC_BOON||Any +1; maximum 30|Teleport up to 30 feet immediately after taking the Attack or Magic action.
Boon of Energy Resistance|EPIC_BOON||Any +1; maximum 30|Choose damage resistances and use your reaction to redirect certain elemental damage.
Boon of Fate|EPIC_BOON||Any +1; maximum 30|Alter a nearby D20 Test with a 2d4 bonus or penalty. Refresh on initiative or a rest.
Boon of Fortitude|EPIC_BOON||Any +1; maximum 30|Raise maximum HP by 40 and add Constitution-based extra healing once per turn.
Boon of Irresistible Offense|EPIC_BOON||STR or DEX +1; maximum 30|Physical damage bypasses resistance; a natural 20 can deal additional damage.
Boon of Recovery|EPIC_BOON||Any +1; maximum 30|Recover instead of falling to 0 HP once per Long Rest, and spend a daily pool of healing dice.
Boon of Skill|EPIC_BOON||Any +1; maximum 30|Gain broad skill proficiency and expertise in a skill of your choice.
Boon of Speed|EPIC_BOON||Any +1; maximum 30|Gain 30 feet of speed and use a Bonus Action to Disengage and escape a grapple.
Boon of Spell Recall|EPIC_BOON|Spellcasting|INT, WIS or CHA +1; maximum 30|When spending a level 1–4 slot, a matching d4 result preserves that slot.
Boon of the Night Spirit|EPIC_BOON||Any +1; maximum 30|Become invisible in dim light or darkness; those lighting conditions also grant extensive damage resistance.
Boon of Truesight|EPIC_BOON||Any +1; maximum 30|Gain Truesight within 60 feet.
Charger|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|Improve Dash movement and enhance a hit after charging toward a target.
Chef|GENERAL||CON or WIS +1; maximum 20|Learn cooking tools, prepare restorative meals and create treats granting temporary HP.
Crafter|ORIGIN|||Learn three crafting tools, receive a nonmagical shopping discount and craft temporary gear during a Long Rest.
Crossbow Expert|GENERAL|DEX 13|DEX +1; maximum 20|Ignore crossbow Loading, fire near enemies without that disadvantage and improve Light crossbow extra attacks.
Crusher|GENERAL||STR or CON +1; maximum 20|Bludgeoning hits can push a target; bludgeoning critical hits grant attacks against it Advantage.
Defense|FIGHTING_STYLE|Fighting Style feature||Gain +1 AC while wearing armor.
Defensive Duelist|GENERAL|DEX 13|DEX +1; maximum 20|Use a finesse weapon and your reaction to improve defense against melee attacks.
Dual Wielder|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|Gain a conditional Bonus Action attack with a second melee weapon and draw or stow two weapons together.
Dueling|FIGHTING_STYLE|Fighting Style feature||Increase damage when fighting with an eligible one-handed melee weapon.
Durable|GENERAL||CON +1; maximum 20|Gain Advantage on death saves and spend a Hit Die to heal as a Bonus Action.
Elemental Adept|GENERAL|Spellcasting|INT, WIS or CHA +1; maximum 20|Choose an elemental type: its spell damage ignores resistance and damage dice showing 1 count as 2.
Fey-Touched|GENERAL||INT, WIS or CHA +1; maximum 20|Prepare Misty Step and a level 1 Divination or Enchantment spell. Each has one free cast per Long Rest.
Grappler|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|Combine a punch and grapple, attack your grappled targets with Advantage and move eligible grappled creatures more easily.
Great Weapon Fighting|FIGHTING_STYLE|Fighting Style feature||For qualifying two-handed melee weapon damage, treat a die result of 1 or 2 as 3.
Great Weapon Master|GENERAL|STR 13|STR +1; maximum 20|Heavy weapon hits during the Attack action gain proficiency-based damage; certain kills and critical hits permit another attack.
Healer|ORIGIN|||Use a healer's kit and the patient's Hit Die to restore HP; reroll healing dice showing 1.
Heavily Armored|GENERAL|Medium armor training|CON or STR +1; maximum 20|Gain training with heavy armor.
Heavy Armor Master|GENERAL|Heavy armor training|CON or STR +1; maximum 20|While wearing heavy armor, reduce incoming physical attack damage by your proficiency bonus.
Inspiring Leader|GENERAL|WIS or CHA 13|WIS or CHA +1; maximum 20|After a rest, an inspiring performance grants temporary HP to up to six nearby allies.
Interception|FIGHTING_STYLE|Fighting Style feature||With a weapon or shield held, use a reaction to reduce damage to a nearby ally by 1d10 plus proficiency.
Keen Mind|GENERAL|INT 13|INT +1; maximum 20|Improve proficiency or expertise in a knowledge skill and take Study as a Bonus Action.
Lightly Armored|GENERAL||STR or DEX +1; maximum 20|Gain training with light armor and shields.
Lucky|ORIGIN|||Spend daily Luck Points to gain Advantage on your D20 Test or impose Disadvantage on an attack against you.
Mage Slayer|GENERAL||STR or DEX +1; maximum 20|Disrupt enemy concentration and resist a failed mental saving throw using a rest-limited benefit.
Magic Initiate|ORIGIN|||Choose Cleric, Druid or Wizard spells: two cantrips and a prepared level 1 spell with a daily free cast.
Martial Weapon Training|GENERAL||STR or DEX +1; maximum 20|Gain proficiency with martial weapons.
Medium Armor Master|GENERAL|Medium armor training|STR or DEX +1; maximum 20|With Dexterity 16 or higher, medium armor can use a +3 Dexterity contribution to AC.
Moderately Armored|GENERAL|Light armor training|STR or DEX +1; maximum 20|Gain training with medium armor.
Mounted Combatant|GENERAL||STR, DEX or WIS +1; maximum 20|Improve mounted attacks, protect your mount from certain Dexterity-save damage and redirect attacks toward yourself.
Musician|ORIGIN|||Learn three instruments and grant Heroic Inspiration to a proficiency-based number of allies after a rest.
Observant|GENERAL|INT or WIS 13|INT or WIS +1; maximum 20|Improve an observation-related skill and use Search as a Bonus Action.
Piercer|GENERAL||STR or DEX +1; maximum 20|Improve piercing damage through a limited reroll and an additional critical-hit damage die.
Poisoner|GENERAL||DEX or INT +1; maximum 20|Prepare and apply poison efficiently; improve poison effectiveness against resistant targets.
Polearm Master|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|Eligible polearms allow a Bonus Action opposite-end strike and a reaction attack when a target enters reach.
Protection|FIGHTING_STYLE|Fighting Style feature||Interpose a shield as a reaction to disadvantage attacks against a nearby ally while you remain close.
Resilient|GENERAL||Chosen ability +1; maximum 20|Choose an ability whose saving throws you are not proficient in; gain proficiency in those saves.
Ritual Caster|GENERAL|INT, WIS or CHA 13|INT, WIS or CHA +1; maximum 20|Prepare proficiency-based level 1 ritual spells and cast one prepared ritual at normal speed without a slot per Long Rest.
Savage Attacker|ORIGIN|||On one weapon hit per turn, roll the weapon's damage dice twice and choose which result to use.
Sentinel|GENERAL|STR or DEX 13|STR or DEX +1; maximum 20|React to nearby Disengage or attacks against allies; opportunity hits can stop the target's movement.
Shadow-Touched|GENERAL||INT, WIS or CHA +1; maximum 20|Prepare Invisibility and a level 1 Illusion or Necromancy spell. Each has a daily free cast.
Sharpshooter|GENERAL|DEX 13|DEX +1; maximum 20|Ranged weapon attacks bypass partial cover and avoid penalties from long range and nearby enemies.
Shield Master|GENERAL|Shield training|STR +1; maximum 20|Bash after an eligible weapon hit and react to avoid damage after a successful Dexterity save while holding a shield.
Skilled|ORIGIN|||Choose any three skill or tool proficiencies.
Skill Expert|GENERAL||Any +1; maximum 20|Learn a skill and gain expertise in a skill you already know.
Skulker|GENERAL|DEX 13|DEX +1; maximum 20|Gain short-range Blindsight, improve combat hiding and remain hidden after a missed attack.
Slasher|GENERAL||STR or DEX +1; maximum 20|Slashing hits can slow a target; slashing critical hits impose Disadvantage on its attacks.
Speedy|GENERAL|DEX or CON 13|DEX or CON +1; maximum 20|Move faster, Dash through difficult terrain and make opportunity attacks against you less accurate.
Spell Sniper|GENERAL|Spellcasting|INT, WIS or CHA +1; maximum 20|Spell attacks bypass partial cover, avoid nearby-enemy disadvantage and extend eligible attack-spell range by 60 feet.
Tavern Brawler|ORIGIN|||Improve unarmed damage, reroll its damage dice showing 1, use improvised weapons and push on an eligible unarmed hit.
Telekinetic|GENERAL||INT, WIS or CHA +1; maximum 20|Learn an invisible, component-free Mage Hand with extended range. A Bonus Action shove permits a Strength save.
Telepathic|GENERAL||INT, WIS or CHA +1; maximum 20|Communicate telepathically with visible creatures within 60 feet and prepare Detect Thoughts with a daily free cast.
Thrown Weapon Fighting|FIGHTING_STYLE|Fighting Style feature||Increase damage from ranged attacks made with thrown weapons.
Tough|ORIGIN|||Maximum HP gains 2 per character level, including levels already gained.
Two-Weapon Fighting|FIGHTING_STYLE|Fighting Style feature||Add the appropriate ability modifier to a Light weapon's extra attack damage when it is not already included.
Unarmed Fighting|FIGHTING_STYLE|Fighting Style feature||Improve unarmed damage and deal recurring damage to one creature you are grappling.
War Caster|GENERAL|Spellcasting|INT, WIS or CHA +1; maximum 20|Gain Advantage on concentration saves, perform somatic components with occupied hands and replace certain opportunity attacks with a single-target Action spell.
Weapon Master|GENERAL||STR or DEX +1; maximum 20|Use the mastery of one proficient weapon kind; you may change that choice after a Long Rest.
""";
}
