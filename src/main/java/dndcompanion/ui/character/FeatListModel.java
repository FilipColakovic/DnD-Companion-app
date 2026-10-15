package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;

import java.util.Comparator;
import java.util.List;

/** Central ordering and prerequisite presentation; no changes to recorded feat rules. */
final class FeatListModel {
    enum Group {
        EQUIPPED("EQUIPPED FEATS"),
        AVAILABLE("AVAILABLE FEATS"),
        LOCKED("LOCKED FEATS");
        final String title;

        Group(String title) {
            this.title = title;
        }
    }

    static Group group(FeatCatalog.Feat feat, CharacterState state) {
        if (state.equippedFeats().containsKey(feat.name())) return Group.EQUIPPED;
        return feat.requirements(state.level(), state.charisma(), state.classId()).isEmpty()
                ? Group.AVAILABLE
                : Group.LOCKED;
    }

    static String status(FeatCatalog.Feat feat, CharacterState state) {
        String requirement = feat.requirements(state.level(), state.charisma(), state.classId());
        boolean recorded = state.equippedFeats().containsKey(feat.name());
        if (!requirement.isEmpty()) return (recorded ? "Recorded · " : "Locked · ") + requirement;
        if (recorded) return "Equipped";
        return feat.needsConfirmation(state.charisma())
                ? "Level available · confirm other prerequisites"
                : "Available";
    }

    static List<FeatCatalog.Feat> ordered(
            List<FeatCatalog.Feat> feats, CharacterState state, boolean categories) {
        return feats.stream()
                .sorted(
                        Comparator.comparing((FeatCatalog.Feat feat) -> group(feat, state))
                                .thenComparingInt(
                                        feat -> categories ? feat.category().ordinal() : 0)
                                .thenComparing(FeatCatalog.Feat::name))
                .toList();
    }
}
