package dndcompanion.domain.character;

import dndcompanion.domain.character.PlaySession.FeatResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** Staged feat action execution. Failed validation never mutates the live session. */
final class FeatActionService {
    private final PlaySession session;
    CharacterSheet sheet;
    final Map<String, Integer> speciesUses;
    private int hp;

    FeatActionService(PlaySession session) {
        this.session = session;
        sheet = session.sheet();
        speciesUses = new HashMap<>(session.snapshot().speciesUses());
        hp = session.hitPoints();
    }

    int hitPoints() {
        return hp;
    }

    int maxHitPoints() {
        return session.maxHitPoints();
    }

    void setHpWithoutNotification(int current) {
        hp = current;
        if (session.automaticHp())
            sheet =
                    sheet.withField(CharacterSheet.Field.HP_DAMAGE, maxHitPoints() - current)
                            .withField(CharacterSheet.Field.HP_ZERO, current == 0 ? 1 : 0);
    }

    FeatResult execute(
            FeatActions.Action action,
            int charisma,
            java.util.random.RandomGenerator random,
            int rerollSides,
            String item) {
        if (!Set.of(1, 4, 6, 8, 10, 12).contains(rerollSides))
            return new FeatResult("Choose a valid damage die.", "");
        if (!FeatActions.all().contains(action) || !session.feats().containsKey(action.feat()))
            return new FeatResult("This feat is not equipped.", "");
        if (action.feat().equals("Telekinetic") && !session.mageHandWasCast())
            return new FeatResult("Cast Mage Hand before using Telekinetic shove.", "");
        if (session.remainingFeatUses(action) == 0)
            return new FeatResult("No uses remain until a rest.", "");
        String feat = action.feat(), id = action.id();
        var weapon = EquipmentRules.weapon(sheet);
        boolean shield = EquipmentRules.shield(sheet);
        if (feat.equals("Defensive Duelist") && !weapon.properties().contains("Finesse"))
            return new FeatResult("Equip a Finesse weapon first.", "");
        if (Set.of("Protection", "Shield Master").contains(feat) && !shield)
            return new FeatResult("Equip a shield first.", "");
        if (feat.equals("Polearm Master")
                && !(Set.of("Quarterstaff", "Spear").contains(weapon.name())
                        || weapon.properties().contains("Heavy")
                                && weapon.properties().contains("Reach")))
            return new FeatResult("Equip an eligible polearm first.", "");
        if (Set.of("Telekinetic", "Poisoner", "Inspiring Leader", "Boon of Irresistible Offense")
                        .contains(feat)
                && session.requiredChoices().stream()
                        .anyMatch(
                                c ->
                                        c.id().equals("feat." + feat + ".ability")
                                                && !c.complete(sheet)))
            return new FeatResult("Choose this feat's ability increase in Character first.", "");
        int pb = 2 + (session.selection().level() - 1) / 4;
        int modifier =
                CharacterSheet.modifier(
                        session.effectiveScore(FeatEffects.ability(sheet, feat), charisma));
        String effect = action.trigger();
        if (feat.equals("Crafter")) {
            if (!FeatActions.craftingItems(session.grantedTools()).contains(item))
                return new FeatResult("Choose gear supported by your proficient tools.", "");
            sheet = sheet.withNote(CharacterSheet.Note.CRAFTED_ITEM, item);
            effect = "Crafted " + item + "; expires at your next Long Rest.";
        }
        int healing = 0;
        if (feat.equals("Durable") || feat.equals("Healer")) {
            if (feat.equals("Healer") && sheet.field(CharacterSheet.Field.HEALER_KIT_USES) == 0)
                return new FeatResult("Add available Healer's Kit uses in Character first.", "");
            if (sheet.field(CharacterSheet.Field.SPENT_HIT_DICE) >= session.selection().level()
                    || hitPoints() <= 0)
                return new FeatResult("No available Hit Dice, or character is at zero HP.", "");
            int hitDie = ClassDefinition.forClass(session.selection().classId()).hitDie();
            int die = random.nextInt(1, hitDie + 1);
            if (feat.equals("Healer") && die == 1) die = random.nextInt(1, hitDie + 1);
            healing = die + (feat.equals("Healer") ? pb : 0);
            sheet =
                    sheet.withField(
                            CharacterSheet.Field.SPENT_HIT_DICE,
                            sheet.field(CharacterSheet.Field.SPENT_HIT_DICE) + 1);
            if (feat.equals("Healer"))
                sheet =
                        sheet.withField(
                                CharacterSheet.Field.HEALER_KIT_USES,
                                sheet.field(CharacterSheet.Field.HEALER_KIT_USES) - 1);
            effect =
                    "d"
                            + hitDie
                            + " ["
                            + die
                            + "]"
                            + (feat.equals("Healer") ? " + PB " + pb : "")
                            + " = "
                            + healing
                            + " healing";
        } else if (feat.equals("Boon of Recovery")) {
            healing = id.equals("recover") ? random.nextInt(1, 11) : 0;
            if (id.equals("laststand"))
                setHpWithoutNotification(Math.min(maxHitPoints(), 1 + maxHitPoints() / 2));
            effect =
                    id.equals("recover")
                            ? "d10 healing " + healing
                            : "Last Stand: HP " + hitPoints();
        } else if (feat.equals("Chef")) {
            if (id.equals("meal")) healing = random.nextInt(1, 9);
            else if (id.equals("cook")) {
                speciesUses.put("feat-stock:Chef", pb);
                effect =
                        "Prepared "
                                + pb
                                + " treats; each grants "
                                + pb
                                + " temporary HP, expires after 8 hours.";
            } else {
                int stock = speciesUses.getOrDefault("feat-stock:Chef", 0);
                if (stock == 0) return new FeatResult("Prepare treats first.", "");
                speciesUses.put("feat-stock:Chef", stock - 1);
                sheet =
                        sheet.withField(
                                CharacterSheet.Field.TEMP_HP,
                                Math.max(sheet.field(CharacterSheet.Field.TEMP_HP), pb));
                effect = "Temporary HP " + pb + "; " + (stock - 1) + " treats remain.";
            }
        } else if (feat.equals("Inspiring Leader")) {
            int temporary = Math.max(0, session.selection().level() + modifier);
            sheet =
                    sheet.withField(
                            CharacterSheet.Field.TEMP_HP,
                            Math.max(sheet.field(CharacterSheet.Field.TEMP_HP), temporary));
            effect =
                    temporary
                            + " temporary HP for up to six witnesses within 30 feet (including"
                            + " self).";
        } else if (feat.equals("Poisoner")) {
            if (id.equals("brew")) {
                speciesUses.merge("feat-stock:Poisoner", pb, Integer::sum);
                effect = "Brewed " + pb + " doses; expend 50 GP and one hour.";
            } else {
                int stock = speciesUses.getOrDefault("feat-stock:Poisoner", 0);
                if (stock == 0) return new FeatResult("Brew poison doses first.", "");
                speciesUses.put("feat-stock:Poisoner", stock - 1);
                effect =
                        "CON DC "
                                + (8 + pb + modifier)
                                + "; failure: "
                                + roll(random, 2, 8)
                                + " Poison damage and Poisoned until end of your next turn; ignores"
                                + " resistance.";
            }
        } else if (feat.equals("Telekinetic"))
            effect =
                    "STR DC "
                            + (8 + pb + modifier)
                            + "; failure: move 5 feet toward or away; willing target may fail.";
        else if (feat.equals("Actor"))
            effect =
                    "Mimicry Insight DC "
                            + (8 + pb + modifier)
                            + "; disguised impersonation checks have advantage.";
        else if (feat.equals("Boon of Energy Resistance"))
            effect =
                    "DEX DC "
                            + (8
                                    + pb
                                    + CharacterSheet.modifier(
                                            session.effectiveScore(
                                                    CharacterSheet.Ability.CON, charisma)))
                            + "; failure: "
                            + (roll(random, 2, 12)
                                    + CharacterSheet.modifier(
                                            session.effectiveScore(
                                                    CharacterSheet.Ability.CON, charisma)))
                            + " damage of resisted type.";
        else if (feat.equals("Boon of Fate"))
            effect = "Add or subtract " + roll(random, 2, 4) + " from nearby D20 Test.";
        else if (feat.equals("Boon of Fortitude"))
            healing =
                    Math.max(
                            0,
                            CharacterSheet.modifier(
                                    session.effectiveScore(CharacterSheet.Ability.CON, charisma)));
        else if (feat.equals("Boon of Irresistible Offense"))
            effect =
                    "Natural 20: extra "
                            + session.effectiveScore(FeatEffects.ability(sheet, feat), charisma)
                            + " damage of the attack's type.";
        else if (feat.equals("Charger") && id.equals("charge"))
            effect = "Charge extra damage " + random.nextInt(1, 9);
        else if (feat.equals("Defensive Duelist"))
            effect =
                    "Reaction: AC "
                            + (session.armorClass(charisma) + pb)
                            + " versus melee attacks until next turn starts.";
        else if (feat.equals("Interception"))
            effect = "Reduce ally's attack damage by " + (random.nextInt(1, 11) + pb) + ".";
        else if (feat.equals("Musician"))
            effect = "Grant Heroic Inspiration to up to " + pb + " allies who hear the song.";
        else if (feat.equals("Shield Master") && id.equals("bash"))
            effect =
                    "STR DC "
                            + (8
                                    + pb
                                    + CharacterSheet.modifier(
                                            session.effectiveScore(
                                                    CharacterSheet.Ability.STR, charisma)))
                            + "; failure: push 5 feet or Prone.";
        else if (feat.equals("Piercer"))
            effect =
                    "Replacement d"
                            + rerollSides
                            + " ["
                            + random.nextInt(1, rerollSides + 1)
                            + "]; replace one piercing damage die and use the new result, once per"
                            + " turn.";
        else if (feat.equals("Lucky"))
            effect =
                    "Additional d20 ["
                            + random.nextInt(1, 21)
                            + "]; use "
                            + (id.equals("advantage")
                                    ? "the higher result with your original d20"
                                    : "the lower result with the enemy's original attack d20")
                            + ".";
        else if (feat.equals("Unarmed Fighting"))
            effect = "Grappled target takes " + random.nextInt(1, 5) + " Bludgeoning damage.";
        else if (feat.equals("Savage Attacker"))
            effect =
                    "Weapon dice: "
                            + roll(
                                    random,
                                    weapon.properties().contains("2d6") ? 2 : 1,
                                    weapon.die())
                            + " or "
                            + roll(
                                    random,
                                    weapon.properties().contains("2d6") ? 2 : 1,
                                    weapon.die())
                            + "; choose either, then add modifiers once.";
        else if (feat.equals("Polearm Master") && id.equals("pole"))
            effect =
                    "Bonus Action: weapon attack; d4 ["
                            + random.nextInt(1, 5)
                            + "] + STR modifier; Bludgeoning damage.";
        if (healing > 0) {
            setHpWithoutNotification(Math.min(maxHitPoints(), hitPoints() + healing));
            effect += "; healed " + healing + ", HP " + hitPoints() + "/" + maxHitPoints();
        }
        if (action.dailyUses() != 0) speciesUses.merge(FeatActions.key(action), 1, Integer::sum);
        return new FeatResult("", feat + " · " + action.label() + ": " + effect);
    }

    private static int roll(java.util.random.RandomGenerator random, int count, int sides) {
        return java.util.stream.IntStream.range(0, count)
                .map(i -> random.nextInt(1, sides + 1))
                .sum();
    }
}
