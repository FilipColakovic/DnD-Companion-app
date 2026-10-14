package dndcompanion.domain.character;

import java.util.List;

/** Player-confirmed triggers for effects that require a target, a hit, or a rest event. */
public final class FeatActions {
    private FeatActions() {}

    public record Action(
            String feat,
            String id,
            String label,
            String trigger,
            int dailyUses,
            boolean shortRest) {}

    private static final String DATA =
"""
Actor|mimic|Mimic voice|Impersonation: disguised Deception/Performance checks have advantage.|0|false
Alert|swap|Swap initiative|Willing ally in this combat; neither participant incapacitated.|0|false
Athlete|stand|Stand / climb|Standing uses 5 feet; climbing speed equals speed; running jumps need 5 feet run-up.|0|false
Boon of Combat Prowess|hit|Turn miss into hit|One missed attack roll, once per turn.|0|false
Boon of Dimensional Travel|blink|Teleport 30 ft|Immediately after an Attack or Magic action, to a visible empty space.|0|false
Boon of Energy Resistance|redirect|Redirect energy|Reaction after taking a chosen damage type; target within 60 feet, not total cover.|0|false
Boon of Fate|fate|Alter D20 ±2d4|You or visible creature within 60 feet succeeds or fails a D20 Test; refresh on initiative or rest.|1|true
Boon of Fortitude|healing|Fortitude healing|After regaining HP, add CON modifier; once per turn.|0|false
Boon of Irresistible Offense|critical|Critical extra damage|Attack roll natural 20; add increased ability SCORE as damage.|0|false
Boon of Recovery|laststand|Last Stand (self)|When damage would reduce you to zero HP, instead gain 1 plus half maximum HP.|1|false
Boon of Recovery|recover|Recover 1d10 (self)|Bonus Action; spend one die from the ten-d10 daily pool.|10|false
Boon of Speed|escape|Disengage / escape|Bonus Action; Disengage and end your Grappled condition.|0|false
Boon of the Night Spirit|invisible|Become invisible|Bonus Action in dim light/darkness; ends after taking an action, bonus action, or reaction.|0|false
Charger|charge|Charge damage +1d8|Move 10 feet straight toward target immediately before melee Attack-action hit; once on your turn.|0|false
Charger|push|Charge push 10 ft|Same charge trigger; target at most one size larger; choose push instead of damage.|0|false
Chef|meal|Meal healing (self)|Finished Short Rest; ate prepared meal with ingredients/tools and spent at least one Hit Die.|0|false
Chef|treat|Eat treat (self)|Bonus Action; prepared treat is available and less than 8 hours old.|0|false
Chef|cook|Prepare PB treats|One hour or completed Long Rest; ingredients and Cook's Utensils are available.|0|false
Crafter|craft|Fast craft gear|Finished Long Rest with required proficient tools; crafted item lasts until next Long Rest.|1|false
Crusher|push|Push 5 ft|Bludgeoning hit; target at most one size larger; once per turn.|0|false
Crusher|critical|Critical advantage|Bludgeoning critical: attacks against target have advantage until your next turn starts.|0|false
Defensive Duelist|parry|Parry melee attack|Reaction to melee hit while holding a Finesse weapon; bonus lasts until your next turn starts.|0|false
Dual Wielder|extra|Bonus attack|After attacking with a Light weapon, use a different eligible non-Two-Handed melee weapon.|0|false
Durable|recover|Recover d6 (self)|Bonus Action; spend one Sorcerer Hit Die, regain its roll (without CON modifier).|0|false
Grappler|grab|Punch and grapple|Unarmed Attack-action hit; apply damage and grapple, once per turn.|0|false
Great Weapon Master|hew|Hew bonus attack|Immediately after a melee weapon critical or reducing a creature to zero with it.|0|false
Healer|heal|Battlefield Medic (self)|Utilize a Healer's Kit use and spend one of the patient's Hit Dice.|0|false
Inspiring Leader|inspire|Performance (self)|Finished Short or Long Rest; up to six witnesses within 30 feet, including yourself.|1|true
Interception|intercept|Intercept damage|Reaction; ally within 5 feet hit by attack; you hold a shield or Simple/Martial weapon.|0|false
Keen Mind|study|Study as bonus action|Make the appropriate Study check.|0|false
Lucky|advantage|Lucky advantage|When rolling your own D20 Test.|-1|false
Lucky|disadvantage|Lucky disadvantage|When a creature makes an attack roll against you.|-1|false
Mage Slayer|guard|Succeed mental save|After failing INT, WIS, or CHA saving throw.|1|true
Mounted Combatant|veer|Redirect mount hit|Mounted and not incapacitated; attack hit your mount.|0|false
Musician|song|Encouraging song|Finished rest, play a proficient instrument; grant inspiration to PB allies who hear.|0|false
Observant|search|Search as bonus action|Make the appropriate Search check.|0|false
Piercer|reroll|Reroll piercing die|Piercing hit; reroll one damage die, once per turn, use new result.|0|false
Poisoner|brew|Brew PB doses|One hour, Poisoner's Kit, and 50 GP of materials are available.|0|false
Poisoner|poison|Poison hit 2d8|Applied poison dose hit; target fails CON save; dose potency lasts one minute or one hit.|0|false
Polearm Master|pole|Pole strike d4|After Attack action with Quarterstaff, Spear, or Heavy Reach weapon; Bonus Action.|0|false
Polearm Master|react|Reactive strike|Reaction when a creature enters your eligible polearm's reach.|0|false
Protection|protect|Protect ally|Reaction with equipped shield; ally within 5 feet targeted by attack.|0|false
Savage Attacker|savage|Roll damage twice|Weapon hit; once per turn, choose either set of weapon damage dice.|0|false
Sentinel|guard|Guardian opportunity attack|Reaction to nearby creature's Disengage or attack that hits someone other than you.|0|false
Shield Master|bash|Shield bash|Equipped shield; melee weapon Attack-action hit against target within 5 feet; once on your turn.|0|false
Shield Master|interpose|Interpose shield|Reaction with shield; successful DEX save for half damage instead takes no damage.|0|false
Skulker|hide|Hide with advantage|Hide action during combat; short-range Blindsight applies.|0|false
Slasher|slow|Slow target 10 ft|Slashing hit, once per turn; lasts until your next turn starts.|0|false
Slasher|critical|Critical disadvantage|Slashing critical; target's attacks have disadvantage until your next turn starts.|0|false
Speedy|dash|Dash through terrain|Dash ignores difficult terrain for this turn; opportunity attacks against you have disadvantage.|0|false
Tavern Brawler|push|Unarmed push|Unarmed Attack-action hit; push 5 feet, once per turn.|0|false
Telekinetic|shove|Telekinetic shove|Bonus Action; visible creature within 30 feet; STR save, failure moves 5 feet toward or away.|0|false
Telepathic|speech|Telepathic speech|Visible creature within 60 feet; language it knows, no automatic reply.|0|false
Unarmed Fighting|grapple|Grappled damage d4|At start of your turn; one creature grappled by you.|0|false
War Caster|reaction|Reaction spell|Creature provokes opportunity attack by leaving your reach; Action spell targets only that creature. Cast it normally to spend resources.|0|false
""";
    private static final List<Action> ALL =
            DATA.lines()
                    .map(
                            line -> {
                                String[] p = line.split("\\|", -1);
                                return new Action(
                                        p[0],
                                        p[1],
                                        p[2],
                                        p[3],
                                        Integer.parseInt(p[4]),
                                        Boolean.parseBoolean(p[5]));
                            })
                    .toList();

    public static List<Action> all() {
        return ALL;
    }

    public static List<String> craftingItems(List<String> tools) {
        java.util.Map<String, List<String>> recipes =
                java.util.Map.of(
                        "Carpenter's Tools", List.of("Ladder", "Torch"),
                        "Leatherworker's Tools", List.of("Map Case", "Scroll Case", "Pouch"),
                        "Mason's Tools", List.of("Block and Tackle"),
                        "Potter's Tools", List.of("Jug", "Lamp"),
                        "Smith's Tools",
                                List.of(
                                        "Ball Bearings",
                                        "Bucket",
                                        "Caltrops",
                                        "Grappling Hook",
                                        "Iron Pot"),
                        "Tinker's Tools", List.of("Bell", "Shovel", "Tinderbox"),
                        "Weaver's Tools", List.of("Basket", "Rope", "Net", "Tent"),
                        "Woodcarver's Tools", List.of("Club", "Greatclub", "Quarterstaff"));
        return tools.stream()
                .flatMap(tool -> recipes.getOrDefault(tool, List.of()).stream())
                .distinct()
                .sorted()
                .toList();
    }

    public static String key(Action action) {
        // Lucky's two actions share a pool; Recovery's two benefits have separate pools.
        return "feat-use:"
                + action.feat()
                + (action.feat().equals("Boon of Recovery") ? ":" + action.id() : "");
    }
}
