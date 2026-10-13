package org.example.domain.character;

import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;

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
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();
    private final int[] slots = new int[9];
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
                sheet.score(CharacterSheet.Ability.CON, 10),
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
        return CharacterChoices.required(sheet, selection.species(), feats.keySet());
    }

    public int pendingDecisions() {
        return (int) requiredChoices().stream().filter(choice -> !choice.complete(sheet)).count();
    }

    public Set<CharacterSheet.Skill> grantedSkills() {
        return CharacterChoices.skills(sheet, selection.species(), feats.keySet());
    }

    public Set<CharacterSheet.Skill> expertiseSkills() {
        return CharacterChoices.expertise(sheet, selection.species(), feats.keySet());
    }

    public int skillTier(CharacterSheet.Skill skill) {
        return Math.max(
                sheet.tier(skill, grantedSkills()), expertiseSkills().contains(skill) ? 2 : 0);
    }

    public int skillTierExcluding(CharacterSheet.Skill skill, String excludedFeat) {
        Set<String> active = new HashSet<>(feats.keySet());
        active.remove(excludedFeat);
        return Math.max(
                sheet.tier(skill, CharacterChoices.skills(sheet, selection.species(), active)),
                CharacterChoices.expertise(sheet, selection.species(), active).contains(skill)
                        ? 2
                        : 0);
    }

    public int skillBonus(CharacterSheet.Skill skill, int charisma) {
        int proficiency =
                SorcererRules.calculate(selection.level(), charisma, selection.subclass())
                        .proficiencyBonus();
        return CharacterSheet.modifier(sheet.score(skill.ability, charisma))
                + skillTier(skill) * proficiency;
    }

    public List<String> grantedTools() {
        Set<String> active = new HashSet<>(feats.keySet());
        if (selection.species() == Species.HUMAN) active.addAll(sheet.choice("human.feat"));
        return CharacterChoices.grantedTools(sheet, active);
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
        if (sheet.choice("background").equals(background == null ? List.of() : List.of(name)))
            return;
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
        return !classSpell && selection.speciesSpell(spell)
                ? CharacterChoices.speciesAbility(sheet, selection.species())
                : CharacterSheet.Ability.CHA;
    }

    public int spellcastingModifier(SpellCatalog.Spell spell, int charisma) {
        return CharacterSheet.modifier(sheet.score(spellcastingAbility(spell), charisma));
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
        changed();
        return "";
    }

    public void removeFeat(String name) {
        if (backgroundFeat(name) && !feats.containsKey(name)) return;
        boolean removed = feats.remove(name) != null;
        if (selection.species() == Species.HUMAN && sheet.choice("human.feat").contains(name)) {
            sheet = sheet.withChoice("human.feat", List.of());
            removed = true;
        }
        if (removed) changed();
    }

    private final Map<String, Integer> speciesUses = new HashMap<>();

    public PlaySession(SpellSelection selection) {
        this.selection = selection;
        selection.addChangeListener(this::holdZeroHp);
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

    private void changed() {
        holdZeroHp();
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
        speciesUses.clear();
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

    public int metamagicCost(Set<Metamagic> options) {
        int cost = options.stream().mapToInt(Metamagic::cost).sum();
        if (selection.level() == 20 && innate) {
            cost -= options.stream().mapToInt(Metamagic::cost).max().orElse(0);
        }
        return cost;
    }

    public String cast(
            SpellCatalog.Spell spell, int slot, boolean speciesFree, Set<Metamagic> options) {
        if (!selection.selected(spell) || !selection.available(spell)) {
            return "Choose an available prepared spell or known cantrip.";
        }
        if (!options.isEmpty()
                && (selection.level() < 2 || meta.size() > Metamagic.limit(selection.level()))) {
            return "Metamagic choices exceed your current level's limit.";
        }
        for (Metamagic option : options) {
            if (!meta.contains(option) || !option.applies(spell)) {
                return "Metamagic is not selected or is incompatible with this spell.";
            }
        }
        long ordinary = options.stream().filter(option -> !option.combinable()).count();
        if (ordinary > (selection.level() >= 7 && innate ? 2 : 1)) {
            return "These Metamagic options cannot be combined right now.";
        }
        if (spell.level() == 0 && (slot != 0 || speciesFree)) {
            return "Cantrips do not spend a slot or a species free cast.";
        }
        if (spell.level() > 0) {
            if (speciesFree) {
                if (freeCasts(spell) == 0 || slot != spell.level()) {
                    return "No species free casts remain, or slot level differs from the spell's"
                               + " base level.";
                }
            } else if (slot < spell.level() || slot > 9 || remainingSlots(slot) == 0) {
                return "No appropriate spell slot remaining.";
            }
        }
        if (options.contains(Metamagic.TWINNED) && slot >= 9) {
            return "Twinned Spell cannot raise the effective level above 9.";
        }
        int cost = metamagicCost(options);
        if (remainingPoints() < cost) {
            return "Not enough Sorcery Points.";
        }
        points += cost;
        if (spell.level() > 0) {
            if (speciesFree) {
                speciesUses.merge(selection.species().name() + ":" + spell.name(), 1, Integer::sum);
            } else {
                slots[slot - 1]++;
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
        sheet = state.sheet();
        restoreRevision++;
        feats.clear();
        feats.putAll(state.feats());
        hitPoints = state.hitPoints();
        maxHitPoints = state.maxHitPoints();
        for (int i = 0; i < 9; i++) {
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
