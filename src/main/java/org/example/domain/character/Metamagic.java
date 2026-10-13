package org.example.domain.character;

import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellMechanics;

/**
 * Original concise summaries of the 2024 options. Eligibility describes spell effects, not targets
 * at the table.
 */
public enum Metamagic {
    CAREFUL(
            "Careful Spell",
            1,
            "Protect up to your Charisma modifier (minimum one) creatures: automatic saves and no"
                    + " half-on-save damage."),
    DISTANT("Distant Spell", 1, "Double a range of at least 5 feet; Touch becomes 30 feet."),
    EMPOWERED(
            "Empowered Spell",
            1,
            "Reroll up to your Charisma modifier (minimum one) damage dice; keep new results."
                    + " Combines with other options."),
    EXTENDED(
            "Extended Spell",
            1,
            "Double durations of at least a minute, capped at 24 hours; gain Advantage on"
                    + " concentration saves."),
    HEIGHTENED(
            "Heightened Spell",
            2,
            "One target has Disadvantage on saving throws against this spell."),
    QUICKENED(
            "Quickened Spell",
            2,
            "Change Action casting to Bonus Action. No other level 1+ spell before or after it this"
                    + " turn."),
    SEEKING(
            "Seeking Spell",
            1,
            "After a spell's attack misses, reroll and keep the result. Combines with other"
                    + " options."),
    SUBTLE(
            "Subtle Spell",
            1,
            "Remove components except consumed materials or materials with a listed price."),
    TRANSMUTED(
            "Transmuted Spell",
            1,
            "Switch spell damage among Acid, Cold, Fire, Lightning, Poison, and Thunder."),
    TWINNED(
            "Twinned Spell",
            1,
            "Add one effective spell level when upcasting permits additional creature targets.");
    private final String title;
    private final int cost;
    private final String description;

    Metamagic(String title, int cost, String description) {
        this.title = title;
        this.cost = cost;
        this.description = description;
    }

    @Override
    public String toString() {
        return title;
    }

    public int cost() {
        return cost;
    }

    public String description() {
        return description;
    }

    public boolean combinable() {
        return this == EMPOWERED || this == SEEKING;
    }

    public static int limit(int level) {
        return level < 2 ? 0 : level < 10 ? 2 : level < 17 ? 4 : 6;
    }

    public boolean applies(SpellCatalog.Spell spell) {
        SpellDetails d = SpellDetails.forSpell(spell);
        return switch (this) {
            case CAREFUL, HEIGHTENED -> SpellMechanics.forcesSave(spell);
            case DISTANT -> d.range().equalsIgnoreCase("Touch") || numericRange(d.range());
            case EMPOWERED -> SpellMechanics.hasDamageRoll(spell);
            case EXTENDED ->
                    spell.name().equals("Creation")
                            || d.duration().matches("(?i).*[1-9][0-9]* (minute|hour|day)s?.*");
            case QUICKENED -> d.castingTime().equalsIgnoreCase("Action");
            case SEEKING -> SpellMechanics.hasAttack(spell);
            case SUBTLE -> true;
            case TRANSMUTED -> SpellMechanics.hasElementalDamage(spell);
            case TWINNED -> SpellMechanics.addsTargets(spell);
        };
    }

    private static boolean numericRange(String range) {
        var match = java.util.regex.Pattern.compile("(?i)^(\\d+) (feet|mile)s?.*").matcher(range);
        return match.matches()
                && (match.group(2).equalsIgnoreCase("mile")
                        || Integer.parseInt(match.group(1)) >= 5);
    }

    /**
     * Explain why the option can be chosen, including conditions that a spell list cannot resolve.
     */
    public String eligibilityNote(SpellCatalog.Spell spell) {
        if (!applies(spell)) {
            return switch (this) {
                case CAREFUL, HEIGHTENED ->
                        "The spell does not force a creature saving throw; granting save bonuses is"
                                + " not enough.";
                case DISTANT ->
                        "Requires Touch or a numeric range of at least 5 feet; Self and Unlimited"
                                + " do not qualify.";
                case EMPOWERED ->
                        "No caster spell-damage dice to reroll. Ally attacks, summoned-creature"
                                + " attacks and externally ignited webs do not qualify.";
                case EXTENDED ->
                        "Requires a spell duration of at least 1 minute; lingering consequences do"
                                + " not change Instantaneous duration.";
                case QUICKENED ->
                        "Requires Action casting; Bonus Action, Reaction and longer casting times"
                                + " do not qualify.";
                case SEEKING -> "The caster does not make an attack roll for this spell.";
                case SUBTLE -> "Use the component rules in the spell description.";
                case TRANSMUTED ->
                        "The spell does not itself deal Acid, Cold, Fire, Lightning, Poison or"
                                + " Thunder damage.";
                case TWINNED ->
                        "Requires explicit additional creature targets when upcast; more damage,"
                                + " area, rays, darts or conditional orb leaps do not qualify.";
            };
        }
        if (this == CAREFUL || this == HEIGHTENED) {
            String condition =
                    switch (spell.name()) {
                        case "Detect Thoughts" ->
                                "Only deeper probing forces the target's Wisdom save. ";
                        case "Enlarge/Reduce", "Levitate", "Seeming" ->
                                "Only an unwilling target makes the relevant saving throw. ";
                        case "Power Word Stun" ->
                                "The initial stun has no saving throw; this applies to the later"
                                        + " Constitution saves. ";
                        case "Web" ->
                                "The Dexterity save occurs when a creature enters the webs or"
                                        + " starts its turn there, not immediately on casting. ";
                        case "Dragon's Breath" ->
                                "This applies to breath-effect Dexterity saves, not to a save by"
                                        + " the willing recipient. ";
                        default -> "";
                    };
            if (!condition.isEmpty())
                return condition
                        + (this == CAREFUL
                                ? "Protect chosen creatures on the spell's saves."
                                : "Choose one target for Disadvantage on saves against the spell.");
        }
        return switch (this) {
            case CAREFUL ->
                    "Protect chosen creatures on the spell's saving throws; no protection from"
                            + " damage without a save.";
            case HEIGHTENED ->
                    "Choose one target to have Disadvantage on its saves against the spell.";
            case DISTANT ->
                    "Extend the spell's range; its area size and separate reaction-trigger distance"
                            + " stay unchanged.";
            case EMPOWERED ->
                    switch (spell.name()) {
                        case "Dimension Door", "Teleport" ->
                                "Only if a teleportation failure causes damage and you roll its"
                                        + " damage dice.";
                        case "Earthquake" ->
                                "Only rolled collapsing-structure damage; fixed 50 damage cannot be"
                                        + " rerolled.";
                        case "Power Word Kill" ->
                                "Only the 12d12 damage against a target above 100 HP; the"
                                        + " instant-death effect has no dice.";
                        case "Dragon's Breath" ->
                                "When you roll this spell's breath damage; not a selected"
                                        + " creature's unrelated attacks.";
                        case "Prismatic Spray" ->
                                "Reroll damage dice of a damaging ray, not the d8 that chooses the"
                                        + " ray.";
                        default ->
                                "Use after rolling spell damage; reroll damage dice, not attack,"
                                        + " save or effect-selection dice.";
                    };
            case EXTENDED ->
                    "Double the spell duration up to 24 hours; concentration saves gain Advantage."
                            + " A duration already at the cap cannot increase.";
            case QUICKENED ->
                    "Change this casting to a Bonus Action; no other level 1+ spell on this turn.";
            case SEEKING ->
                    "Use only after your attack misses; reroll the d20 and keep the new result.";
            case SUBTLE ->
                    "Remove V/S and unpriced, unconsumed M; keep any priced or consumed materials.";
            case TRANSMUTED ->
                    switch (spell.name()) {
                        case "Sorcerous Burst" ->
                                "Only a listed elemental type can be changed; Psychic cannot be"
                                        + " transmuted.";
                        case "Prismatic Spray" ->
                                "Change one listed elemental ray type; condition rays are"
                                        + " unchanged.";
                        case "Ice Knife", "Ice Storm", "Meteor Swarm" ->
                                "Change the elemental damage component only; Piercing and"
                                        + " Bludgeoning stay unchanged.";
                        case "Hunger of Hadar" ->
                                "Choose which listed damage type to replace; the other component"
                                        + " stays unchanged.";
                        default ->
                                "Change a listed elemental damage type; damage dice, targeting and"
                                        + " other effects stay unchanged.";
                    };
            case TWINNED ->
                    "Use the spell's next effective level for its explicit additional targets;"
                            + " spend the selected slot, not a higher one.";
        };
    }
}
