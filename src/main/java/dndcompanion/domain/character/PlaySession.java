package dndcompanion.domain.character;

import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellSelection;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Shared live resource model. Failed casts validate fully before spending anything. */
public final class PlaySession {
    private boolean mageHandCast;
    private final SpellSelection selection;
    private final Runnable selectionListener = this::selectionChanged;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final int[] slots = new int[RuleLimits.MAX_SPELL_LEVEL];
    private int points, innateUses, textSize = 14;
    private int hitPoints, maxHitPoints;

    public boolean automaticHp() {
        return sheet.values().containsKey("field.AUTOMATIC_HP")
                ? sheet.field(CharacterSheet.Field.AUTOMATIC_HP) == 1
                : maxHitPoints == 0;
    }

    public HitPointRules.Calculation hpCalculation() {
        var selected = feats();
        return HitPointRules.calculate(
                selection.progression(),
                effectiveScore(CharacterSheet.Ability.CON, 10),
                selection.species(),
                selected.containsKey("Tough"),
                selected.containsKey("Boon of Fortitude"));
    }

    public int hitPoints() {
        return automaticHp()
                ? (sheet.field(CharacterSheet.Field.HP_ZERO) == 1
                        ? 0
                        : Math.max(0, maxHitPoints() - sheet.field(CharacterSheet.Field.HP_DAMAGE)))
                : hitPoints;
    }

    public int maxHitPoints() {
        return automaticHp() ? hpCalculation().maximum() : maxHitPoints;
    }

    public void setCurrentHitPoints(int current) {
        int maximum = maxHitPoints();
        if (current < 0 || current > maximum)
            throw new IllegalArgumentException("HP must be between zero and maximum HP");
        if (automaticHp()) {
            setSheet(
                    sheet.withField(CharacterSheet.Field.HP_DAMAGE, maximum - current)
                            .withField(CharacterSheet.Field.HP_ZERO, current == 0 ? 1 : 0));
        } else {
            hitPoints = current;
            changed();
        }
    }

    public void setAutomaticHp(boolean automatic) {
        if (automatic == automaticHp()) return;
        int current = hitPoints(), maximum = maxHitPoints(), damage = maximum - current;
        if (automatic) {
            hitPoints = 0;
            maxHitPoints = 0;
            setSheet(
                    sheet.withField(CharacterSheet.Field.AUTOMATIC_HP, 1)
                            .withField(CharacterSheet.Field.HP_DAMAGE, damage)
                            .withField(
                                    CharacterSheet.Field.HP_ZERO,
                                    current == 0 && maximum > 0 ? 1 : 0));
        } else {
            hitPoints = current;
            maxHitPoints = maximum;
            setSheet(sheet.withField(CharacterSheet.Field.AUTOMATIC_HP, 0));
        }
    }

    public void setHitPoints(int current, int maximum) {
        if (current < 0 || maximum < 0 || current > maximum)
            throw new IllegalArgumentException("HP must be between zero and maximum HP");
        hitPoints = current;
        maxHitPoints = maximum;
        sheet = sheet.withField(CharacterSheet.Field.AUTOMATIC_HP, 0);
        changed();
    }

    private boolean restorationUsed, innate, dark;
    private String concentration = "";
    private String lastCastDetails = "";

    public String lastCastDetails() {
        return lastCastDetails;
    }

    private final Set<Metamagic> meta = EnumSet.noneOf(Metamagic.class);
    private final Map<String, String> feats = new HashMap<>();
    private long restoreRevision;
    private CharacterSheet sheet = CharacterSheet.defaults();

    public CharacterSheet sheet() {
        return sheet;
    }

    public void setSheet(CharacterSheet sheet) {
        Objects.requireNonNull(sheet);
        if (this.sheet.equals(sheet)) return;
        this.sheet = sheet;
        changed();
    }

    public long restoreRevision() {
        return restoreRevision;
    }

    public Map<String, String> feats() {
        var result = new HashMap<>(CharacterChoices.feats(sheet, selection.species(), feats));
        if (!sorcerer()) {
            for (String style : sheet.choice("fighter.style"))
                result.putIfAbsent(style, "Fighter · Fighting Style");
            if (selection.level() >= 7)
                for (String style : sheet.choice("fighter.style.extra"))
                    result.putIfAbsent(style, "Champion · Additional Fighting Style");
        }
        return Map.copyOf(result);
    }

    public SorcererRules.Stats stats(int charisma) {
        return ClassRules.compatibility(selection.progression(), charisma);
    }

    public boolean sorcerer() {
        return selection.classId() == ClassId.SORCERER;
    }

    public List<ResourcePool> resources() {
        if (!sorcerer()) return ClassResourceRules.fighter(selection.level(), speciesUses);
        return List.of(
                new ResourcePool(
                        "sorcerer.points",
                        "Sorcery Points",
                        maxPoints(),
                        points,
                        ResourcePool.Recovery.LONG_REST),
                new ResourcePool(
                        "sorcerer.innate",
                        "Innate Sorcery",
                        2,
                        innateUses,
                        ResourcePool.Recovery.LONG_REST));
    }

    public ResourcePool resource(String id) {
        return resources().stream().filter(pool -> pool.id().equals(id)).findFirst().orElseThrow();
    }

    public FeatResult useClassAction(
            ClassActions.Action action,
            int charisma,
            java.util.random.RandomGenerator random,
            int original,
            int dc) {
        var execution = new ClassActionService(this);
        var result = execution.execute(action, charisma, random, original, dc);
        if (result.error().isEmpty()) {
            sheet = execution.sheet;
            speciesUses.clear();
            speciesUses.putAll(execution.spent);
            if (!automaticHp()) hitPoints = execution.hitPoints;
            changed();
        }
        return result;
    }

    public List<CharacterChoices.Choice> requiredChoices() {
        return CharacterChoices.required(sheet, selection.species(), feats.keySet()).stream()
                .map(
                        choice ->
                                choice.id().equals("feat.Ritual Caster.spells")
                                        ? FeatEffects.choices("Ritual Caster", selection.level())
                                                .stream()
                                                .filter(c -> c.id().equals(choice.id()))
                                                .findFirst()
                                                .orElseThrow()
                                        : choice)
                .map(
                        choice ->
                                choice.id().equals("feat.Weapon Master.weapon")
                                        ? new CharacterChoices.Choice(
                                                choice.id(),
                                                choice.title(),
                                                choice.explanation(),
                                                1,
                                                EquipmentRules.weapons().stream()
                                                        .filter(
                                                                w ->
                                                                        !w.properties()
                                                                                        .contains(
                                                                                                "Unarmed")
                                                                                && (!w.martial()
                                                                                        || feats().containsKey(
                                                                                                        "Martial"
                                                                                                            + " Weapon"
                                                                                                            + " Training")))
                                                        .map(EquipmentRules.Weapon::name)
                                                        .toList())
                                        : choice)
                .map(
                        choice ->
                                choice.id().equals("feat.Boon of Skill.expertise")
                                        ? new CharacterChoices.Choice(
                                                choice.id(),
                                                choice.title(),
                                                choice.explanation(),
                                                1,
                                                java.util.Arrays.stream(
                                                                CharacterSheet.Skill.values())
                                                        .filter(
                                                                skill ->
                                                                        skillTier(skill) < 2
                                                                                || sheet.choice(
                                                                                                choice
                                                                                                        .id())
                                                                                        .contains(
                                                                                                skill.label))
                                                        .map(skill -> skill.label)
                                                        .toList())
                                        : choice)
                .toList();
    }

    public int effectiveScore(CharacterSheet.Ability ability, int charisma) {
        return FeatEffects.score(sheet, feats().keySet(), ability, charisma);
    }

    public CharacterSheet effectiveSheet() {
        CharacterSheet effective = sheet;
        for (var ability : CharacterSheet.Ability.values())
            effective =
                    effective.withAbility(
                            ability,
                            effectiveScore(ability, sheet.score(CharacterSheet.Ability.CHA, 10)));
        for (var skill : CharacterSheet.Skill.values())
            if (dependentExpertise(skill)) effective = effective.withSkill(skill, 2);
        return effective;
    }

    private boolean dependentExpertise(CharacterSheet.Skill skill) {
        return sheet.skillUpgrade(skill).stream()
                .anyMatch(
                        CharacterGrants.sources(sheet, selection.species(), feats.keySet(), skill)
                                ::contains);
    }

    public void setSkillTier(CharacterSheet.Skill skill, int tier) {
        if (tier == 2 && sheet.tier(skill) == 0 && grantedSkills().contains(skill))
            setSheet(
                    sheet.withSkillUpgrade(
                            skill,
                            CharacterGrants.sources(
                                    sheet, selection.species(), feats.keySet(), skill)));
        else setSheet(sheet.withSkill(skill, tier).withSkillUpgrade(skill, Set.of()));
    }

    public int armorClass(int charisma) {
        return EquipmentRules.ac(
                sheet, feats().keySet(), effectiveScore(CharacterSheet.Ability.DEX, charisma));
    }

    public int speed() {
        return sheet.field(CharacterSheet.Field.SPEED)
                + (feats().containsKey("Speedy") ? 10 : 0)
                + (feats().containsKey("Boon of Speed") ? 30 : 0);
    }

    public boolean saveProficiency(CharacterSheet.Ability ability) {
        return ClassDefinition.forClass(selection.classId()).savingThrows().contains(ability)
                || feats().containsKey("Resilient")
                        && sheet.choice("feat.Resilient.ability").contains(ability.name());
    }

    public List<FeatEffects.SpellGrant> featSpellGrants() {
        return FeatEffects.spells(sheet, selection.species(), feats.keySet());
    }

    public int remainingFeatUses(FeatActions.Action action) {
        int maximum = action.dailyUses() < 0 ? 2 + (selection.level() - 1) / 4 : action.dailyUses();
        return maximum == 0
                ? Integer.MAX_VALUE
                : Math.max(0, maximum - speciesUses.getOrDefault(FeatActions.key(action), 0));
    }

    public record FeatResult(String error, String event) {}

    public FeatResult useFeatAction(
            FeatActions.Action action, int charisma, java.util.random.RandomGenerator random) {
        return useFeatAction(action, charisma, random, EquipmentRules.weapon(sheet).die());
    }

    public FeatResult useFeatAction(
            FeatActions.Action action,
            int charisma,
            java.util.random.RandomGenerator random,
            int rerollSides) {
        return useFeatAction(action, charisma, random, rerollSides, "");
    }

    public FeatResult useFeatAction(
            FeatActions.Action action,
            int charisma,
            java.util.random.RandomGenerator random,
            int rerollSides,
            String item) {
        var execution = new FeatActionService(this);
        var result = execution.execute(action, charisma, random, rerollSides, item);
        if (result.error().isEmpty()) {
            sheet = execution.sheet;
            speciesUses.clear();
            speciesUses.putAll(execution.speciesUses);
            if (!automaticHp()) hitPoints = execution.hitPoints();
            changed();
        }
        return result;
    }

    boolean mageHandWasCast() {
        return mageHandCast;
    }

    private void setHpWithoutNotification(int current) {
        if (automaticHp())
            sheet =
                    sheet.withField(CharacterSheet.Field.HP_DAMAGE, maxHitPoints() - current)
                            .withField(CharacterSheet.Field.HP_ZERO, current == 0 ? 1 : 0);
        else hitPoints = current;
    }

    public void takeDamage(int damage) {
        if (damage < 0) throw new IllegalArgumentException("Damage cannot be negative");
        int temp = sheet.field(CharacterSheet.Field.TEMP_HP);
        int absorbed = Math.min(temp, damage);
        sheet = sheet.withField(CharacterSheet.Field.TEMP_HP, temp - absorbed);
        setHpWithoutNotification(Math.max(0, hitPoints() - damage + absorbed));
        changed();
    }

    public FeatResult spendHitDie(int charisma, java.util.random.RandomGenerator random) {
        int spent = sheet.field(CharacterSheet.Field.SPENT_HIT_DICE);
        if (spent >= selection.level()) return new FeatResult("No Hit Dice remain.", "");
        if (hitPoints() == 0 || hitPoints() >= maxHitPoints())
            return new FeatResult("Spend a Hit Die while conscious and below maximum HP.", "");
        int sides = ClassDefinition.forClass(selection.classId()).hitDie();
        int die = random.nextInt(1, sides + 1);
        int healing =
                Math.max(
                        1,
                        die
                                + CharacterSheet.modifier(
                                        effectiveScore(CharacterSheet.Ability.CON, charisma)));
        sheet = sheet.withField(CharacterSheet.Field.SPENT_HIT_DICE, spent + 1);
        setHpWithoutNotification(Math.min(maxHitPoints(), hitPoints() + healing));
        changed();
        return new FeatResult(
                "",
                "Short Rest Hit Die: d"
                        + sides
                        + " ["
                        + die
                        + "] + CON = "
                        + healing
                        + " HP (limited by maximum HP)");
    }

    public int pendingDecisions() {
        return (int) requiredChoices().stream().filter(choice -> !choice.complete(sheet)).count();
    }

    private CharacterGrants grants() {
        return CharacterGrants.calculate(sheet, selection.species(), feats.keySet());
    }

    public Set<CharacterSheet.Skill> grantedSkills() {
        return grants().skills();
    }

    public Set<CharacterSheet.Skill> expertiseSkills() {
        return grants().expertise();
    }

    public int skillTier(CharacterSheet.Skill skill) {
        return Math.max(grants().tier(sheet, skill), dependentExpertise(skill) ? 2 : 0);
    }

    /** New proficiency choices exclude their own grant so saved choices remain editable. */
    public boolean proficiencyOptionAvailable(String choiceId, String option) {
        boolean newProficiency =
                choiceId.equals(ChoiceIds.SORCERER_SKILLS)
                        || choiceId.equals("fighter.skills")
                        || choiceId.equals(ChoiceIds.HUMAN_SKILL)
                        || choiceId.equals(ChoiceIds.ELF_SKILL)
                        || choiceId.equals(ChoiceIds.FEAT_SKILL_EXPERT_SKILL)
                        || choiceId.matches("(?:.*\\.)?feat\\.Skilled(?:\\.copy\\.[0-9]+)?");
        if (!newProficiency) return true;
        CharacterSheet.Skill skill =
                Arrays.stream(CharacterSheet.Skill.values())
                        .filter(item -> item.label.equals(option))
                        .findFirst()
                        .orElse(null);
        if (skill == null) return true;
        CharacterSheet withoutChoice = sheet.withChoice(choiceId, List.of());
        return CharacterGrants.calculate(withoutChoice, selection.species(), feats.keySet())
                        .tier(withoutChoice, skill)
                == 0;
    }

    public int skillTierExcluding(CharacterSheet.Skill skill, String excludedFeat) {
        Set<String> active = new HashSet<>(feats.keySet());
        active.remove(excludedFeat);
        return CharacterGrants.calculate(sheet, selection.species(), active).tier(sheet, skill);
    }

    public int skillBonus(CharacterSheet.Skill skill, int charisma) {
        int proficiency = ClassRules.proficiency(selection.level());
        return SkillRules.bonus(
                effectiveScore(skill.ability, charisma), proficiency, skillTier(skill));
    }

    public List<String> grantedTools() {
        return grants().tools();
    }

    public String featConfiguration(String name) {
        return requiredChoices().stream()
                .filter(
                        choice ->
                                (choice.id().startsWith("feat." + name)
                                                || choice.id().contains(".feat." + name))
                                        && choice.complete(sheet))
                .map(choice -> choice.title() + ": " + String.join(", ", sheet.choice(choice.id())))
                .collect(java.util.stream.Collectors.joining("\n"));
    }

    public void selectBackground(BackgroundCatalog.Background background) {
        String name = background == null ? "" : background.name();
        if (sheet.choice(ChoiceIds.BACKGROUND)
                .equals(background == null ? List.of() : List.of(name))) return;
        setSheet(
                sheet.withChoice("background", background == null ? List.of() : List.of(name))
                        .withNote(CharacterSheet.Note.BACKGROUND, name)
                        .withNote(CharacterSheet.Note.BACKGROUND_FEAT, ""));
    }

    public boolean backgroundFeat(String name) {
        return BackgroundCatalog.selected(sheet)
                .map(background -> background.feat().equals(name))
                .orElse(false);
    }

    public CharacterSheet.Ability spellcastingAbility(SpellCatalog.Spell spell) {
        boolean classSpell =
                sorcerer()
                        && (SpellCatalog.alwaysPrepared(
                                        spell, selection.level(), selection.subclass())
                                || selection.chosenSpells().contains(spell.name())
                                        && spell.sorcererSpell());
        if (!classSpell && selection.featSpell(spell))
            return featSpellGrants().stream()
                    .filter(g -> g.spell().equals(spell.name()))
                    .map(FeatEffects.SpellGrant::ability)
                    .findFirst()
                    .orElse(CharacterSheet.Ability.CHA);
        return !classSpell && selection.speciesSpell(spell)
                ? CharacterChoices.speciesAbility(sheet, selection.species())
                : CharacterSheet.Ability.CHA;
    }

    public int spellcastingModifier(SpellCatalog.Spell spell, int charisma) {
        return CharacterSheet.modifier(effectiveScore(spellcastingAbility(spell), charisma));
    }

    public int spellcastingModifier(SpellCatalog.Spell spell, int charisma, String source) {
        var grant =
                featSpellGrants().stream()
                        .filter(g -> g.source().equals(source) && g.spell().equals(spell.name()))
                        .findFirst();
        if (grant.isPresent())
            return CharacterSheet.modifier(effectiveScore(grant.get().ability(), charisma));
        if (source != null && source.equals("Species free cast"))
            return CharacterSheet.modifier(
                    effectiveScore(
                            CharacterChoices.speciesAbility(sheet, selection.species()), charisma));
        return spellcastingModifier(spell, charisma);
    }

    public String spellRange(SpellCatalog.Spell spell) {
        String range = SpellDetails.forSpell(spell).range();
        if (spell.name().equals("Mage Hand") && feats().containsKey("Telekinetic"))
            return "60 feet (Telekinetic)";
        var matcher =
                java.util.regex.Pattern.compile(
                                "^(\\d+) feet$", java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(range);
        if (feats().containsKey("Spell Sniper")
                && dndcompanion.domain.spells.SpellMechanics.hasAttack(spell)
                && matcher.matches()
                && Integer.parseInt(matcher.group(1)) >= 10)
            return (Integer.parseInt(matcher.group(1)) + 60) + " feet (Spell Sniper)";
        return range;
    }

    public String spellComponents(SpellCatalog.Spell spell, String source) {
        if (spell.name().equals("Mage Hand") && feats().containsKey("Telekinetic"))
            return "None (Telekinetic)";
        if (spell.name().equals("Detect Thoughts") && "Telepathic".equals(source))
            return "None (Telepathic free cast)";
        return SpellDetails.forSpell(spell).components();
    }

    public String equipFeat(
            FeatCatalog.Feat feat, String notes, int charisma, boolean prerequisitesConfirmed) {
        String error = feat.requirements(selection.level(), charisma, selection.classId());
        if (!error.isEmpty() && !feats.containsKey(feat.name())) return error;
        if (feat.needsConfirmation(charisma)
                && !prerequisitesConfirmed
                && !feats.containsKey(feat.name()))
            return "Confirm the additional prerequisites on your character sheet.";
        if (notes.length() > 2000) return "Keep feat notes within 2000 characters.";
        if (backgroundFeat(feat.name()) && !feats.containsKey(feat.name())) {
            setSheet(sheet.withNote(CharacterSheet.Note.BACKGROUND_FEAT, notes));
            return "";
        }
        if (!feats().containsKey(feat.name())
                && Set.of("Skilled", "Skill Expert").contains(feat.name())) {
            CharacterGrants other = grants();
            String prefix = "choice.feat." + feat.name();
            for (String key : List.copyOf(sheet.values().keySet())) {
                if (!key.startsWith(prefix)) continue;
                String id = key.substring(7);
                boolean proficiency =
                        feat.name().equals("Skilled")
                                ? id.equals("feat.Skilled")
                                        || id.matches("feat\\.Skilled\\.copy\\.[0-9]+")
                                : id.equals(ChoiceIds.FEAT_SKILL_EXPERT_SKILL);
                if (proficiency
                        && java.util.Arrays.stream(CharacterSheet.Skill.values())
                                .anyMatch(
                                        skill ->
                                                sheet.choice(id).contains(skill.label)
                                                        && other.tier(sheet, skill) > 0))
                    sheet = sheet.withChoice(id, List.of());
            }
        }
        feats.put(feat.name(), notes);
        if (feat.repeatable() && sheet.choice("feat." + feat.name() + ".copies").isEmpty())
            sheet = sheet.withChoice("feat." + feat.name() + ".copies", List.of("1"));
        changed();
        return "";
    }

    public void removeFeat(String name) {
        if (backgroundFeat(name) && !feats.containsKey(name)) return;
        boolean removed = feats.remove(name) != null;
        if (!removed
                && selection.species() == Species.HUMAN
                && sheet.choice(ChoiceIds.HUMAN_FEAT).contains(name)) {
            sheet = sheet.withChoice(ChoiceIds.HUMAN_FEAT, List.of());
            removed = true;
        }
        if (removed) changed();
    }

    public String acquireFeatCopy(FeatCatalog.Feat feat, int charisma) {
        if (!feat.repeatable()) return "This feat is not repeatable.";
        String error = feat.requirements(selection.level(), charisma, selection.classId());
        if (!error.isEmpty()) return error;
        int next = feats.containsKey(feat.name()) ? FeatEffects.copies(sheet, feat.name()) + 1 : 1;
        if (next > (feat.name().equals("Magic Initiate") ? 3 : 5))
            return "All supported copies are already recorded.";
        if (feat.name().equals("Skilled")) {
            String id = FeatEffects.prefix(feat.name(), next);
            if (sheet.choice(id).stream()
                    .anyMatch(option -> !proficiencyOptionAvailable(id, option)))
                sheet = sheet.withChoice(id, List.of());
        }
        feats.putIfAbsent(feat.name(), "");
        sheet =
                sheet.withChoice(
                        "feat." + feat.name() + ".copies", List.of(Integer.toString(next)));
        changed();
        return "";
    }

    private final Map<String, Integer> speciesUses = new HashMap<>();

    public PlaySession(SpellSelection selection) {
        this.selection = selection;
        selection.addChangeListener(selectionListener);
    }

    public void dispose() {
        selection.removeChangeListener(selectionListener);
        listeners.clear();
    }

    public void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void holdZeroHp() {
        if (automaticHp()
                && sheet.field(CharacterSheet.Field.HP_ZERO) == 0
                && sheet.field(CharacterSheet.Field.HP_DAMAGE) > 0
                && sheet.field(CharacterSheet.Field.HP_DAMAGE) >= maxHitPoints())
            sheet = sheet.withField(CharacterSheet.Field.HP_ZERO, 1);
    }

    private void selectionChanged() {
        selection.setFeatSpells(
                featSpellGrants().stream()
                        .map(FeatEffects.SpellGrant::spell)
                        .collect(java.util.stream.Collectors.toSet()));
        holdZeroHp();
    }

    private void changed() {
        selectionChanged();
        listeners.forEach(Runnable::run);
    }

    public void characterChanged() {
        changed();
    }

    public SpellSelection selection() {
        return selection;
    }

    public int maxPoints() {
        if (!sorcerer()) return 0;
        return selection.level() == 1 ? 0 : selection.level();
    }

    public int remainingPoints() {
        return Math.max(0, maxPoints() - points);
    }

    public int maxSlots(int slot) {
        return stats(17).spellSlots().get(slot - 1);
    }

    public int remainingSlots(int slot) {
        return Math.max(0, maxSlots(slot) - slots[slot - 1]);
    }

    public void setRemainingPoints(int remaining) {
        if (remaining < 0 || remaining > maxPoints()) {
            throw new IllegalArgumentException("Point range");
        }
        points = maxPoints() - remaining;
        changed();
    }

    public void setRemainingSlots(int slot, int remaining) {
        if (remaining < 0 || remaining > maxSlots(slot)) {
            throw new IllegalArgumentException("Slot range");
        }
        slots[slot - 1] = maxSlots(slot) - remaining;
        changed();
    }

    public Set<Metamagic> metamagic() {
        return Set.copyOf(meta);
    }

    public String choose(Metamagic option, boolean choose) {
        if (choose && !sorcerer()) return "Metamagic requires Sorcerer levels.";
        if (!choose) {
            meta.remove(option);
            changed();
            return "";
        }
        if (meta.contains(option)) {
            return "";
        }
        if (meta.size() >= Metamagic.limit(selection.level())) {
            return "Remove an option before selecting another.";
        }
        meta.add(option);
        changed();
        return "";
    }

    public String shortRest() {
        mageHandCast = false;
        if (!sorcerer())
            for (ResourcePool pool : resources())
                speciesUses.put("resource:" + pool.id(), ClassResourceRules.afterShortRest(pool));
        FeatActions.all().stream()
                .filter(FeatActions.Action::shortRest)
                .forEach(action -> speciesUses.remove(FeatActions.key(action)));
        innate = false;
        String result = "Short Rest: spell slots are unchanged.";
        if (selection.level() >= 5 && !restorationUsed && remainingPoints() < maxPoints()) {
            int recovered = Math.min(selection.level() / 2, maxPoints() - remainingPoints());
            points = Math.max(0, points - recovered);
            restorationUsed = true;
            result =
                    "Short Rest: recovered "
                            + recovered
                            + " Sorcery Points. Restoration used until Long Rest.";
        }
        changed();
        return result;
    }

    public void longRest() {
        mageHandCast = false;
        sheet = sheet.rested();
        if (automaticHp())
            sheet =
                    sheet.withField(CharacterSheet.Field.HP_DAMAGE, 0)
                            .withField(CharacterSheet.Field.HP_ZERO, 0);
        else hitPoints = maxHitPoints;
        Arrays.fill(slots, 0);
        points = 0;
        restorationUsed = false;
        innateUses = 0;
        innate = false;
        concentration = "";
        speciesUses.keySet().removeIf(key -> !key.equals("feat-stock:Poisoner"));
        changed();
    }

    public boolean innateActive() {
        return innate;
    }

    public boolean featActionAvailable(FeatActions.Action action) {
        return feats().containsKey(action.feat())
                && remainingFeatUses(action) > 0
                && (!action.feat().equals("Telekinetic") || mageHandCast);
    }

    public String toggleInnate() {
        if (!sorcerer()) return "Innate Sorcery requires Sorcerer levels.";
        if (innate) {
            innate = false;
            changed();
            return "";
        }
        if (innateUses >= 2) {
            if (selection.level() < 7 || remainingPoints() < 2) {
                return "No Innate Sorcery uses remaining.";
            }
            points += 2;
        } else {
            innateUses++;
        }
        innate = true;
        changed();
        return "";
    }

    public String concentration() {
        return concentration;
    }

    public void endConcentration() {
        concentration = "";
        changed();
    }

    public int freeCasts(SpellCatalog.Spell spell) {
        if (spell.level() == 0 || !selection.speciesSpell(spell)) {
            return 0;
        }
        int max = selection.species() == Species.FOREST_GNOME ? 2 + (selection.level() - 1) / 4 : 1;
        return Math.max(
                0,
                max - speciesUses.getOrDefault(selection.species().name() + ":" + spell.name(), 0));
    }

    public List<String> castSources(SpellCatalog.Spell spell) {
        if (spell.level() == 0) return List.of("Cantrip");
        java.util.ArrayList<String> sources =
                new java.util.ArrayList<>(sorcerer() ? List.of("Spell slot") : List.of());
        if (selection.speciesSpell(spell)) sources.add("Species free cast");
        for (var grant : featSpellGrants())
            if (grant.spell().equals(spell.name()) && grant.dailyCast())
                sources.add(grant.source());
        if (feats().containsKey("Ritual Caster") && SpellDetails.forSpell(spell).ritual()) {
            sources.add("Ritual (no slot, +10 minutes)");
            sources.add("Quick Ritual");
        }
        return List.copyOf(sources);
    }

    public static String castSourceLabel(String source) {
        if (source == null) return "";
        if (source.startsWith("human.feat.")) return "Human · " + source.substring(11);
        if (source.startsWith("background.")) {
            String[] parts = source.split("\\.", 4);
            return parts[1] + " · " + parts[3];
        }
        if (source.startsWith("feat.")) return source.substring(5).replace(".copy.", " · Copy ");
        return source;
    }

    public int freeCasts(SpellCatalog.Spell spell, String source) {
        if (source.equals("Species free cast")) return freeCasts(spell);
        if (source.equals("Ritual (no slot, +10 minutes)")) return Integer.MAX_VALUE;
        if (source.equals("Quick Ritual"))
            return Math.max(0, 1 - speciesUses.getOrDefault("feat:Ritual Caster:Quick Ritual", 0));
        return Math.max(0, 1 - speciesUses.getOrDefault("feat:" + source + ":" + spell.name(), 0));
    }

    public String castFromSource(
            SpellCatalog.Spell spell, int slot, String source, Set<Metamagic> options) {
        if (source.equals("Spell slot") || source.equals("Cantrip"))
            return cast(spell, slot, false, options);
        if (source.equals("Species free cast")) return cast(spell, spell.level(), true, options);
        if (!castSources(spell).contains(source))
            return "This casting source does not grant this spell.";
        if (freeCasts(spell, source) == 0) return "No free casts remain until a Long Rest.";
        var context =
                new CastingRules.Context(
                        selection.level(),
                        innate,
                        selection.selected(spell),
                        selection.available(spell),
                        meta,
                        1,
                        0,
                        remainingPoints());
        String error = CastingRules.validate(spell, spell.level(), true, options, context);
        if (!error.isEmpty()) return error;
        error = equipmentCastingError();
        if (!error.isEmpty()) return error;
        points += metamagicCost(options);
        lastCastDetails = "";
        if (!source.equals("Ritual (no slot, +10 minutes)"))
            speciesUses.merge(
                    source.equals("Quick Ritual")
                            ? "feat:Ritual Caster:Quick Ritual"
                            : "feat:" + source + ":" + spell.name(),
                    1,
                    Integer::sum);
        if (SpellDetails.forSpell(spell).concentration()) concentration = spell.name();
        if (spell.name().equals("Mage Hand")) mageHandCast = true;
        changed();
        return "";
    }

    public String equipmentCastingError() {
        var training = ClassDefinition.forClass(selection.classId()).armorTraining();
        var armor = EquipmentRules.worn(sheet);
        if (!training.contains(armor.category())
                && !EquipmentRules.trained(armor, feats().keySet()))
            return "Cannot cast spells while wearing armor without its training.";
        if (!training.contains("Shield")
                && EquipmentRules.shield(sheet)
                && !feats().containsKey("Lightly Armored"))
            return "Cannot cast spells while using a shield without shield training.";
        return "";
    }

    public int metamagicCost(Set<Metamagic> options) {
        return CastingRules.metamagicCost(selection.level(), innate, options);
    }

    public String cast(
            SpellCatalog.Spell spell, int slot, boolean speciesFree, Set<Metamagic> options) {
        return cast(
                spell, slot, speciesFree, options, java.util.random.RandomGenerator.getDefault());
    }

    public String cast(
            SpellCatalog.Spell spell,
            int slot,
            boolean speciesFree,
            Set<Metamagic> options,
            java.util.random.RandomGenerator random) {
        Set<Metamagic> requested = Set.copyOf(options);
        var context =
                new CastingRules.Context(
                        selection.level(),
                        innate,
                        selection.selected(spell),
                        selection.available(spell),
                        meta,
                        freeCasts(spell),
                        slot >= 1 && slot <= RuleLimits.MAX_SPELL_LEVEL ? remainingSlots(slot) : 0,
                        remainingPoints());
        String error = CastingRules.validate(spell, slot, speciesFree, requested, context);
        if (!error.isEmpty()) return error;
        error = equipmentCastingError();
        if (!error.isEmpty()) return error;
        int cost = metamagicCost(requested);
        lastCastDetails = "";
        points += cost;
        if (spell.level() > 0) {
            if (speciesFree) {
                speciesUses.merge(selection.species().name() + ":" + spell.name(), 1, Integer::sum);
            } else {
                if (feats().containsKey("Boon of Spell Recall") && slot <= 4) {
                    int die = random.nextInt(1, 5);
                    lastCastDetails =
                            "Spell Recall d4 ["
                                    + die
                                    + "] · "
                                    + (die == slot ? "slot preserved" : "slot spent");
                    if (die != slot) slots[slot - 1]++;
                } else slots[slot - 1]++;
            }
        }
        if (SpellDetails.forSpell(spell).concentration()) {
            concentration = spell.name();
        }
        if (spell.name().equals("Mage Hand")) mageHandCast = true;
        changed();
        return "";
    }

    public void appearance(boolean dark, int textSize) {
        this.dark = dark;
        this.textSize = textSize;
        changed();
    }

    public String rollInitiative(int charisma, java.util.random.RandomGenerator random) {
        speciesUses.remove("feat-use:Boon of Fate");
        int die = random.nextInt(1, 21),
                bonus =
                        CharacterSheet.modifier(
                                        effectiveScore(CharacterSheet.Ability.DEX, charisma))
                                + (feats().containsKey("Alert")
                                        ? 2 + (selection.level() - 1) / 4
                                        : 0);
        boolean advantage = !sorcerer() && selection.level() >= 3;
        int second = advantage ? random.nextInt(1, 21) : die;
        changed();
        return "Initiative d20 ["
                + die
                + (advantage ? ", " + second + " · Advantage" : "")
                + "] "
                + String.format("%+d", bonus)
                + " = "
                + (Math.max(die, second) + bonus)
                + (feats().containsKey("Boon of Fate") ? "; Boon of Fate refreshed" : "");
    }

    public PlayState snapshot() {
        CharacterSheet savedSheet =
                sorcerer()
                        ? sheet
                        : sheet.withChoice(
                                "class.progression", List.of(selection.progression().encode()));
        return new PlayState(
                Arrays.stream(slots).boxed().toList(),
                points,
                restorationUsed,
                meta,
                concentration,
                speciesUses,
                innateUses,
                innate,
                dark,
                textSize,
                hitPoints,
                maxHitPoints,
                feats,
                savedSheet);
    }

    public void restore(PlayState state) {
        mageHandCast = false;
        lastCastDetails = "";
        sheet = state.sheet();
        restoreRevision++;
        feats.clear();
        feats.putAll(state.feats());
        hitPoints = state.hitPoints();
        maxHitPoints = state.maxHitPoints();
        for (int i = 0; i < RuleLimits.MAX_SPELL_LEVEL; i++) {
            slots[i] = state.spentSlots().get(i);
        }
        points = state.spentPoints();
        restorationUsed = state.restorationUsed();
        meta.clear();
        meta.addAll(state.metamagic());
        concentration = state.concentration();
        speciesUses.clear();
        speciesUses.putAll(state.speciesUses());
        innateUses = state.innateUses();
        innate = state.innateActive();
        dark = state.dark();
        textSize = state.textSize();
        changed();
    }
}
