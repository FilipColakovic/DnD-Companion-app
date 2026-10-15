package dndcompanion.persistence;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlayState;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.HashSet;

/** Explicit versioned data format. No Java type names or polymorphic deserialization. */
public final class CharacterJson {
    private final JsonMapper mapper = JsonMapper.builder().build();

    public String encode(CharacterState state) {
        var root = mapper.createObjectNode();
        root.put("format", "dndcompanion.character");
        root.put("version", 1);
        root.put("name", state.name());
        root.put("level", state.level());
        root.put("charisma", state.charisma());
        root.put("subclass", state.subclass().name());
        root.put("species", state.species().name());
        var classes = root.putArray("classes");
        for (var progression : state.classes())
            classes.addObject()
                    .put("classId", progression.classId().name())
                    .put("level", progression.level())
                    .put("subclass", progression.subclass());
        var abilities = root.putObject("abilities");
        for (var ability : dndcompanion.domain.character.CharacterSheet.Ability.values())
            abilities.put(ability.name(), state.play().sheet().score(ability, state.charisma()));
        var spells = root.putArray("spells");
        state.chosenSpells().stream().sorted().forEach(spells::add);
        root.put("play", state.play().encode());
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
    }

    public CharacterState decode(String json) {
        if (json.length() > 4_000_000)
            throw new IllegalArgumentException("Character file is too large.");
        JsonNode root = mapper.readTree(json);
        if (root == null
                || !root.isObject()
                || !text(root, "format").equals("dndcompanion.character")
                || number(root, "version") != 1)
            throw new IllegalArgumentException("Unsupported character file format or version.");
        if (!root.path("spells").isArray())
            throw new IllegalArgumentException("Missing spell selections.");
        var spells = new HashSet<String>();
        for (JsonNode spell : root.path("spells")) {
            if (!spell.isString() || !spells.add(spell.asString()))
                throw new IllegalArgumentException("Invalid or repeated spell selection.");
        }
        if (!root.path("classes").isArray() || root.path("classes").size() != 1)
            throw new IllegalArgumentException(
                    "Only single-class characters are currently supported.");
        JsonNode cls = root.path("classes").get(0);
        var progression =
                new dndcompanion.domain.character.ClassProgression(
                        dndcompanion.domain.character.ClassId.valueOf(text(cls, "classId")),
                        number(cls, "level"),
                        text(cls, "subclass"));
        if (progression.level() != number(root, "level"))
            throw new IllegalArgumentException("Class and character levels do not match.");
        if (!root.path("abilities").isObject())
            throw new IllegalArgumentException("Missing ability scores.");
        var play = PlayState.decode(text(root, "play"));
        var sheet = play.sheet();
        var storedProgression = sheet.choice("class.progression");
        if (!storedProgression.isEmpty()
                && !storedProgression.equals(java.util.List.of(progression.encode())))
            throw new IllegalArgumentException("Conflicting class progression in character data.");
        for (var ability : dndcompanion.domain.character.CharacterSheet.Ability.values())
            sheet = sheet.withAbility(ability, number(root.path("abilities"), ability.name()));
        if (sheet.score(dndcompanion.domain.character.CharacterSheet.Ability.CHA, 10)
                != number(root, "charisma"))
            throw new IllegalArgumentException(
                    "Ability scores do not match legacy Charisma field.");
        if (progression.classId() == dndcompanion.domain.character.ClassId.FIGHTER)
            sheet = sheet.withChoice("class.progression", java.util.List.of(progression.encode()));
        else if (progression.classId()
                        != dndcompanion.domain.character.ClassProgression.classId(sheet)
                || !progression.subclass().equals(text(root, "subclass")))
            throw new IllegalArgumentException("Class metadata does not match the character.");
        return new CharacterState(
                number(root, "level"),
                number(root, "charisma"),
                SorcererRules.Subclass.valueOf(text(root, "subclass")),
                spells,
                Species.valueOf(text(root, "species")),
                play.withSheet(sheet),
                text(root, "name"));
    }

    private static String text(JsonNode root, String key) {
        JsonNode value = root.get(key);
        if (value == null || !value.isString())
            throw new IllegalArgumentException("Missing or invalid " + key);
        return value.asString();
    }

    private static int number(JsonNode root, String key) {
        JsonNode value = root.get(key);
        if (value == null || !value.isInt())
            throw new IllegalArgumentException("Missing or invalid " + key);
        return value.intValue();
    }
}
