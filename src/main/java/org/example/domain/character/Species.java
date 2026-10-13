package org.example.domain.character;

import java.util.Arrays;
import java.util.List;

/** PHB 2024 species; spell-bearing lineages are selectable separately. */
public enum Species {
    AASIMAR("Aasimar", "Light:0:1"),
    DRAGONBORN("Dragonborn"),
    DWARF("Dwarf"),
    DROW("Elf · Drow", "Dancing Lights:0:1", "Faerie Fire:1:3", "Darkness:2:5"),
    HIGH_ELF("Elf · High", "Prestidigitation:0:1", "Detect Magic:1:3", "Misty Step:2:5"),
    WOOD_ELF("Elf · Wood", "Druidcraft:0:1", "Longstrider:1:3", "Pass without Trace:2:5"),
    FOREST_GNOME("Gnome · Forest", "Minor Illusion:0:1", "Speak with Animals:1:1"),
    ROCK_GNOME("Gnome · Rock", "Mending:0:1", "Prestidigitation:0:1"),
    GOLIATH("Goliath"),
    HALFLING("Halfling"),
    HUMAN("Human"),
    ORC("Orc"),
    ABYSSAL_TIEFLING(
            "Tiefling · Abyssal",
            "Thaumaturgy:0:1",
            "Poison Spray:0:1",
            "Ray of Sickness:1:3",
            "Hold Person:2:5"),
    CHTHONIC_TIEFLING(
            "Tiefling · Chthonic",
            "Thaumaturgy:0:1",
            "Chill Touch:0:1",
            "False Life:1:3",
            "Ray of Enfeeblement:2:5"),
    INFERNAL_TIEFLING(
            "Tiefling · Infernal",
            "Thaumaturgy:0:1",
            "Fire Bolt:0:1",
            "Hellish Rebuke:1:3",
            "Darkness:2:5");

    public record Grant(String name, int spellLevel, int characterLevel) {}

    private final String label;
    private final List<Grant> grants;

    Species(String label, String... spells) {
        this.label = label;
        grants =
                Arrays.stream(spells)
                        .map(
                                value -> {
                                    String[] fields = value.split(":");
                                    return new Grant(
                                            fields[0],
                                            Integer.parseInt(fields[1]),
                                            Integer.parseInt(fields[2]));
                                })
                        .toList();
    }

    @Override
    public String toString() {
        return label;
    }

    public List<Grant> grants() {
        return grants;
    }

    public boolean grants(String name, int level) {
        return grants.stream()
                .anyMatch(grant -> grant.name().equals(name) && level >= grant.characterLevel());
    }

    public String castingNote(String name, int level) {
        if (!grants(name, level)) {
            return "";
        }
        if (grants.stream()
                .anyMatch(grant -> grant.name().equals(name) && grant.spellLevel() == 0)) {
            return "Species cantrip";
        }
        if (this == FOREST_GNOME) {
            return "Species · " + (2 + (level - 1) / 4) + " / Long Rest";
        }
        return "Species · 1 / Long Rest";
    }
}
