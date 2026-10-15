package dndcompanion.domain.character;

import java.util.List;
import java.util.Map;

/** Pure pool definitions and recovery calculations, independent of Swing and persistence. */
public final class ClassResourceRules {
    private ClassResourceRules() {}

    public static List<ResourcePool> fighter(int level, Map<String, Integer> spent) {
        return List.of(
                pool(
                        "fighter.secondWind",
                        "Second Wind",
                        FighterRules.secondWind(level),
                        spent,
                        ResourcePool.Recovery.ONE_PER_SHORT_REST),
                pool(
                        "fighter.actionSurge",
                        "Action Surge",
                        FighterRules.actionSurge(level),
                        spent,
                        ResourcePool.Recovery.SHORT_REST),
                pool(
                        "fighter.indomitable",
                        "Indomitable",
                        FighterRules.indomitable(level),
                        spent,
                        ResourcePool.Recovery.LONG_REST));
    }

    private static ResourcePool pool(
            String id,
            String label,
            int maximum,
            Map<String, Integer> spent,
            ResourcePool.Recovery recovery) {
        return new ResourcePool(
                id, label, maximum, spent.getOrDefault("resource:" + id, 0), recovery);
    }

    public static int afterShortRest(ResourcePool pool) {
        return switch (pool.recovery()) {
            case LONG_REST -> pool.spent();
            case SHORT_REST -> 0;
            case ONE_PER_SHORT_REST -> Math.max(0, pool.spent() - 1);
        };
    }
}
