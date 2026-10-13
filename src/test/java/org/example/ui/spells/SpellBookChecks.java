package org.example.ui.spells;

import org.example.domain.character.CharacterState;
import org.example.domain.character.SorcererRules;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;
import org.example.ui.GrimoirePanel;

import java.awt.Component;
import java.awt.Container;

import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public final class SpellBookChecks {
    public static void main(String[] args) throws Exception {
        int[] counts = {20, 22, 30, 21, 13, 13, 12, 8, 6, 5};
        for (int level = 0; level <= 9; level++) {
            int spellLevel = level;
            check(
                    SpellCatalog.all().stream()
                                    .filter(
                                            spell ->
                                                    spell.sorcererSpell()
                                                            && spell.level() == spellLevel)
                                    .count()
                            == counts[level],
                    "Complete PHB class list at spell level " + level);
        }
        check(
                SpellCatalog.all().stream().map(SpellCatalog.Spell::name).distinct().count()
                        == SpellCatalog.all().size(),
                "No duplicate spell names");
        for (var spell : SpellCatalog.all()) {
            var details = SpellDetails.forSpell(spell);
            check(
                    !details.description().isBlank()
                            && !details.school().isBlank()
                            && !details.castingTime().isBlank()
                            && !details.range().isBlank()
                            && !details.components().isBlank()
                            && !details.duration().isBlank(),
                    "Full details for " + spell.name());
            check(
                    !details.shortDescription().isBlank()
                            && details.shortDescription().length() <= 135,
                    "Short summary for " + spell.name());
        }
        check(
                SpellDetails.forSpell(spell("Chill Touch")).range().equalsIgnoreCase("touch"),
                "2024 Chill Touch range");
        check(
                SpellDetails.forSpell(spell("Sleep")).concentration(),
                "2024 Sleep requires concentration");
        check(
                SpellDetails.forSpell(spell("Witch Bolt")).description().contains("Bonus Action"),
                "2024 Witch Bolt uses Bonus Action");
        check(
                SpellDetails.forSpell(spell("Summon Dragon"))
                        .description()
                        .contains("Draconic Spirit"),
                "Summon includes spirit stats");
        for (var subclass : SorcererRules.Subclass.values()) {
            for (int level = 1; level <= 20; level++) {
                SpellSelection selection = new SpellSelection();
                selection.setCharacter(level, subclass);
                var stats = SorcererRules.calculate(level, 17, subclass);
                check(
                        selection.automaticCount()
                                == stats.subclassPreparedSpells()
                                        + (level >= 3 && subclass == SorcererRules.Subclass.ABERRANT
                                                ? 1
                                                : 0),
                        "Subclass grants match calculator");
                check(
                        SpellCatalog.forSubclass(subclass).stream()
                                .filter(selection::automatic)
                                .allMatch(selection::selected),
                        "Subclass grants auto-selected");
            }
        }
        SpellSelection selection = new SpellSelection();
        check(
                !selection.choose(spell("Fireball"), true).isEmpty(),
                "Level 1 can't choose Fireball");
        check(selection.choose(spell("Shield"), true).isEmpty(), "Choose Shield");
        check(selection.choose(spell("Magic Missile"), true).isEmpty(), "Choose Magic Missile");
        check(!selection.choose(spell("Sleep"), true).isEmpty(), "Enforce prepared spell limit");
        check(selection.choose(spell("Shield"), false).isEmpty(), "Remove selection");
        check(selection.choose(spell("Sleep"), true).isEmpty(), "Freed slot can be used");
        selection.setCharacter(5, SorcererRules.Subclass.ABERRANT);
        check(selection.choose(spell("Fireball"), true).isEmpty(), "Level 5 chooses Fireball");
        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC);
        check(
                selection.selected(spell("Fireball")) && !selection.available(spell("Fireball")),
                "Level decrease preserves dormant choice");
        selection.setCharacter(5, SorcererRules.Subclass.ABERRANT);
        check(
                selection.selected(spell("Fireball")) && selection.available(spell("Fireball")),
                "Level increase restores choice");
        check(
                !selection.choose(spell("Mind Sliver"), false).isEmpty(),
                "Cannot remove subclass cantrip");
        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC);
        for (String name : new String[] {"Fire Bolt", "Light", "Mage Hand", "True Strike"}) {
            check(selection.choose(spell(name), true).isEmpty(), "Choose cantrip");
        }
        check(!selection.choose(spell("Friends"), true).isEmpty(), "Enforce cantrip limit");

        SwingUtilities.invokeAndWait(
                () -> {
                    GrimoirePanel workspace = new GrimoirePanel(CharacterState.defaults());
                    try {
                        var library = workspace.libraryPanel();
                        var editor = workspace.characterController();
                        var fireball = accessible(library, JButton.class, "Choose Fireball");
                        check(!fireball.isEnabled(), "Unavailable Fireball cannot be learned");
                        editor.setLevel(5);
                        check(
                                accessible(library, JButton.class, "Choose Fireball").isEnabled(),
                                "Live level change unlocks Fireball");
                        accessible(library, JButton.class, "Choose Fireball").doClick(0);
                        check(
                                workspace.playSession().selection().selected(spell("Fireball")),
                                "Library Learn updates manual choices");
                        var search = accessible(library, JTextField.class, "Search library spells");
                        search.setText("FIREBALL");
                        check(
                                library.visibleSpellCount() == 2,
                                "Case-insensitive search includes Delayed Blast Fireball");
                        search.setText("no-such-spell");
                        check(library.visibleSpellCount() == 0, "Empty search");
                        search.setText("");
                        workspace.showPreparedSpells();
                        check(
                                library.visibleSpellCount()
                                        == workspace.playSession().selection().automaticCount() + 1,
                                "Prepared filter includes learned spell");
                        library.inspect("Fireball");
                        editor.setLevel(1);
                        check(
                                editor.snapshot().chosenSpells().contains("Fireball"),
                                "Dormant learned choices survive level decrease");
                        check(
                                accessible(library, JButton.class, "Choose Fireball").isEnabled(),
                                "Dormant learned spells remain removable");
                        accessible(library, JButton.class, "Choose Fireball").doClick(0);
                        check(
                                !editor.snapshot().chosenSpells().contains("Fireball"),
                                "Unlearn removes dormant spell");
                    } finally {
                        workspace.dispose();
                    }
                });
        System.out.println(
                "Passed: full spell catalog, selection limits and live card-based spell learning"
                    + " and filtering.");
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
