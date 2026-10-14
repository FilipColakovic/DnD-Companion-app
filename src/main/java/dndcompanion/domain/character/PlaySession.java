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
                selection.level(),
                effectiveScore(CharacterSheet.Ability.CON, 10),
                selection.species(),
                selection.subclass(),
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
        return CharacterChoices.feats(sheet, selection.species(), feats);
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
            if (ability != CharacterSheet.Ability.CHA)
                effective = effective.withAbility(ability, effectiveScore(ability, 10));
        return effective;
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
        return ability == CharacterSheet.Ability.CON
                || ability == CharacterSheet.Ability.CHA
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
        if (!Set.of(1, 4, 6, 8, 10, 12).contains(rerollSides))
            return new FeatResult("Choose a valid damage die.", "");
        if (!FeatActions.all().contains(action) || !feats().containsKey(action.feat()))
            return new FeatResult("This feat is not equipped.", "");
        if (remainingFeatUses(action) == 0)
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
                && requiredChoices().stream()
                        .anyMatch(
                                c ->
                                        c.id().equals("feat." + feat + ".ability")
                                                && !c.complete(sheet)))
            return new FeatResult("Choose this feat's ability increase in Character first.", "");
        int pb = 2 + (selection.level() - 1) / 4;
        int modifier =
                CharacterSheet.modifier(effectiveScore(FeatEffects.ability(sheet, feat), charisma));
        String effect = action.trigger();
        if (feat.equals("Crafter")) {
            if (!FeatActions.craftingItems(grantedTools()).contains(item))
                return new FeatResult("Choose gear supported by your proficient tools.", "");
            sheet = sheet.withNote(CharacterSheet.Note.CRAFTED_ITEM, item);
            effect = "Crafted " + item + "; expires at your next Long Rest.";
        }
        int healing = 0;
        if (feat.equals("Durable") || feat.equals("Healer")) {
            if (feat.equals("Healer") && sheet.field(CharacterSheet.Field.HEALER_KIT_USES) == 0)
                return new FeatResult("Add available Healer's Kit uses in Character first.", "");
            if (sheet.field(CharacterSheet.Field.SPENT_HIT_DICE) >= selection.level()
                    || hitPoints() <= 0)
                return new FeatResult("No available Hit Dice, or character is at zero HP.", "");
            int die = random.nextInt(1, 7);
            if (feat.equals("Healer") && die == 1) die = random.nextInt(1, 7);
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
                    "d6 ["
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
            int temporary = Math.max(0, selection.level() + modifier);
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
                                            effectiveScore(CharacterSheet.Ability.CON, charisma)))
                            + "; failure: "
                            + (roll(random, 2, 12)
                                    + CharacterSheet.modifier(
                                            effectiveScore(CharacterSheet.Ability.CON, charisma)))
                            + " damage of resisted type.";
        else if (feat.equals("Boon of Fate"))
            effect = "Add or subtract " + roll(random, 2, 4) + " from nearby D20 Test.";
        else if (feat.equals("Boon of Fortitude"))
            healing =
                    Math.max(
                            0,
                            CharacterSheet.modifier(
                                    effectiveScore(CharacterSheet.Ability.CON, charisma)));
        else if (feat.equals("Boon of Irresistible Offense"))
            effect =
                    "Natural 20: extra "
                            + effectiveScore(FeatEffects.ability(sheet, feat), charisma)
                            + " damage of the attack's type.";
        else if (feat.equals("Charger") && id.equals("charge"))
            effect = "Charge extra damage " + random.nextInt(1, 9);
        else if (feat.equals("Defensive Duelist"))
            effect =
                    "Reaction: AC "
                            + (armorClass(charisma) + pb)
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
                                            effectiveScore(CharacterSheet.Ability.STR, charisma)))
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
        changed();
        return new FeatResult("", feat + " · " + action.label() + ": " + effect);
    }

    private static int roll(java.util.random.RandomGenerator random, int count, int sides) {
        return java.util.stream.IntStream.range(0, count)
                .map(i -> random.nextInt(1, sides + 1))
                .sum();
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
        return grants().tier(sheet, skill);
    }

    /** New proficiency choices exclude their own grant so saved choices remain editable. */
    public boolean proficiencyOptionAvailable(String choiceId, String option) {
        boolean newProficiency =
                choiceId.equals(ChoiceIds.SORCERER_SKILLS)
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
        int proficiency =
                SorcererRules.calculate(selection.level(), charisma, selection.subclass())
                        .proficiencyBonus();
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
                SpellCatalog.alwaysPrepared(spell, selection.level(), selection.subclass())
                        || selection.chosenSpells().contains(spell.name()) && spell.sorcererSpell();
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
        String error = feat.requirements(selection.level(), charisma);
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
        feats.put(feat.name(), notes);
        if (feat.repeatable() && sheet.choice("feat." + feat.name() + ".copies").isEmpty())
            sheet = sheet.withChoice("feat." + feat.name() + ".copies", List.of("1"));
        changed();
        return "";
    }

    public void removeFeat(String name) {
        if (backgroundFeat(name) && !feats.containsKey(name)) return;
        boolean removed = feats.remove(name) != null;
        if (selection.species() == Species.HUMAN
                && sheet.choice(ChoiceIds.HUMAN_FEAT).contains(name)) {
            sheet = sheet.withChoice(ChoiceIds.HUMAN_FEAT, List.of());
            removed = true;
        }
        if (removed) changed();
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
        return selection.level() == 1 ? 0 : selection.level();
    }

    public int remainingPoints() {
        return Math.max(0, maxPoints() - points);
    }

    public int maxSlots(int slot) {
        return SorcererRules.calculate(selection.level(), 17, selection.subclass())
                .spellSlots()
                .get(slot - 1);
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

    public String toggleInnate() {
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
        java.util.ArrayList<String> sources = new java.util.ArrayList<>(List.of("Spell slot"));
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
        changed();
        return "";
    }

    public String equipmentCastingError() {
        if (!EquipmentRules.trained(EquipmentRules.worn(sheet), feats().keySet()))
            return "Cannot cast spells while wearing armor without its training.";
        if (EquipmentRules.shield(sheet) && !feats().containsKey("Lightly Armored"))
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
        changed();
        return "Initiative d20 ["
                + die
                + "] "
                + String.format("%+d", bonus)
                + " = "
                + (die + bonus)
                + (feats().containsKey("Boon of Fate") ? "; Boon of Fate refreshed" : "");
    }

    public PlayState snapshot() {
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
                sheet);
    }

    public void restore(PlayState state) {
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
