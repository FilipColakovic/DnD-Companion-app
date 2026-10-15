package dndcompanion.domain.character;

import java.util.List;

/** Collection-shaped storage is ready for multiclass; only one class is currently supported. */
public record ClassProgression(ClassId classId, int level, String subclass) {
    public ClassProgression {
        if (classId == null || !classId.supported() || level < 1 || level > 20)
            throw new IllegalArgumentException("Choose a supported single class and level 1–20.");
        if (classId == ClassId.SORCERER) SorcererRules.Subclass.valueOf(subclass);
        else if (!"CHAMPION".equals(subclass))
            throw new IllegalArgumentException("Only Champion is implemented for Fighter.");
    }

    public String encode() {
        return classId.name() + ":" + level + ":" + subclass;
    }

    public static ClassProgression decode(String value) {
        String[] parts = value.split(":", -1);
        if (parts.length != 3) throw new IllegalArgumentException("Invalid class progression.");
        return new ClassProgression(
                ClassId.valueOf(parts[0]), Integer.parseInt(parts[1]), parts[2]);
    }

    public static ClassId classId(CharacterSheet sheet) {
        var classes = sheet.choice("class.progression");
        return classes.isEmpty() ? ClassId.SORCERER : decode(classes.getFirst()).classId();
    }

    public String subclassLabel() {
        return classId == ClassId.FIGHTER
                ? level < 3 ? "Fighter" : "Champion"
                : SorcererRules.Subclass.valueOf(subclass).toString();
    }

    public static List<ClassProgression> from(CharacterState state) {
        var values = state.play().sheet().choice("class.progression");
        return values.isEmpty()
                ? List.of(
                        new ClassProgression(
                                ClassId.SORCERER, state.level(), state.subclass().name()))
                : values.stream().map(ClassProgression::decode).toList();
    }
}
