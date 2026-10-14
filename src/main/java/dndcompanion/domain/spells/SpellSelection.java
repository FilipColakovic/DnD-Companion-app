package dndcompanion.domain.spells;

import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/** Manual selections survive character changes; automatic subclass spells are always derived. */
public final class SpellSelection {
    private final Set<String> choices = new HashSet<>();
    private Set<String> featSpells = Set.of();

    /**
     * Synchronize derived grants before the owning session/selection operation announces its
     * change. Grants are never persisted as manual selections; this avoids duplicate notifications.
     */
    public void setFeatSpells(Set<String> names) {
        Set<String> next = Set.copyOf(names);
        if (featSpells.equals(next)) return;
        featSpells = next;
    }

    public boolean featSpell(SpellCatalog.Spell spell) {
        return featSpells.contains(spell.name());
    }

    private int level = 1;
    private Species species = Species.HUMAN;
    private SorcererRules.Subclass subclass = SorcererRules.Subclass.ABERRANT;
    private final CopyOnWriteArrayList<Runnable> listeners = new CopyOnWriteArrayList<>();

    public void addChangeListener(Runnable listener) {
        listeners.add(listener);
    }

    public void removeChangeListener(Runnable listener) {
        listeners.remove(listener);
    }

    private void changed() {
        listeners.forEach(Runnable::run);
    }

    public void setCharacter(int level, SorcererRules.Subclass subclass) {
        setCharacter(level, subclass, species);
    }

    public void setCharacter(int level, SorcererRules.Subclass subclass, Species species) {
        SorcererRules.calculate(level, 17, subclass); // Validate before updating state.
        java.util.Objects.requireNonNull(species);
        if (this.level == level && this.subclass == subclass && this.species == species) {
            return;
        }
        this.level = level;
        this.subclass = subclass;
        this.species = species;
        changed();
    }

    public int level() {
        return level;
    }

    public Set<String> chosenSpells() {
        return Set.copyOf(choices);
    }

    /**
     * Restore all manual choices, including dormant or over-limit choices after a level decrease.
     */
    public void restoreChoices(Set<String> names) {
        Set<String> restored = Set.copyOf(names);
        for (String name : restored) {
            if (SpellCatalog.all().stream().noneMatch(spell -> spell.name().equals(name))) {
                throw new IllegalArgumentException("Unknown spell: " + name);
            }
        }
        choices.clear();
        choices.addAll(restored);
        changed();
    }

    public SorcererRules.Subclass subclass() {
        return subclass;
    }

    public Species species() {
        return species;
    }

    public boolean speciesSpell(SpellCatalog.Spell spell) {
        return species.grants(spell.name(), level);
    }

    public java.util.List<SpellCatalog.Spell> catalog() {
        return SpellCatalog.all().stream()
                .filter(
                        spell ->
                                spell.sorcererSpell()
                                        || featSpell(spell)
                                        || SpellCatalog.unlockLevel(spell, subclass) > 0
                                        || species.grants().stream()
                                                .anyMatch(
                                                        grant -> grant.name().equals(spell.name())))
                .toList();
    }

    public int maxSpellLevel() {
        return Math.min(9, (level + 1) / 2);
    }

    public boolean automatic(SpellCatalog.Spell spell) {
        return SpellCatalog.alwaysPrepared(spell, level, subclass)
                || speciesSpell(spell)
                || featSpell(spell);
    }

    public boolean available(SpellCatalog.Spell spell) {
        return automatic(spell) || spell.sorcererSpell() && spell.level() <= maxSpellLevel();
    }

    public boolean selected(SpellCatalog.Spell spell) {
        return automatic(spell) || choices.contains(spell.name());
    }

    public int limit(boolean cantrip) {
        var stats = SorcererRules.calculate(level, 17, subclass);
        return cantrip
                ? stats.cantrips()
                        - (level >= 3 && subclass == SorcererRules.Subclass.ABERRANT ? 1 : 0)
                : stats.basePreparedSpells();
    }

    public int chosenCount(boolean cantrip) {
        return (int)
                SpellCatalog.all().stream()
                        .filter(spell -> (spell.level() == 0) == cantrip)
                        .filter(
                                spell ->
                                        choices.contains(spell.name())
                                                && available(spell)
                                                && !automatic(spell))
                        .count();
    }

    public int automaticCount() {
        return (int) SpellCatalog.all().stream().filter(this::automatic).count();
    }

    public int speciesCount(boolean cantrip) {
        return (int)
                SpellCatalog.all().stream()
                        .filter(this::speciesSpell)
                        .filter(spell -> (spell.level() == 0) == cantrip)
                        .filter(spell -> !SpellCatalog.alwaysPrepared(spell, level, subclass))
                        .count();
    }

    /**
     * Return a message if selection is refused. Deselecting is always allowed for manual choices.
     */
    public String choose(SpellCatalog.Spell spell, boolean selected) {
        if (automatic(spell)) {
            return "This spell is granted by your subclass, species, or feat.";
        }
        if (!selected) {
            choices.remove(spell.name());
            changed();
            return "";
        }
        if (!available(spell)) {
            return "This spell isn't available at your current Sorcerer level.";
        }
        if (choices.contains(spell.name())) {
            return "";
        }
        boolean cantrip = spell.level() == 0;
        if (chosenCount(cantrip) >= limit(cantrip)) {
            return "Remove a "
                    + (cantrip ? "cantrip" : "prepared spell")
                    + " before choosing another.";
        }
        choices.add(spell.name());
        changed();
        return "";
    }

    public String summary() {
        String counts =
                "Cantrips: "
                        + chosenCount(true)
                        + " / "
                        + limit(true)
                        + "    Prepared spells: "
                        + chosenCount(false)
                        + " / "
                        + limit(false)
                        + "    Automatic spells: "
                        + automaticCount();
        if (chosenCount(true) > limit(true) || chosenCount(false) > limit(false)) {
            counts += "    Over limit — remove choices.";
        }
        return counts;
    }
}
