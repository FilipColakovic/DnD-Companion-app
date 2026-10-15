package dndcompanion.domain.character;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/** Immutable resources, choices, and display settings stored with the character checkpoint. */
public record PlayState(
        List<Integer> spentSlots,
        int spentPoints,
        boolean restorationUsed,
        Set<Metamagic> metamagic,
        String concentration,
        Map<String, Integer> speciesUses,
        int innateUses,
        boolean innateActive,
        boolean dark,
        int textSize,
        int hitPoints,
        int maxHitPoints,
        Map<String, String> feats,
        CharacterSheet sheet) {
    public PlayState withSheet(CharacterSheet value) {
        return new PlayState(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                dark,
                textSize,
                hitPoints,
                maxHitPoints,
                feats,
                value);
    }

    public PlayState withAppearance(boolean value, int size) {
        return new PlayState(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                value,
                size,
                hitPoints,
                maxHitPoints,
                feats,
                sheet);
    }

    /** Compatibility constructor: former turn flags are ignored when loading older callers. */
    public PlayState(
            List<Integer> spentSlots,
            int spentPoints,
            boolean restorationUsed,
            Set<Metamagic> metamagic,
            String concentration,
            Map<String, Integer> speciesUses,
            int innateUses,
            boolean innateActive,
            boolean slotCast,
            boolean leveledCast,
            boolean quickened,
            boolean freeMetamagicUsed,
            boolean dark,
            int textSize,
            int hitPoints,
            int maxHitPoints,
            Map<String, String> feats,
            CharacterSheet sheet) {
        this(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                dark,
                textSize,
                hitPoints,
                maxHitPoints,
                feats,
                sheet);
    }

    public PlayState(
            List<Integer> spentSlots,
            int spentPoints,
            boolean restorationUsed,
            Set<Metamagic> metamagic,
            String concentration,
            Map<String, Integer> speciesUses,
            int innateUses,
            boolean innateActive,
            boolean slotCast,
            boolean leveledCast,
            boolean quickened,
            boolean freeMetamagicUsed,
            boolean dark,
            int textSize,
            int hitPoints,
            int maxHitPoints,
            Map<String, String> feats) {
        this(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                slotCast,
                leveledCast,
                quickened,
                freeMetamagicUsed,
                dark,
                textSize,
                hitPoints,
                maxHitPoints,
                feats,
                CharacterSheet.defaults());
    }

    public PlayState(
            List<Integer> spentSlots,
            int spentPoints,
            boolean restorationUsed,
            Set<Metamagic> metamagic,
            String concentration,
            Map<String, Integer> speciesUses,
            int innateUses,
            boolean innateActive,
            boolean slotCast,
            boolean leveledCast,
            boolean quickened,
            boolean freeMetamagicUsed,
            boolean dark,
            int textSize,
            int hitPoints,
            int maxHitPoints) {
        this(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                slotCast,
                leveledCast,
                quickened,
                freeMetamagicUsed,
                dark,
                textSize,
                hitPoints,
                maxHitPoints,
                Map.of());
    }

    public PlayState(
            List<Integer> spentSlots,
            int spentPoints,
            boolean restorationUsed,
            Set<Metamagic> metamagic,
            String concentration,
            Map<String, Integer> speciesUses,
            int innateUses,
            boolean innateActive,
            boolean slotCast,
            boolean leveledCast,
            boolean quickened,
            boolean freeMetamagicUsed,
            boolean dark,
            int textSize) {
        this(
                spentSlots,
                spentPoints,
                restorationUsed,
                metamagic,
                concentration,
                speciesUses,
                innateUses,
                innateActive,
                slotCast,
                leveledCast,
                quickened,
                freeMetamagicUsed,
                dark,
                textSize,
                0,
                0);
    }

    public PlayState {
        spentSlots = List.copyOf(spentSlots);
        metamagic = Set.copyOf(metamagic);
        speciesUses = Map.copyOf(speciesUses);
        feats = Map.copyOf(feats);
        feats.forEach(
                (name, notes) -> {
                    FeatCatalog.named(name);
                    if (notes.length() > 2000)
                        throw new IllegalArgumentException("Feat notes exceed 2000 characters");
                });
        Objects.requireNonNull(concentration);
        Objects.requireNonNull(sheet);
        if (spentSlots.size() != 9
                || spentSlots.stream().anyMatch(n -> n < 0)
                || spentPoints < 0
                || innateUses < 0
                || speciesUses.values().stream().anyMatch(n -> n < 0)
                || textSize < 12
                || textSize > 18
                || hitPoints < 0
                || maxHitPoints < 0
                || hitPoints > maxHitPoints) {
            throw new IllegalArgumentException("Invalid play state");
        }
    }

    public static PlayState defaults() {
        return new PlayState(
                Collections.nCopies(9, 0),
                0,
                false,
                Set.of(),
                "",
                Map.of(),
                0,
                false,
                false,
                14,
                0,
                0,
                Map.of(),
                CharacterSheet.defaults());
    }

    public String encode() {
        Properties p = new Properties();
        p.setProperty(
                "slots",
                spentSlots.stream().map(Object::toString).collect(Collectors.joining(",")));
        p.setProperty("points", "" + spentPoints);
        p.setProperty("restoration", "" + restorationUsed);
        p.setProperty(
                "metamagic",
                metamagic.stream().map(Enum::name).sorted().collect(Collectors.joining(",")));
        p.setProperty("concentration", concentration);
        p.setProperty("innateUses", "" + innateUses);
        p.setProperty("innate", "" + innateActive);
        p.setProperty("dark", "" + dark);
        p.setProperty("textSize", "" + textSize);
        p.setProperty("hp", "" + hitPoints);
        p.setProperty("maxHp", "" + maxHitPoints);
        speciesUses.forEach((name, n) -> p.setProperty("species." + name, "" + n));
        feats.forEach((name, notes) -> p.setProperty("feat." + name, notes));
        sheet.values().forEach((key, value) -> p.setProperty("sheet." + key, value));
        try {
            StringWriter writer = new StringWriter();
            p.store(writer, null);
            return writer.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static PlayState decode(String data) {
        if (data == null || data.isBlank()) {
            return defaults();
        }
        Properties p = new Properties();
        try {
            p.load(new StringReader(data));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Set<Metamagic> meta = new HashSet<>();
        for (String value : p.getProperty("metamagic", "").split(",")) {
            if (!value.isBlank()) {
                meta.add(Metamagic.valueOf(value));
            }
        }
        Map<String, Integer> uses = new HashMap<>();
        Map<String, String> feats = new HashMap<>();
        Map<String, String> sheet = new HashMap<>();
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("sheet."))
                .forEach(k -> sheet.put(k.substring(6), p.getProperty(k)));
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("feat."))
                .forEach(k -> feats.put(k.substring(5), p.getProperty(k)));
        p.stringPropertyNames().stream()
                .filter(k -> k.startsWith("species."))
                .forEach(k -> uses.put(k.substring(8), Integer.parseInt(p.getProperty(k))));
        return new PlayState(
                Arrays.stream(p.getProperty("slots", "0,0,0,0,0,0,0,0,0").split(","))
                        .map(Integer::parseInt)
                        .toList(),
                Integer.parseInt(p.getProperty("points", "0")),
                bool(p, "restoration"),
                meta,
                p.getProperty("concentration", ""),
                uses,
                Integer.parseInt(p.getProperty("innateUses", "0")),
                bool(p, "innate"),
                bool(p, "dark"),
                Integer.parseInt(p.getProperty("textSize", "14")),
                Integer.parseInt(p.getProperty("hp", "0")),
                Integer.parseInt(p.getProperty("maxHp", "0")),
                feats,
                new CharacterSheet(sheet));
    }

    private static boolean bool(Properties p, String key) {
        return Boolean.parseBoolean(p.getProperty(key, "false"));
    }
}
