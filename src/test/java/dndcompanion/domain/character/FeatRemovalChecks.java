package dndcompanion.domain.character;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterSheet.Skill;

import java.util.List;
import java.util.Set;

public final class FeatRemovalChecks {
    public static void main(String[] args) {
        try (var controller =
                new CharacterController(
                        new CharacterState(
                                8, 17, SorcererRules.Subclass.WILD_MAGIC, Set.of(), Species.ORC))) {
            var session = controller.playSession();
            equip(session, "Skilled");
            session.setSheet(
                    session.sheet()
                            .withChoice(
                                    "feat.Skilled",
                                    List.of("Arcana", "History", "Tool: Thieves' Tools")));
            session.setSkillTier(Skill.ARCANA, 2);
            check(
                    session.skillTier(Skill.ARCANA) == 2 && session.sheet().tier(Skill.ARCANA) == 0,
                    "Granted-skill promotion is source-bound, not a permanent manual value");
            session.setSkillTier(Skill.PERCEPTION, 1);
            session.removeFeat("Skilled");
            check(
                    session.skillTier(Skill.ARCANA) == 0 && session.skillTier(Skill.HISTORY) == 0,
                    "Removing Skilled removes its proficiency and dependent expertise");
            check(
                    session.skillTier(Skill.PERCEPTION) == 1
                            && !session.grantedTools().contains("Thieves' Tools"),
                    "Independent manual skill remains; granted tool disappears");
            check(
                    !session.sheet().choice("feat.Skilled").isEmpty(),
                    "Configuration remains dormant");
            equip(session, "Skilled");
            check(
                    session.skillTier(Skill.ARCANA) == 2,
                    "Unchanged source configuration can be reactivated");
            session.setSheet(
                    session.sheet().withChoice("sorcerer.skills", List.of("Arcana", "Deception")));
            session.removeFeat("Skilled");
            check(
                    session.skillTier(Skill.ARCANA) == 1,
                    "Independent overlapping class proficiency remains");
            equip(session, "Skilled");
            check(
                    session.sheet().choice("feat.Skilled").isEmpty(),
                    "Dormant proficiency choices are revalidated before reactivation");
            session.removeFeat("Skilled");
            equip(session, "Skill Expert");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Skill Expert.skill", List.of("Insight"))
                            .withChoice("feat.Skill Expert.expertise", List.of("Deception"))
                            .withChoice("feat.Skill Expert.ability", List.of("CHA")));
            check(
                    session.effectiveScore(Ability.CHA, 17) == 18
                            && session.skillTier(Skill.INSIGHT) == 1
                            && session.skillTier(Skill.DECEPTION) == 2,
                    "Skill Expert benefits active");
            session.removeFeat("Skill Expert");
            check(
                    session.effectiveScore(Ability.CHA, 17) == 17
                            && session.skillTier(Skill.INSIGHT) == 0
                            && session.skillTier(Skill.DECEPTION) == 1,
                    "Skill Expert reverses only its contributions");
            for (String feat : List.of("Keen Mind", "Observant")) {
                equip(session, feat);
                String label = feat.equals("Keen Mind") ? "Arcana" : "Perception";
                Skill skill = feat.equals("Keen Mind") ? Skill.ARCANA : Skill.PERCEPTION;
                session.setSheet(
                        session.sheet()
                                .withChoice("feat." + feat, List.of(label))
                                .withChoice("feat." + feat + ".tier", List.of("Expertise")));
                check(session.skillTier(skill) == 2, feat + " upgrades existing proficiency");
                session.removeFeat(feat);
                check(session.skillTier(skill) == 1, feat + " removal restores original tier");
            }
            var saved = controller.snapshot();
            controller.restore(
                    new CharacterState(
                            saved.level(),
                            saved.charisma(),
                            saved.subclass(),
                            saved.chosenSpells(),
                            saved.species(),
                            PlayState.decode(saved.play().encode()),
                            saved.name()));
            check(
                    session.skillTier(Skill.ARCANA) == 1
                            && session.skillTier(Skill.PERCEPTION) == 1,
                    "Dormant upgrades remain inactive after save/reopen");
            equip(session, "Skilled");
            session.setSheet(
                    session.sheet()
                            .withChoice(
                                    "feat.Skilled",
                                    List.of("Investigation", "History", "Tool: Thieves' Tools")));
            equip(session, "Keen Mind");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Keen Mind", List.of("Investigation"))
                            .withChoice("feat.Keen Mind.tier", List.of("Expertise")));
            check(
                    session.skillTier(Skill.INVESTIGATION) == 2,
                    "Keen Mind upgrades a feat proficiency");
            session.removeFeat("Skilled");
            check(
                    session.skillTier(Skill.INVESTIGATION) == 0
                            && session.requiredChoices().stream()
                                    .anyMatch(
                                            c ->
                                                    c.id().equals("feat.Keen Mind")
                                                            && !c.complete(session.sheet())),
                    "Removing prerequisite proficiency deactivates dependent feat expertise and"
                            + " prompts reconfirmation");
        }
        try (var controller =
                new CharacterController(
                        new CharacterState(
                                8, 20, SorcererRules.Subclass.WILD_MAGIC, Set.of(), Species.ORC))) {
            var session = controller.playSession();
            equip(session, "Skilled");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Skilled.copies", List.of("2"))
                            .withChoice("feat.Skilled", List.of("Arcana", "History", "Deception"))
                            .withChoice(
                                    "feat.Skilled.copy.2",
                                    List.of("Insight", "Nature", "Perception")));
            session.setSkillTier(Skill.INSIGHT, 2);
            session.setSheet(session.sheet().withChoice("feat.Skilled.copies", List.of("1")));
            check(
                    session.skillTier(Skill.INSIGHT) == 0 && session.skillTier(Skill.ARCANA) == 1,
                    "Removing one repeatable copy removes only its dependent grants");
            session.setSheet(session.sheet().withChoice("feat.Skilled.copies", List.of("2")));
            check(
                    session.skillTier(Skill.INSIGHT) == 2,
                    "Copy configuration and dependencies survive reselection");
            equip(session, "Telekinetic");
            session.setSheet(
                    session.sheet().withChoice("feat.Telekinetic.ability", List.of("CHA")));
            check(session.effectiveScore(Ability.CHA, 20) == 20, "Ability grants respect cap");
            session.removeFeat("Telekinetic");
            check(
                    session.effectiveScore(Ability.CHA, 20) == 20
                            && session.sheet().score(Ability.CHA, 20) == 20,
                    "Removing capped bonus never subtracts from base");
        }
        try (var controller =
                new CharacterController(
                        CharacterState.create("Separate sources", ClassId.SORCERER))) {
            var session = controller.playSession();
            session.setSheet(
                    session.sheet()
                            .withChoice(ChoiceIds.HUMAN_FEAT, List.of("Skilled"))
                            .withChoice("human.feat.scoped", List.of("Skilled"))
                            .withChoice(
                                    "human.feat.Skilled",
                                    List.of("Insight", "Nature", "Tool: Thieves' Tools")));
            check(
                    session.acquireFeatCopy(FeatCatalog.named("Skilled"), 17).isEmpty(),
                    "Acquire an independent repeatable feat copy");
            session.setSheet(
                    session.sheet()
                            .withChoice(
                                    "feat.Skilled", List.of("Arcana", "History", "Perception")));
            session.removeFeat("Skilled");
            check(
                    session.skillTier(Skill.ARCANA) == 0
                            && session.skillTier(Skill.INSIGHT) == 1
                            && session.grantedTools().contains("Thieves' Tools"),
                    "Removing acquired copy preserves independent Human source");
            session.setSheet(
                    session.sheet()
                            .withChoice(ChoiceIds.HUMAN_FEAT, List.of("Magic Initiate"))
                            .withChoice("human.feat.scoped", List.of("Magic Initiate"))
                            .withChoice("human.feat.Magic Initiate.list", List.of("Wizard"))
                            .withChoice(
                                    "human.feat.Magic Initiate.ability", List.of("Intelligence"))
                            .withChoice(
                                    "human.feat.Magic Initiate.cantrips",
                                    List.of("Mage Hand", "Message"))
                            .withChoice("human.feat.Magic Initiate.spell", List.of("Shield")));
            check(
                    session.acquireFeatCopy(FeatCatalog.named("Magic Initiate"), 17).isEmpty(),
                    "Acquire additional Magic Initiate");
            session.setSheet(
                    session.sheet()
                            .withChoice("feat.Magic Initiate.list", List.of("Cleric"))
                            .withChoice("feat.Magic Initiate.ability", List.of("Wisdom"))
                            .withChoice(
                                    "feat.Magic Initiate.cantrips", List.of("Light", "Guidance"))
                            .withChoice("feat.Magic Initiate.spell", List.of("Bless")));
            check(
                    session.featSpellGrants().size() == 6,
                    "Independent casting sources grant separate lists");
            session.removeFeat("Magic Initiate");
            check(
                    session.featSpellGrants().size() == 3
                            && session.featSpellGrants().stream()
                                    .noneMatch(g -> g.spell().equals("Bless")),
                    "Removing acquired Magic Initiate preserves only Human spell grants");
        }
        try (var controller =
                new CharacterController(
                        CharacterState.create("Background check", ClassId.SORCERER))) {
            var session = controller.playSession();
            session.setSheet(
                    session.sheet()
                            .withChoice(ChoiceIds.BACKGROUND, List.of("Noble"))
                            .withChoice("background.Noble.abilities", List.of("CHA +2 / INT +1")));
            check(
                    session.effectiveScore(Ability.CHA, 17) == 19,
                    "New background allocation is a derived grant");
            session.setSheet(session.sheet().withChoice(ChoiceIds.BACKGROUND, List.of()));
            check(
                    session.effectiveScore(Ability.CHA, 17) == 17
                            && session.sheet().score(Ability.CHA, 17) == 17,
                    "Removing background reverses new allocation and preserves base score");
        }
        System.out.println(
                "Passed: source-bound proficiency upgrades, overlapping grants, dormant"
                        + " configuration, feat/background removal and restoration.");
    }

    private static void equip(PlaySession session, String name) {
        check(session.equipFeat(FeatCatalog.named(name), "", 17, true).isEmpty(), "Equip " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
