package org.example.domain.character;

import org.example.domain.spells.SpellCatalog;

import java.util.Set;

/**
 * Read-only cast validation and costs. A session applies spending only after validation succeeds.
 */
public final class CastingRules {
    private CastingRules() {}

    public record Context(
            int level,
            boolean innate,
            boolean selected,
            boolean available,
            Set<Metamagic> known,
            int freeCasts,
            int remainingSlots,
            int remainingPoints) {
        public Context {
            known = Set.copyOf(known);
        }
    }

    public static int metamagicCost(int level, boolean innate, Set<Metamagic> options) {
        int cost = options.stream().mapToInt(Metamagic::cost).sum();
        if (level == RuleLimits.MAX_LEVEL && innate)
            cost -= options.stream().mapToInt(Metamagic::cost).max().orElse(0);
        return cost;
    }

    public static String validate(
            SpellCatalog.Spell spell,
            int slot,
            boolean speciesFree,
            Set<Metamagic> options,
            Context context) {
        if (!context.selected() || !context.available())
            return "Choose an available prepared spell or known cantrip.";
        if (!options.isEmpty()
                && (context.level() < 2
                        || context.known().size() > Metamagic.limit(context.level())))
            return "Metamagic choices exceed your current level's limit.";
        for (Metamagic option : options) {
            if (!context.known().contains(option) || !option.applies(spell))
                return "Metamagic is not selected or is incompatible with this spell.";
        }
        long ordinary = options.stream().filter(option -> !option.combinable()).count();
        if (ordinary > (context.level() >= 7 && context.innate() ? 2 : 1))
            return "These Metamagic options cannot be combined right now.";
        if (spell.level() == 0 && (slot != 0 || speciesFree))
            return "Cantrips do not spend a slot or a species free cast.";
        if (spell.level() > 0) {
            if (speciesFree) {
                if (context.freeCasts() == 0 || slot != spell.level())
                    return "No species free casts remain, or slot level differs from the spell's"
                            + " base level.";
            } else if (slot < spell.level()
                    || slot > RuleLimits.MAX_SPELL_LEVEL
                    || context.remainingSlots() == 0) return "No appropriate spell slot remaining.";
        }
        if (options.contains(Metamagic.TWINNED) && slot >= RuleLimits.MAX_SPELL_LEVEL)
            return "Twinned Spell cannot raise the effective level above 9.";
        if (context.remainingPoints() < metamagicCost(context.level(), context.innate(), options))
            return "Not enough Sorcery Points.";
        return "";
    }
}
