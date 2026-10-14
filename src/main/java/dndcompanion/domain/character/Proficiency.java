package dndcompanion.domain.character;

/** Named skill tiers with unchanged saved values. */
public enum Proficiency {
    UNTRAINED(0, "Untrained"),
    PROFICIENT(1, "Proficient"),
    EXPERTISE(2, "Expertise");
    private final int storedValue;
    private final String label;

    Proficiency(int storedValue, String label) {
        this.storedValue = storedValue;
        this.label = label;
    }

    public int storedValue() {
        return storedValue;
    }

    public String label() {
        return label;
    }

    public Proficiency next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public static Proficiency fromStoredValue(int value) {
        for (Proficiency tier : values()) if (tier.storedValue == value) return tier;
        throw new IllegalArgumentException("Proficiency tier must be 0–2");
    }
}
