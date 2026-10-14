package dndcompanion.domain.spells;

import java.util.Set;

/**
 * Audited 2024 spell effects. Mentions of resistance, allies' attacks and stat blocks are not
 * caster effects.
 */
public final class SpellMechanics {
    private SpellMechanics() {}

    private static Set<String> names(String value) {
        return Set.of(value.split("\\|"));
    }

    private static final Set<String> SAVES =
            names(
                    "Acid Splash|Arms of Hadar|Banishment|Bigby's"
                        + " Hand|Blight|Blindness/Deafness|Burning Hands|Calm Emotions|Chain"
                        + " Lightning|Charm Monster|Charm Person|Circle of Death|Cloudkill|Color"
                        + " Spray|Command|Cone of Cold|Confusion|Counterspell|Crown of"
                        + " Madness|Delayed Blast Fireball|Detect Thoughts|Disintegrate|Dissonant"
                        + " Whispers|Dominate Beast|Dominate Monster|Dominate Person|Dragon's"
                        + " Breath|Earthquake|Enlarge/Reduce|Evard's Black Tentacles|Eyebite|Faerie"
                        + " Fire|Fear|Finger of Death|Fire Storm|Fireball|Flaming Sphere|Flesh to"
                        + " Stone|Friends|Grease|Gust of Wind|Hellish Rebuke|Hold Monster|Hold"
                        + " Person|Hunger of Hadar|Hypnotic Pattern|Ice Knife|Ice Storm|Incendiary"
                        + " Cloud|Insect Plague|Levitate|Lightning Bolt|Mass Suggestion|Meteor"
                        + " Swarm|Mind Sliver|Mind Spike|Otiluke's Freezing Sphere|Phantasmal"
                        + " Force|Polymorph|Power Word Stun|Prismatic Spray|Ray of"
                        + " Enfeeblement|Reverse Gravity|Seeming|Shatter|Sleep|Sleet"
                        + " Storm|Slow|Stinking Cloud|Suggestion|Sunbeam|Sunburst|Synaptic"
                        + " Static|Telekinesis|Thunderclap|Thunderwave|Vitriolic Sphere|Wall of"
                        + " Fire|Wall of Stone|Web");
    private static final Set<String> ATTACKS =
            names(
                    "Bigby's Hand|Chill Touch|Chromatic Orb|Fire Bolt|Flame Blade|Ice Knife|Poison"
                            + " Spray|Ray of Frost|Ray of Sickness|Scorching Ray|Shocking"
                            + " Grasp|Sorcerous Burst|True Strike|Vampiric Touch|Witch Bolt");
    private static final Set<String> DAMAGE_ROLLS =
            names(
                    "Acid Splash|Arms of Hadar|Bigby's Hand|Blight|Burning Hands|Chain"
                        + " Lightning|Chill Touch|Chromatic Orb|Circle of Death|Cloud of"
                        + " Daggers|Cloudkill|Cone of Cold|Delayed Blast Fireball|Dimension"
                        + " Door|Disintegrate|Dissonant Whispers|Dragon's Breath|Earthquake|Fire"
                        + " Bolt|Fire Shield|Fire Storm|Fireball|Flame Blade|Flaming Sphere|Finger"
                        + " of Death|Hellish Rebuke|Hunger of Hadar|Ice Knife|Ice Storm|Incendiary"
                        + " Cloud|Insect Plague|Lightning Bolt|Magic Missile|Meteor Swarm|Mind"
                        + " Sliver|Mind Spike|Otiluke's Freezing Sphere|Phantasmal Force|Poison"
                        + " Spray|Power Word Kill|Prismatic Spray|Ray of Frost|Ray of"
                        + " Sickness|Scorching Ray|Shatter|Shocking Grasp|Sorcerous"
                        + " Burst|Sunbeam|Sunburst|Synaptic"
                        + " Static|Teleport|Thunderclap|Thunderwave|True Strike|Vampiric"
                        + " Touch|Vitriolic Sphere|Wall of Fire|Witch Bolt");
    private static final Set<String> ELEMENTAL_DAMAGE =
            names(
                    "Acid Splash|Burning Hands|Chain Lightning|Chromatic Orb|Cloudkill|Cone of"
                        + " Cold|Delayed Blast Fireball|Dragon's Breath|Fire Bolt|Fire Shield|Fire"
                        + " Storm|Fireball|Flame Blade|Flaming Sphere|Hellish Rebuke|Hunger of"
                        + " Hadar|Ice Knife|Ice Storm|Incendiary Cloud|Lightning Bolt|Otiluke's"
                        + " Freezing Sphere|Poison Spray|Prismatic Spray|Ray of Frost|Ray of"
                        + " Sickness|Scorching Ray|Shatter|Shocking Grasp|Sorcerous"
                        + " Burst|Thunderclap|Thunderwave|Vitriolic Sphere|Wall of Fire|Witch"
                        + " Bolt");
    // Explicit additional-target upcasting, not more projectiles, bouncing, area or summon attacks.
    private static final Set<String> ADDITIONAL_TARGETS =
            names(
                    "Banishment|Blindness/Deafness|Chain Lightning|Charm Monster|Charm"
                            + " Person|Command|Enhance Ability|Etherealness|Fly|Freedom of"
                            + " Movement|Gaseous Form|Hold Monster|Hold"
                            + " Person|Invisibility|Jump|Longstrider|Spider Climb");

    public static boolean forcesSave(SpellCatalog.Spell spell) {
        return SAVES.contains(spell.name())
                || Set.of(
                                "Animal Friendship",
                                "Bane",
                                "Compelled Duel",
                                "Entangle",
                                "Ensnaring Strike",
                                "Inflict Wounds",
                                "Sacred Flame",
                                "Sanctuary",
                                "Searing Smite",
                                "Toll the Dead",
                                "Word of Radiance",
                                "Tasha's Hideous Laughter",
                                "Vicious Mockery")
                        .contains(spell.name());
    }

    public static boolean hasAttack(SpellCatalog.Spell spell) {
        return ATTACKS.contains(spell.name())
                || Set.of(
                                "Eldritch Blast",
                                "Guiding Bolt",
                                "Produce Flame",
                                "Starry Wisp",
                                "Thorn Whip")
                        .contains(spell.name());
    }

    public static boolean hasDamageRoll(SpellCatalog.Spell spell) {
        return DAMAGE_ROLLS.contains(spell.name())
                || Set.of(
                                "Eldritch Blast",
                                "Guiding Bolt",
                                "Produce Flame",
                                "Starry Wisp",
                                "Thorn Whip",
                                "Inflict Wounds",
                                "Sacred Flame",
                                "Toll the Dead",
                                "Word of Radiance",
                                "Vicious Mockery")
                        .contains(spell.name());
    }

    public static boolean hasElementalDamage(SpellCatalog.Spell spell) {
        return ELEMENTAL_DAMAGE.contains(spell.name()) || spell.name().equals("Produce Flame");
    }

    public static boolean addsTargets(SpellCatalog.Spell spell) {
        return ADDITIONAL_TARGETS.contains(spell.name())
                || Set.of(
                                "Animal Friendship",
                                "Bane",
                                "Bless",
                                "Heroism",
                                "Tasha's Hideous Laughter")
                        .contains(spell.name());
    }

    public static String damageNote(SpellCatalog.Spell spell) {
        return switch (spell.name()) {
            case "Web" ->
                    "No damage on casting. Restrains creatures that fail a Dexterity save. Only"
                        + " webs ignited by a separate fire deal 2d4 Fire damage when a creature"
                        + " starts its turn in that fire.";
            case "Dimension Door" ->
                    "Teleportation; no normal damage. A blocked arrival causes 4d6 Force damage to"
                            + " the travelers and the teleport fails.";
            case "Teleport" ->
                    "Teleportation; no normal damage. A mishap causes 3d10 Force damage per"
                            + " traveler or object; further mishaps can repeat it.";
            case "Etherealness" ->
                    "No normal damage. Returning inside an occupied space causes Force damage equal"
                            + " to twice the distance shunted in feet.";
            case "Earthquake" ->
                    "Structures take 50 Bludgeoning damage per tremor. Collapsing structures can"
                        + " deal 12d6 Bludgeoning damage to nearby creatures; falling into fissures"
                        + " is resolved separately.";
            case "Power Word Kill" ->
                    "A target with 100 HP or fewer dies; otherwise it takes 12d12 Psychic damage.";
            case "Ice Knife" ->
                    "Hit: 1d10 Piercing. Hit or miss: a Dexterity save against the explosion, 2d6"
                            + " Cold on failure, plus 1d6 Cold per slot level above 1.";
            case "Ice Storm" ->
                    "Dexterity save: 2d10 Bludgeoning + 4d6 Cold; half on success. Only Bludgeoning"
                            + " increases, by 1d10 per slot level above 4.";
            case "Vitriolic Sphere" ->
                    "Failed Dexterity save: 10d4 Acid now + 5d4 Acid at the end of the target's"
                            + " next turn. Success: half the initial damage only. Initial damage"
                            + " increases by 2d4 per slot level above 4.";
            case "Witch Bolt" ->
                    "Initial hit: 2d12 Lightning + 1d12 per slot level above 1. Subsequent Bonus"
                            + " Action: 1d12 Lightning, even if the initial attack missed.";
            case "Hunger of Hadar" ->
                    "Start of turn: 2d6 Cold. End of turn: Dexterity save, 2d6 Acid on failure. Add"
                        + " 1d6 per slot level above 3 to either damage type, chosen when casting.";
            case "Meteor Swarm" ->
                    "Dexterity save: 20d6 Fire + 20d6 Bludgeoning, half on success; a creature in"
                            + " overlapping spheres is affected once.";
            case "Prismatic Spray" ->
                    "Roll the ray for each target. Red–blue deal 12d6 of the listed elemental type;"
                            + " other rays impose conditions. Resolve the Prismatic Rays table.";
            case "Delayed Blast Fireball" ->
                    "Explosion: 12d6 Fire + 1d6 per slot level above 7 + 1d6 per turn ended while"
                            + " concentrating; Dexterity save for half.";
            case "Fire Shield" ->
                    "A melee attacker within 5 feet takes 2d8 Fire (warm shield) or 2d8 Cold (chill"
                            + " shield). The spell also grants the opposite resistance.";
            case "Flame Blade" ->
                    "Melee spell attack: 3d6 Fire + spellcasting ability modifier; add 1d6 per slot"
                            + " level above 2.";
            case "Bigby's Hand" ->
                    "Clenched Fist: 5d8 Force on a spell attack hit (+2d8 per slot level above 5)."
                        + " Grasping Hand crush: 4d6 Bludgeoning + spellcasting ability modifier"
                        + " (+2d6 per higher slot).";
            case "True Strike" ->
                    "Weapon attack and weapon damage use your spellcasting ability modifier. Choose"
                        + " Radiant or the weapon's normal damage type; add 1d6 Radiant at level 5,"
                        + " 2d6 at 11, 3d6 at 17.";
            case "Alter Self" ->
                    "Natural Weapons option changes your Unarmed Strike to 1d6 of the growth's"
                        + " damage type + spellcasting ability modifier. Other options do not deal"
                        + " damage.";
            case "Enlarge/Reduce" ->
                    "Changes the target's weapon and Unarmed Strike damage by +1d4 (Enlarge) or"
                            + " −1d4 (Reduce); this is not a separate spell damage roll.";
            case "Wish" ->
                    "Resolve the chosen Wish effect or replicated spell. Stress damage is a"
                            + " consequence of later casting, not Wish's primary damage.";
            case "Animate Objects", "Summon Aberration", "Summon Construct", "Summon Dragon" ->
                    "The summoned creatures use their own stat-block attacks and effects. Their"
                            + " attacks are not the caster's spell attack or damage rolls.";
            default ->
                    hasDamageRoll(spell)
                            ? "Resolve damage timing, targets and conditions from the spell text."
                            : "No direct damage. Apply the spell's described effects.";
        };
    }
}
