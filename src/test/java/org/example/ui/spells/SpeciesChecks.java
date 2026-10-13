package org.example.ui.spells;

import org.example.domain.character.CharacterState;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;

import javax.swing.SwingUtilities;

public final class SpeciesChecks {
    public static void main(String[] args) throws Exception {
        for (Species species : Species.values()) {
            for (int level = 1; level <= 20; level++) {
                SpellSelection selection = new SpellSelection();
                selection.setCharacter(level, SorcererRules.Subclass.WILD_MAGIC, species);
                long expected =
                        species.grants().stream()
                                .filter(grant -> grant.characterLevel() <= selection.level())
                                .count();
                check(
                        selection.automaticCount() == expected,
                        "Species grant unlocks for " + species + " level " + level);
                check(
                        selection.limit(true)
                                == SorcererRules.calculate(
                                                level, 17, SorcererRules.Subclass.WILD_MAGIC)
                                        .cantrips(),
                        "Species spells do not consume class cantrip capacity");
                for (Species.Grant grant : species.grants()) {
                    var spell =
                            SpellCatalog.all().stream()
                                    .filter(entry -> entry.name().equals(grant.name()))
                                    .findFirst()
                                    .orElseThrow();
                    check(
                            !SpellDetails.forSpell(spell).description().isBlank(),
                            "Full species spell information");
                    if (level >= grant.characterLevel()) {
                        check(
                                selection.selected(spell) && selection.available(spell),
                                "Species grants are automatically selected");
                        check(
                                !selection.choose(spell, false).isEmpty(),
                                "Cannot deselect species grant");
                    }
                }
            }
        }
        SpellSelection gnome = new SpellSelection();
        gnome.setCharacter(9, SorcererRules.Subclass.ABERRANT, Species.FOREST_GNOME);
        check(
                Species.FOREST_GNOME.castingNote("Speak with Animals", 9).contains("4 /"),
                "Forest Gnome uses proficiency for free casts");

        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel workspace = new GrimoirePanel(CharacterState.defaults());
                    try {
                        workspace.showPreparedSpells();
                        var library = workspace.libraryPanel();
                        workspace.characterController().selectSpecies(Species.WOOD_ELF);
                        check(
                                library.visibleSpellCount() == 1,
                                "Species changes update the current prepared library");
                        workspace.characterController().setLevel(5);
                        check(
                                library.visibleSpellCount() == 10,
                                "Level 5 combines subclass and Wood Elf grants");
                        workspace.characterController().selectSpecies(Species.INFERNAL_TIEFLING);
                        check(
                                library.visibleSpellCount() == 11,
                                "Species change replaces derived grants");
                        workspace.characterController().selectSpecies(Species.HUMAN);
                        check(
                                library.visibleSpellCount() == 7
                                        && workspace.snapshot().chosenSpells().isEmpty(),
                                "Grants never become manual choices");
                    } finally {
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: PHB species grants at all levels, free casts, complete details and live"
                    + " library updates.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static <T extends Component> T accessible(Container root, Class<T> type, String name) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)
                    && child.getAccessibleContext() != null
                    && name.equals(child.getAccessibleContext().getAccessibleName()))
                return type.cast(child);
            if (child instanceof Container nested) {
                T result = accessible(nested, type, name);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
