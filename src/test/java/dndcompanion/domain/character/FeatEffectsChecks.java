package dndcompanion.domain.character;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/** Source removal, overlapping grants, capped derived stats, resource atomicity and saves. */
public final class FeatEffectsChecks {
    public static void main(String[] args) {
        telekinetic();
        telekineticRequiresMageHand();
        spellSources();
        catalogCoverage();
        resourcesAndEquipment();
        repeatableAndCombatMath();
        System.out.println(
                "Passed: feat choices, reversible score/spell grants, all 75 feat boosts,"
                        + " equipment, free casts, use pools and save round trips.");
    }

    private static CharacterController character(int level) {
        var controller = new CharacterController();
        controller.selectSpecies(Species.ORC);
        controller.setLevel(level);
        return controller;
    }

    private static void equip(PlaySession session, String name) {
        check(session.equipFeat(FeatCatalog.named(name), "", 17, true).isEmpty(), "Equip " + name);
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Missing feat spell " + name));
    }

    private static void telekineticRequiresMageHand() {
        try (var controller = character(4)) {
            var session = controller.playSession();
            equip(session, "Telekinetic");
            session.setSheet(
                    session.sheet().withChoice("feat.Telekinetic.ability", List.of("CHA")));
            var shove =
                    FeatActions.all().stream()
                            .filter(action -> action.feat().equals("Telekinetic"))
                            .findFirst()
                            .orElseThrow();
            var before = session.snapshot();
            check(!session.featActionAvailable(shove), "Shove locked before casting Mage Hand");
            check(
                    !session.useFeatAction(shove, 17, new Random(1)).error().isEmpty(),
                    "Domain rejects shove before casting");
            check(session.snapshot().equals(before), "Rejected shove changes no state");
            check(
                    !session.cast(spell("Mage Hand"), 0, false, Set.of(Metamagic.QUICKENED))
                            .isEmpty(),
                    "Unselected Metamagic rejects Mage Hand cast");
            check(!session.featActionAvailable(shove), "Failed cast does not unlock shove");
            check(session.cast(spell("Mage Hand"), 0, false, Set.of()).isEmpty(), "Cast Mage Hand");
            check(session.featActionAvailable(shove), "Successful cast unlocks shove");
            check(
                    session.useFeatAction(shove, 17, new Random(1)).error().isEmpty(),
                    "Use unlocked shove");
            session.shortRest();
            check(!session.featActionAvailable(shove), "Short Rest requires a new Mage Hand cast");
            session.castFromSource(spell("Mage Hand"), 0, "Cantrip", Set.of());
            check(session.featActionAvailable(shove), "Combat casting source unlocks shove");
            session.longRest();
            check(!session.featActionAvailable(shove), "Long Rest clears Mage Hand cast");
            session.cast(spell("Mage Hand"), 0, false, Set.of());
            session.restore(before);
            check(
                    !session.featActionAvailable(shove),
                    "Restoring character clears casting prerequisite");
        }
    }

    private static void telekinetic() {
        try (var controller = character(4)) {
            var session = controller.playSession();
            int pending = session.pendingDecisions();
            equip(session, "Telekinetic");
            check(session.pendingDecisions() == pending + 1, "Ability decision appears");
            check(
                    session.selection().automatic(spell("Mage Hand")),
                    "Mage Hand automatically granted");
            check(
                    session.selection().chosenSpells().isEmpty(),
                    "Feat spells do not consume manual selections");
            session.setSheet(
                    session.sheet().withChoice("feat.Telekinetic.ability", List.of("CHA")));
            check(
                    session.effectiveScore(Ability.CHA, controller.snapshot().charisma()) == 18,
                    "Chosen CHA +1");
            check(controller.snapshot().charisma() == 17, "Stored base score unchanged");
            check(
                    session.skillBonus(CharacterSheet.Skill.DECEPTION, 17) == 4,
                    "Effective skill bonus");
            check(session.pendingDecisions() == pending, "Reminder clears after choice");
            var saved =
                    new CharacterState(
                            4,
                            17,
                            controller.snapshot().subclass(),
                            Set.of(),
                            Species.ORC,
                            PlayState.decode(session.snapshot().encode()),
                            "Test");
            controller.restore(saved);
            check(session.effectiveScore(Ability.CHA, 17) == 18, "Load does not double bonus");
            session.removeFeat("Telekinetic");
            check(session.effectiveScore(Ability.CHA, 17) == 17, "Removing feat reverses boost");
            check(
                    !session.selection().selected(spell("Mage Hand")),
                    "Removing source removes grant");
            session.selection().choose(spell("Mage Hand"), true);
            equip(session, "Telekinetic");
            session.removeFeat("Telekinetic");
            check(
                    session.selection().selected(spell("Mage Hand")),
                    "Manual learning survives grant removal");
        }
    }

    private static void spellSources() {
        try (var controller = character(4)) {
            var session = controller.playSession();
            equip(session, "Magic Initiate");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Magic Initiate.list", List.of("Cleric"))
                            .withChoice("feat.Magic Initiate.ability", List.of("Wisdom"))
                            .withChoice(
                                    "feat.Magic Initiate.cantrips",
                                    List.of("Guidance", "Sacred Flame"))
                            .withChoice("feat.Magic Initiate.spell", List.of("Bless")));
            check(
                    session.selection().catalog().contains(spell("Bless")),
                    "Non-Sorcerer spells enter live catalog");
            check(session.selection().automatic(spell("Guidance")), "Chosen cantrips granted");
            check(
                    session.spellcastingAbility(spell("Sacred Flame")) == Ability.WIS,
                    "Feat casting ability");
            int slots = session.remainingSlots(1);
            check(
                    session.castFromSource(spell("Bless"), 1, "feat.Magic Initiate", Set.of())
                            .isEmpty(),
                    "Free feat cast");
            check(session.remainingSlots(1) == slots, "Free cast preserves slots");
            var spent = session.snapshot();
            check(
                    !session.castFromSource(spell("Bless"), 1, "feat.Magic Initiate", Set.of())
                            .isEmpty(),
                    "Spent free use blocked");
            check(session.snapshot().equals(spent), "Failed free cast atomic");
            session.shortRest();
            check(
                    session.freeCasts(spell("Bless"), "feat.Magic Initiate") == 0,
                    "No short rest reset");
            session.longRest();
            check(session.freeCasts(spell("Bless"), "feat.Magic Initiate") == 1, "Long rest reset");
            session.selectBackground(
                    BackgroundCatalog.all().stream()
                            .filter(b -> b.name().equals("Sage"))
                            .findFirst()
                            .orElseThrow());
            session.setSheet(
                    session.sheet()
                            .withChoice(
                                    "background.Sage.feat.Magic Initiate.ability",
                                    List.of("Intelligence"))
                            .withChoice(
                                    "background.Sage.feat.Magic Initiate.cantrips",
                                    List.of("Mage Hand", "Light"))
                            .withChoice(
                                    "background.Sage.feat.Magic Initiate.spell",
                                    List.of("Find Familiar")));
            check(
                    session.selection().automatic(spell("Find Familiar")),
                    "Independent background spell source");
            session.removeFeat("Magic Initiate");
            check(
                    !session.selection().selected(spell("Bless"))
                            && session.selection().selected(spell("Find Familiar")),
                    "Removing manual copy preserves background grant");
        }
    }

    private static void catalogCoverage() {
        for (var feat : FeatCatalog.all()) {
            var sheet = CharacterSheet.defaults();
            var choices = FeatEffects.choices(feat.name(), 20);
            for (var choice : choices) {
                var selected = choice.options().subList(0, choice.count());
                sheet = sheet.withChoice(choice.id(), selected);
                check(choice.complete(sheet), "Choice saves " + choice.id());
                if (choice.id().endsWith(".spell") || choice.id().endsWith(".spells"))
                    for (String name : choice.options())
                        check(
                                !SpellDetails.forSpell(spell(name)).description().isBlank(),
                                "Complete spell " + name);
            }
            if (!FeatEffects.abilities(feat).isEmpty()) {
                int total = 0;
                for (Ability ability : Ability.values())
                    total += FeatEffects.score(sheet, Set.of(feat.name()), ability, 10) - 10;
                check(
                        total == (feat.name().equals("Ability Score Improvement") ? 2 : 1),
                        "Every feat's ASI applied: " + feat.name());
            }
        }
        for (String list : List.of("Cleric", "Druid", "Wizard")) {
            var sheet =
                    CharacterSheet.defaults().withChoice("feat.Magic Initiate.list", List.of(list));
            for (var choice : CharacterChoices.initiateChoices(sheet, "feat.Magic Initiate", ""))
                if (choice.id().endsWith(".spell") || choice.id().endsWith(".cantrips"))
                    for (String name : choice.options()) spell(name);
        }
        var sheet =
                CharacterSheet.defaults()
                        .withAbility(Ability.INT, 20)
                        .withChoice("feat.Telekinetic.ability", List.of("INT"));
        check(
                FeatEffects.score(sheet, Set.of("Telekinetic"), Ability.INT, 10) == 20,
                "General cap 20");
        sheet =
                sheet.withAbility(Ability.INT, 30)
                        .withChoice("feat.Boon of Skill.ability", List.of("INT"));
        check(
                FeatEffects.score(sheet, Set.of("Boon of Skill"), Ability.INT, 10) == 30,
                "Epic cap 30");
    }

    private static void resourcesAndEquipment() {
        try (var controller = character(4)) {
            var session = controller.playSession();
            equip(session, "Lucky");
            var lucky =
                    FeatActions.all().stream()
                            .filter(a -> a.feat().equals("Lucky") && a.id().equals("advantage"))
                            .findFirst()
                            .orElseThrow();
            check(session.remainingFeatUses(lucky) == 2, "Lucky PB pool");
            int[] notifications = {0};
            session.addListener(() -> notifications[0]++);
            check(session.useFeatAction(lucky, 17, new Random(1)).error().isEmpty(), "Use Luck");
            check(notifications[0] == 1, "One completed-operation notification");
            check(
                    PlayState.decode(session.snapshot().encode())
                            .speciesUses()
                            .equals(session.snapshot().speciesUses()),
                    "Feat pool persisted");
            session.shortRest();
            check(session.remainingFeatUses(lucky) == 1, "Luck remains spent on short rest");
            session.longRest();
            check(session.remainingFeatUses(lucky) == 2, "Luck long rest reset");
            equip(session, "Lightly Armored");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Lightly Armored.ability", List.of("DEX"))
                            .withChoice("equipment.armor", List.of("Studded Leather"))
                            .withChoice("equipment.shield", List.of("Shield")));
            check(session.armorClass(17) == 14, "Armor + shield with effective DEX");
            session.setSheet(session.sheet().withChoice("equipment.armor", List.of("Plate")));
            session.selection().choose(spell("Shield"), true);
            var before = session.snapshot();
            check(
                    !session.cast(spell("Shield"), 1, false, Set.of()).isEmpty(),
                    "Untrained armor blocks casting");
            check(before.equals(session.snapshot()), "Armor failure spends nothing");
            session.removeFeat("Lightly Armored");
            check(
                    session.effectiveScore(Ability.DEX, 17) == 10,
                    "Equipment does not store derived score");
        }
    }

    private static void check(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }

    private static void repeatableAndCombatMath() {
        try (var controller = character(19)) {
            var session = controller.playSession();
            equip(session, "Ability Score Improvement");
            session.setSheet(
                    session.sheet()
                            .withAbility(Ability.STR, 16)
                            .withChoice("feat.Ability Score Improvement.copies", List.of("2"))
                            .withChoice("feat.Ability Score Improvement.ability", List.of("STR +2"))
                            .withChoice(
                                    "feat.Ability Score Improvement.copy.2.ability",
                                    List.of("STR +1 / WIS +1")));
            check(
                    session.effectiveScore(Ability.STR, 17) == 19
                            && session.effectiveScore(Ability.WIS, 17) == 11,
                    "Independent repeated ASIs");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Ability Score Improvement.copies", List.of("1")));
            check(
                    session.effectiveScore(Ability.STR, 17) == 18
                            && session.effectiveScore(Ability.WIS, 17) == 10,
                    "Reducing copy count reverses only extra copy");
            session.removeFeat("Ability Score Improvement");
            session.setSheet(
                    session.sheet()
                            .withChoice("equipment.weapon", List.of("Greatsword"))
                            .withChoice("equipment.armor", List.of("Plate"))
                            .withChoice(
                                    "feat.Boon of Energy Resistance.damage",
                                    List.of("Cold", "Fire")));
            var p = session.snapshot();
            session.restore(
                    new PlayState(
                            p.spentSlots(),
                            p.spentPoints(),
                            p.restorationUsed(),
                            p.metamagic(),
                            p.concentration(),
                            p.speciesUses(),
                            p.innateUses(),
                            p.innateActive(),
                            p.dark(),
                            p.textSize(),
                            p.hitPoints(),
                            p.maxHitPoints(),
                            Map.of(
                                    "Great Weapon Fighting",
                                    "",
                                    "Martial Weapon Training",
                                    "",
                                    "Heavy Armor Master",
                                    "",
                                    "Boon of Energy Resistance",
                                    "",
                                    "Boon of the Night Spirit",
                                    "",
                                    "Boon of Spell Recall",
                                    ""),
                            p.sheet()));
            var damage =
                    FeatCombatRules.weapon(
                            session,
                            17,
                            new FeatCombatRules.Context(false, true, true, false, false, false),
                            new FixedDice(1, 2));
            check(
                    damage.dice().equals(List.of(3, 3)) && damage.total() == 9,
                    "Two-handed Great Weapon Fighting raises 1/2 to 3, STR added once");
            check(
                    FeatCombatRules.incoming(session, "Slashing", 11, true, true) == 2,
                    "Heavy armor reduction before resistance, rounded down");
            check(
                    FeatCombatRules.incoming(session, "Cold", 11, false, false) == 5,
                    "Selected elemental resistance");
            check(
                    FeatCombatRules.incoming(session, "Psychic", 11, false, true) == 11,
                    "Night Spirit does not resist Psychic");
            var healing =
                    dndcompanion.domain.spells.HealingDice.roll(
                            new dndcompanion.domain.spells.HealingDice.Formula(2, 8, true),
                            3,
                            true,
                            new FixedDice(1, 1, 8));
            check(
                    healing.dice().equals(List.of(1, 8)) && healing.total() == 12,
                    "Healer rerolls each initial 1 only once and adds modifier once");
            session.setSheet(session.sheet().withChoice("equipment.armor", List.of("Unarmored")));
            session.selection().choose(spell("Magic Missile"), true);
            int slots = session.remainingSlots(1);
            check(
                    session.cast(spell("Magic Missile"), 1, false, Set.of(), new FixedDice(1))
                                    .isEmpty()
                            && session.remainingSlots(1) == slots,
                    "Matching Spell Recall d4 preserves slot");
            check(
                    session.cast(spell("Magic Missile"), 1, false, Set.of(), new FixedDice(4))
                                    .isEmpty()
                            && session.remainingSlots(1) == slots - 1,
                    "Nonmatching Spell Recall spends slot");
        }
    }

    private static final class FixedDice extends Random {
        private final int[] values;
        private int index;

        FixedDice(int... values) {
            this.values = values;
        }

        @Override
        public int nextInt(int origin, int bound) {
            if (index == values.length) throw new AssertionError("Unexpected extra roll");
            int value = values[index++];
            if (value < origin || value >= bound) throw new AssertionError("Invalid fixture die");
            return value;
        }
    }
}
