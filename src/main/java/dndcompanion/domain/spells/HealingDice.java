package dndcompanion.domain.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Reviewed PHB 2024 healing spell dice; Healer rerolls each initial 1 once. */
public final class HealingDice {
    private HealingDice() {}

    public record Formula(int count, int sides, boolean abilityModifier) {}

    public record Roll(List<Integer> dice, int total, String notes) {}

    public static Optional<Formula> profile(
            SpellCatalog.Spell spell, int slot, int characterLevel) {
        if (spell == null) return Optional.empty();
        return switch (spell.name()) {
            case "Cure Wounds" -> Optional.of(new Formula(2 * Math.max(1, slot), 8, true));
            case "Healing Word" -> Optional.of(new Formula(2 * Math.max(1, slot), 4, true));
            default -> Optional.empty();
        };
    }

    public static Roll roll(Formula formula, int modifier, boolean healer, RandomGenerator random) {
        List<Integer> dice = new ArrayList<>();
        int rerolls = 0;
        for (int index = 0; index < formula.count(); index++) {
            int value = random.nextInt(1, formula.sides() + 1);
            if (healer && value == 1) {
                value = random.nextInt(1, formula.sides() + 1);
                rerolls++;
            }
            dice.add(value);
        }
        return new Roll(
                List.copyOf(dice),
                Math.max(
                        0,
                        dice.stream().mapToInt(Integer::intValue).sum()
                                + (formula.abilityModifier() ? modifier : 0)),
                rerolls == 0 ? "" : "Healer: rerolled " + rerolls + " initial 1s once");
    }
}
