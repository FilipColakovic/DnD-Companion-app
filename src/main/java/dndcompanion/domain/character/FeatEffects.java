package dndcompanion.domain.character;

import dndcompanion.domain.character.CharacterSheet.Ability;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/** Derived benefits never change persisted base scores or manual spell selections. */
public final class FeatEffects {
    private FeatEffects() {}

    public record SpellGrant(String source, String spell, Ability ability, boolean dailyCast) {}

    public static int copies(CharacterSheet sheet, String name) {
        if (!FeatCatalog.named(name).repeatable()) return 1;
        return sheet.choice("feat." + name + ".copies").stream()
                .findFirst()
                .map(Integer::parseInt)
                .orElse(1);
    }

    public static String prefix(String name, int copy) {
        return "feat." + name + (copy == 1 ? "" : ".copy." + copy);
    }

    public static List<CharacterChoices.Choice> configuredChoices(
            CharacterSheet sheet, String name, int level) {
        List<CharacterChoices.Choice> result = new ArrayList<>();
        if (FeatCatalog.named(name).repeatable())
            result.add(
                    new CharacterChoices.Choice(
                            "feat." + name + ".copies",
                            name + " · Copies acquired",
                            "Record copies actually acquired from character advancement; background"
                                    + " copies are configured separately.",
                            1,
                            name.equals("Magic Initiate")
                                    ? List.of("1", "2", "3")
                                    : List.of("1", "2", "3", "4", "5")));
        for (int copy = 1; copy <= copies(sheet, name); copy++) {
            String prefix = prefix(name, copy);
            for (var choice : choices(name, level)) {
                List<String> options = choice.options();
                if (name.equals("Elemental Adept") && choice.id().endsWith(".damage")) {
                    Set<String> other = new java.util.HashSet<>();
                    for (int index = 1; index <= copies(sheet, name); index++)
                        if (index != copy)
                            other.addAll(sheet.choice(prefix(name, index) + ".damage"));
                    options = options.stream().filter(type -> !other.contains(type)).toList();
                }
                result.add(
                        new CharacterChoices.Choice(
                                choice.id().replace("feat." + name, prefix),
                                choice.title() + (copy > 1 ? " · Copy " + copy : ""),
                                choice.explanation(),
                                choice.count(),
                                options));
            }
        }
        return List.copyOf(result);
    }

    public static List<Ability> abilities(FeatCatalog.Feat feat) {
        String increase = feat.abilityIncrease();
        if (increase.isEmpty()) return List.of();
        if (increase.startsWith("Any") || increase.startsWith("Chosen"))
            return Arrays.asList(Ability.values());
        return Arrays.stream(Ability.values()).filter(a -> increase.contains(a.name())).toList();
    }

    public static List<CharacterChoices.Choice> choices(String name, int level) {
        var feat = FeatCatalog.named(name);
        List<CharacterChoices.Choice> result = new ArrayList<>();
        List<Ability> allowed = abilities(feat);
        if (name.equals("Ability Score Improvement")) {
            List<String> allocations = new ArrayList<>();
            for (Ability a : Ability.values()) {
                allocations.add(a + " +2");
                for (Ability b : Ability.values())
                    if (a.ordinal() < b.ordinal()) allocations.add(a + " +1 / " + b + " +1");
            }
            result.add(choice(name, "ability", "Ability increases", 1, allocations));
        } else if (allowed.size() > 1) {
            if (name.equals("Resilient"))
                allowed =
                        allowed.stream().filter(a -> a != Ability.CON && a != Ability.CHA).toList();
            result.add(
                    choice(
                            name,
                            "ability",
                            "Ability increase",
                            1,
                            allowed.stream().map(Enum::name).toList()));
        }
        switch (name) {
            case "Fey-Touched" ->
                    result.add(
                            choice(
                                    name,
                                    "spell",
                                    "Additional prepared spell",
                                    1,
                                    List.of(
                                            "Animal Friendship",
                                            "Bane",
                                            "Bless",
                                            "Charm Person",
                                            "Command",
                                            "Compelled Duel",
                                            "Comprehend Languages",
                                            "Detect Evil and Good",
                                            "Detect Magic",
                                            "Detect Poison and Disease",
                                            "Dissonant Whispers",
                                            "Hex",
                                            "Hunter's Mark",
                                            "Heroism",
                                            "Identify",
                                            "Sleep",
                                            "Speak with Animals",
                                            "Tasha's Hideous Laughter")));
            case "Shadow-Touched" ->
                    result.add(
                            choice(
                                    name,
                                    "spell",
                                    "Additional prepared spell",
                                    1,
                                    List.of(
                                            "Color Spray",
                                            "Disguise Self",
                                            "False Life",
                                            "Illusory Script",
                                            "Inflict Wounds",
                                            "Ray of Sickness",
                                            "Silent Image")));
            case "Ritual Caster" ->
                    result.add(
                            choice(
                                    name,
                                    "spells",
                                    "Prepared rituals",
                                    2 + (level - 1) / 4,
                                    List.of(
                                            "Alarm",
                                            "Comprehend Languages",
                                            "Detect Magic",
                                            "Detect Poison and Disease",
                                            "Find Familiar",
                                            "Identify",
                                            "Illusory Script",
                                            "Purify Food and Drink",
                                            "Speak with Animals",
                                            "Tenser's Floating Disk",
                                            "Unseen Servant")));
            case "Elemental Adept" ->
                    result.add(
                            choice(
                                    name,
                                    "damage",
                                    "Damage type",
                                    1,
                                    List.of("Acid", "Cold", "Fire", "Lightning", "Thunder")));
            case "Boon of Energy Resistance" ->
                    result.add(
                            choice(
                                    name,
                                    "damage",
                                    "Resistances",
                                    2,
                                    List.of(
                                            "Acid",
                                            "Cold",
                                            "Fire",
                                            "Lightning",
                                            "Necrotic",
                                            "Poison",
                                            "Psychic",
                                            "Radiant",
                                            "Thunder")));
            case "Boon of Skill" ->
                    result.add(
                            choice(
                                    name,
                                    "expertise",
                                    "Expertise",
                                    1,
                                    Arrays.stream(CharacterSheet.Skill.values())
                                            .map(s -> s.label)
                                            .toList()));
            case "Weapon Master" ->
                    result.add(
                            choice(
                                    name,
                                    "weapon",
                                    "Weapon mastery",
                                    1,
                                    EquipmentRules.weapons().stream()
                                            .filter(w -> !w.properties().contains("Unarmed"))
                                            .map(EquipmentRules.Weapon::name)
                                            .toList()));
            default -> {}
        }
        return List.copyOf(result);
    }

    public static String passives(CharacterSheet sheet, Set<String> feats) {
        List<String> result = new ArrayList<>();
        if (feats.contains("Lightly Armored")) result.add("Light armor and shield training");
        if (feats.contains("Moderately Armored")) result.add("Medium armor training");
        if (feats.contains("Heavily Armored")) result.add("Heavy armor training");
        if (feats.contains("Martial Weapon Training")) result.add("Martial weapon proficiency");
        if (feats.contains("Tavern Brawler"))
            result.add("Improvised weapon proficiency; unarmed d4 and reroll 1s");
        if (feats.contains("Blind Fighting") || feats.contains("Skulker"))
            result.add("Blindsight 10 feet");
        if (feats.contains("Boon of Truesight")) result.add("Truesight 60 feet");
        if (feats.contains("Boon of Energy Resistance"))
            result.add(
                    "Resistance: "
                            + String.join(
                                    ", ", sheet.choice("feat.Boon of Energy Resistance.damage")));
        if (feats.contains("Boon of the Night Spirit"))
            result.add("In dim light/darkness: resistance except Psychic and Radiant");
        if (feats.contains("Elemental Adept"))
            result.add(
                    "Elemental Adept: "
                            + String.join(", ", elementalTypes(sheet))
                            + " spell damage ignores resistance; 1s count as 2");
        if (feats.contains("Poisoner")) result.add("Poison damage ignores resistance");
        if (feats.contains("War Caster"))
            result.add("Concentration save advantage; somatic components with occupied hands");
        if (feats.contains("Durable")) result.add("Death saving throw advantage");
        if (feats.contains("Athlete"))
            result.add("Climb speed equals speed; stand/jump run-up 5 feet");
        if (feats.contains("Crafter")) result.add("20% nonmagical shopping discount");
        return String.join("\n", result);
    }

    public static Set<String> elementalTypes(CharacterSheet sheet) {
        Set<String> result = new java.util.TreeSet<>();
        for (int copy = 1; copy <= copies(sheet, "Elemental Adept"); copy++)
            result.addAll(sheet.choice(prefix("Elemental Adept", copy) + ".damage"));
        return Set.copyOf(result);
    }

    private static CharacterChoices.Choice choice(
            String feat, String suffix, String title, int count, List<String> options) {
        return new CharacterChoices.Choice(
                "feat." + feat + "." + suffix,
                feat + " · " + title,
                title.equals("Ability increase")
                        ? "Adds +1 automatically while this feat is equipped; your base score is"
                                + " preserved."
                        : "Confirm these choices to apply the feat's benefits.",
                count,
                options);
    }

    public static int score(
            CharacterSheet sheet, Set<String> feats, Ability ability, int charisma) {
        int base = sheet.score(ability, charisma), normal = 0, epic = 0;
        for (String name : feats) {
            var feat = FeatCatalog.named(name);
            List<Ability> allowed = abilities(feat);
            for (int copy = 1; copy <= copies(sheet, name); copy++) {
                int bonus = 0;
                if (name.equals("Ability Score Improvement")) {
                    String allocation =
                            sheet.choice(prefix(name, copy) + ".ability").stream()
                                    .findFirst()
                                    .orElse("");
                    bonus =
                            allocation.contains(ability + " +2")
                                    ? 2
                                    : allocation.contains(ability + " +1") ? 1 : 0;
                } else if (allowed.size() == 1 && allowed.contains(ability)
                        || allowed.size() > 1
                                && sheet.choice(prefix(name, copy) + ".ability")
                                        .contains(ability.name())) {
                    bonus = 1;
                }
                if (feat.category() == FeatCatalog.Category.EPIC_BOON) epic += bonus;
                else normal += bonus;
            }
        }
        return Math.min(30, Math.max(base, Math.min(20, base + normal)) + epic);
    }

    public static Ability ability(CharacterSheet sheet, String feat) {
        var allowed = abilities(FeatCatalog.named(feat));
        if (allowed.isEmpty()) return Ability.CHA;
        if (allowed.size() == 1) return allowed.getFirst();
        return sheet.choice("feat." + feat + ".ability").stream()
                .findFirst()
                .map(Ability::valueOf)
                .orElse(Ability.CHA);
    }

    public static List<SpellGrant> spells(
            CharacterSheet sheet, Species species, Set<String> manual) {
        Set<String> feats =
                CharacterChoices.feats(
                                sheet,
                                species,
                                manual.stream()
                                        .collect(
                                                java.util.stream.Collectors.toMap(n -> n, n -> "")))
                        .keySet();
        List<SpellGrant> result = new ArrayList<>();
        for (String name : feats) {
            if (!Set.of(
                            "Telekinetic",
                            "Telepathic",
                            "Fey-Touched",
                            "Shadow-Touched",
                            "Ritual Caster")
                    .contains(name)) continue;
            Ability ability = ability(sheet, name);
            switch (name) {
                case "Telekinetic" -> result.add(new SpellGrant(name, "Mage Hand", ability, false));
                case "Telepathic" ->
                        result.add(new SpellGrant(name, "Detect Thoughts", ability, true));
                case "Fey-Touched", "Shadow-Touched" -> {
                    result.add(
                            new SpellGrant(
                                    name,
                                    name.equals("Fey-Touched") ? "Misty Step" : "Invisibility",
                                    ability,
                                    true));
                    for (String spell : sheet.choice("feat." + name + ".spell"))
                        result.add(new SpellGrant(name, spell, ability, true));
                }
                case "Ritual Caster" -> {
                    for (String spell : sheet.choice("feat." + name + ".spells"))
                        result.add(new SpellGrant(name, spell, ability, false));
                }
                default -> {}
            }
        }
        if (manual.contains("Magic Initiate")
                || species == Species.HUMAN
                        && sheet.choice(ChoiceIds.HUMAN_FEAT).contains("Magic Initiate"))
            for (int copy = 1; copy <= copies(sheet, "Magic Initiate"); copy++)
                initiate(result, sheet, prefix("Magic Initiate", copy), "");
        BackgroundCatalog.selected(sheet)
                .filter(b -> b.feat().equals("Magic Initiate"))
                .ifPresent(
                        b ->
                                initiate(
                                        result,
                                        sheet,
                                        b.id() + ".feat.Magic Initiate",
                                        b.spellList()));
        return List.copyOf(result);
    }

    private static void initiate(
            List<SpellGrant> result, CharacterSheet sheet, String prefix, String fixedList) {
        String list =
                fixedList.isEmpty()
                        ? sheet.choice(prefix + ".list").stream().findFirst().orElse("")
                        : fixedList;
        if (list.isEmpty()) return;
        Ability ability =
                switch (sheet.choice(prefix + ".ability").stream().findFirst().orElse("Charisma")) {
                    case "Intelligence" -> Ability.INT;
                    case "Wisdom" -> Ability.WIS;
                    default -> Ability.CHA;
                };
        for (var choice : CharacterChoices.initiateChoices(sheet, prefix, fixedList)) {
            if (!choice.complete(sheet)) continue;
            if (choice.id().endsWith(".spell") || choice.id().endsWith(".cantrips"))
                for (String spell : sheet.choice(choice.id()))
                    result.add(
                            new SpellGrant(prefix, spell, ability, choice.id().endsWith(".spell")));
        }
    }

    public static String spellNotes(Set<String> feats, String spell) {
        List<String> notes = new ArrayList<>();
        if (feats.contains("Telekinetic") && spell.equals("Mage Hand"))
            notes.add(
                    "Telekinetic: invisible hand, no Verbal or Somatic components; range and"
                            + " distance +30 feet.");
        if (feats.contains("Telepathic") && spell.equals("Detect Thoughts"))
            notes.add(
                    "Telepathic: the daily free cast requires no components; spell-slot casts use"
                            + " the normal components.");
        if (feats.contains("War Caster"))
            notes.add(
                    "War Caster: advantage on concentration saves; somatic components with occupied"
                            + " hands.");
        if (feats.contains("Spell Sniper"))
            notes.add(
                    "Spell Sniper: spell attacks ignore half/three-quarters cover and nearby-enemy"
                            + " disadvantage; eligible attack ranges +60 feet.");
        if (feats.contains("Mage Slayer"))
            notes.add(
                    "Mage Slayer: damaged concentrating targets have disadvantage on concentration"
                            + " saves.");
        return String.join("\n", notes);
    }
}
