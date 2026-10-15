package dndcompanion.domain.character;

public enum ClassId {
    BARBARIAN,
    BARD,
    CLERIC,
    DRUID,
    FIGHTER,
    MONK,
    PALADIN,
    RANGER,
    ROGUE,
    SORCERER,
    WARLOCK,
    WIZARD;

    public boolean supported() {
        return this == SORCERER || this == FIGHTER;
    }

    public String label() {
        return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
    }
}
