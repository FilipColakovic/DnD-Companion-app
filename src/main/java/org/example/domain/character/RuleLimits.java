package org.example.domain.character;

/** Shared character and resource bounds, independent of UI controls. */
public final class RuleLimits {
    public static final int MIN_LEVEL = 1, MAX_LEVEL = 20;
    public static final int MIN_ABILITY_SCORE = 1, MAX_ABILITY_SCORE = 30;
    public static final int MAX_SPELL_LEVEL = 9;

    private RuleLimits() {}
}
