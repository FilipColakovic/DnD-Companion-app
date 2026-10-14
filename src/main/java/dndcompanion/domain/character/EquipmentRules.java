package dndcompanion.domain.character;

import java.util.List;
import java.util.Set;

/** PHB 2024 mundane equipment, with derived armor and weapon benefits. */
public final class EquipmentRules {
    private EquipmentRules() {}

    public record Armor(String name, String category, int ac, int dexCap) {}

    public record Weapon(
            String name,
            boolean martial,
            int die,
            String damage,
            String properties,
            String mastery) {}

    private static final List<Armor> ARMOR =
            List.of(
                    new Armor("Unarmored", "None", 10, 99),
                    new Armor("Padded", "Light", 11, 99),
                    new Armor("Leather", "Light", 11, 99),
                    new Armor("Studded Leather", "Light", 12, 99),
                    new Armor("Hide", "Medium", 12, 2),
                    new Armor("Chain Shirt", "Medium", 13, 2),
                    new Armor("Scale Mail", "Medium", 14, 2),
                    new Armor("Breastplate", "Medium", 14, 2),
                    new Armor("Half Plate", "Medium", 15, 2),
                    new Armor("Ring Mail", "Heavy", 14, 0),
                    new Armor("Chain Mail", "Heavy", 16, 0),
                    new Armor("Splint", "Heavy", 17, 0),
                    new Armor("Plate", "Heavy", 18, 0));
    private static final String DATA =
"""
Unarmed|false|1|Bludgeoning|Unarmed|None
Club|false|4|Bludgeoning|Light|Slow
Dagger|false|4|Piercing|Finesse Light Thrown|Nick
Greatclub|false|8|Bludgeoning|Two-Handed|Push
Handaxe|false|6|Slashing|Light Thrown|Vex
Javelin|false|6|Piercing|Thrown|Slow
Light Hammer|false|4|Bludgeoning|Light Thrown|Nick
Mace|false|6|Bludgeoning||Sap
Quarterstaff|false|6|Bludgeoning|Versatile|Topple
Sickle|false|4|Slashing|Light|Nick
Spear|false|6|Piercing|Thrown Versatile|Sap
Dart|false|4|Piercing|Finesse Ranged Thrown|Vex
Light Crossbow|false|8|Piercing|Ranged Loading Two-Handed|Slow
Shortbow|false|6|Piercing|Ranged Two-Handed|Vex
Sling|false|4|Bludgeoning|Ranged|Slow
Battleaxe|true|8|Slashing|Versatile|Topple
Flail|true|8|Bludgeoning||Sap
Glaive|true|10|Slashing|Heavy Reach Two-Handed|Graze
Greataxe|true|12|Slashing|Heavy Two-Handed|Cleave
Greatsword|true|6|Slashing|Heavy Two-Handed 2d6|Graze
Halberd|true|10|Slashing|Heavy Reach Two-Handed|Cleave
Lance|true|10|Piercing|Heavy Reach Two-Handed|Topple
Longsword|true|8|Slashing|Versatile|Sap
Maul|true|6|Bludgeoning|Heavy Two-Handed 2d6|Topple
Morningstar|true|8|Piercing||Sap
Pike|true|10|Piercing|Heavy Reach Two-Handed|Push
Rapier|true|8|Piercing|Finesse|Vex
Scimitar|true|6|Slashing|Finesse Light|Nick
Shortsword|true|6|Piercing|Finesse Light|Vex
Trident|true|8|Piercing|Thrown Versatile|Topple
Warhammer|true|8|Bludgeoning|Versatile|Push
War Pick|true|8|Piercing|Versatile|Sap
Whip|true|4|Slashing|Finesse Reach|Slow
Blowgun|true|1|Piercing|Ranged Loading|Vex
Hand Crossbow|true|6|Piercing|Ranged Light Loading|Vex
Heavy Crossbow|true|10|Piercing|Ranged Heavy Loading Two-Handed|Push
Longbow|true|8|Piercing|Ranged Heavy Two-Handed|Slow
Musket|true|12|Piercing|Ranged Loading Two-Handed|Slow
Pistol|true|10|Piercing|Ranged Loading|Vex
""";
    private static final List<Weapon> WEAPONS =
            DATA.lines()
                    .map(
                            s -> {
                                String[] p = s.split("\\|", -1);
                                return new Weapon(
                                        p[0],
                                        Boolean.parseBoolean(p[1]),
                                        Integer.parseInt(p[2]),
                                        p[3],
                                        p[4],
                                        p[5]);
                            })
                    .toList();

    public static List<Armor> armor() {
        return ARMOR;
    }

    public static List<Weapon> weapons() {
        return WEAPONS;
    }

    public static Armor worn(CharacterSheet sheet) {
        String name = sheet.choice("equipment.armor").stream().findFirst().orElse("Unarmored");
        return ARMOR.stream().filter(a -> a.name().equals(name)).findFirst().orElseThrow();
    }

    public static Weapon weapon(CharacterSheet sheet) {
        String name = sheet.choice("equipment.weapon").stream().findFirst().orElse("Quarterstaff");
        return WEAPONS.stream().filter(w -> w.name().equals(name)).findFirst().orElseThrow();
    }

    public static boolean shield(CharacterSheet sheet) {
        return sheet.choice("equipment.shield").contains("Shield");
    }

    public static boolean trained(Armor armor, Set<String> feats) {
        return armor.category().equals("None")
                || switch (armor.category()) {
                    case "Light" -> feats.contains("Lightly Armored");
                    case "Medium" -> feats.contains("Moderately Armored");
                    case "Heavy" -> feats.contains("Heavily Armored");
                    default -> false;
                };
    }

    public static int ac(CharacterSheet sheet, Set<String> feats, int dexterity) {
        if (sheet.field(CharacterSheet.Field.AC_OVERRIDE) > 0)
            return sheet.field(CharacterSheet.Field.AC_OVERRIDE);
        Armor armor = worn(sheet);
        int cap =
                armor.category().equals("Medium")
                                && feats.contains("Medium Armor Master")
                                && dexterity >= 16
                        ? 3
                        : armor.dexCap();
        return armor.ac()
                + (cap == 0 ? 0 : Math.min(cap, CharacterSheet.modifier(dexterity)))
                + (shield(sheet) ? 2 : 0)
                + (!armor.category().equals("None") && feats.contains("Defense") ? 1 : 0);
    }

    public static List<CharacterChoices.Choice> choices() {
        java.util.ArrayList<String> offhand = new java.util.ArrayList<>(List.of("None"));
        offhand.addAll(
                WEAPONS.stream()
                        .filter(
                                w ->
                                        !w.properties().contains("Two-Handed")
                                                && !w.properties().contains("Unarmed"))
                        .map(Weapon::name)
                        .toList());
        return List.of(
                new CharacterChoices.Choice(
                        "equipment.armor",
                        "Worn armor",
                        "Armor affects AC and casting eligibility.",
                        1,
                        ARMOR.stream().map(Armor::name).toList()),
                new CharacterChoices.Choice(
                        "equipment.weapon",
                        "Main weapon",
                        "Weapon properties determine eligible feat benefits.",
                        1,
                        WEAPONS.stream().map(Weapon::name).toList()),
                new CharacterChoices.Choice(
                        "equipment.offhand",
                        "Off-hand weapon",
                        "Choose None when this hand holds a shield or is free.",
                        1,
                        offhand),
                new CharacterChoices.Choice(
                        "equipment.shield",
                        "Shield",
                        "Holding a shield grants +2 AC; shield training is required for casting.",
                        1,
                        List.of("No shield", "Shield")));
    }
}
