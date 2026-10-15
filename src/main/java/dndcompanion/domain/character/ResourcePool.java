package dndcompanion.domain.character;

public record ResourcePool(String id, String label, int maximum, int spent, Recovery recovery) {
    public enum Recovery {
        LONG_REST,
        SHORT_REST,
        ONE_PER_SHORT_REST
    }

    public int remaining() {
        return Math.max(0, maximum - spent);
    }
}
