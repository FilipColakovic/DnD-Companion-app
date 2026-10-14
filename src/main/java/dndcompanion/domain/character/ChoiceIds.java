package dndcompanion.domain.character;

/** Persisted choice vocabulary; helper methods preserve existing scoped identifiers. */
public final class ChoiceIds {
    private ChoiceIds() {}

    public static final String BACKGROUND = "background";
    public static final String BACKGROUND_WAYFARER_GAMING = "background.Wayfarer.gaming";
    public static final String DRAGONBORN_ANCESTRY = "dragonborn.ancestry";
    public static final String ELF_SKILL = "elf.skill";
    public static final String FEAT_CRAFTER = "feat.Crafter";
    public static final String FEAT_KEEN_MIND = "feat.Keen Mind";
    public static final String FEAT_KEEN_MIND_TIER = "feat.Keen Mind.tier";
    public static final String FEAT_MAGIC_INITIATE_ABILITY = "feat.Magic Initiate.ability";
    public static final String FEAT_MAGIC_INITIATE_CANTRIPS = "feat.Magic Initiate.cantrips";
    public static final String FEAT_MAGIC_INITIATE_LIST = "feat.Magic Initiate.list";
    public static final String FEAT_MAGIC_INITIATE_SPELL = "feat.Magic Initiate.spell";
    public static final String FEAT_MUSICIAN = "feat.Musician";
    public static final String FEAT_OBSERVANT = "feat.Observant";
    public static final String FEAT_OBSERVANT_TIER = "feat.Observant.tier";
    public static final String FEAT_SKILL_EXPERT_EXPERTISE = "feat.Skill Expert.expertise";
    public static final String FEAT_SKILL_EXPERT_SKILL = "feat.Skill Expert.skill";
    public static final String FEAT_SKILLED = "feat.Skilled";
    public static final String GOLIATH_ANCESTRY = "goliath.ancestry";
    public static final String HUMAN_FEAT = "human.feat";
    public static final String HUMAN_SKILL = "human.skill";
    public static final String SORCERER_SKILLS = "sorcerer.skills";

    public static String feat(String name) {
        return "feat." + name;
    }

    public static String background(String name) {
        return "background." + name;
    }

    public static String scopedFeat(String source, String name) {
        return source + ".feat." + name;
    }

    public static String species(Species species, String choice) {
        return "species." + species.name() + "." + choice;
    }
}
