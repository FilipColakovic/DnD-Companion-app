package org.example.ui.spells;

import org.example.domain.character.Species;
import org.example.domain.character.SorcererRules;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;
import org.example.ui.character.SorcererPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public final class SpeciesChecks {
    public static void main(String[] args) throws Exception {
        for (Species species : Species.values()) {
            for (int level = 1; level <= 20; level++) {
                SpellSelection selection = new SpellSelection();
                selection.setCharacter(level, SorcererRules.Subclass.WILD_MAGIC, species);
                long expected = species.grants().stream().filter(grant -> grant.characterLevel() <= selection.level()).count();
                check(selection.automaticCount() == expected, "Species grant unlocks for " + species + " level " + level);
                check(selection.limit(true) == SorcererRules.calculate(level, 17, SorcererRules.Subclass.WILD_MAGIC).cantrips(),
                        "Species spells do not consume class cantrip capacity");
                for (Species.Grant grant : species.grants()) {
                    var spell = SpellCatalog.all().stream().filter(entry -> entry.name().equals(grant.name())).findFirst().orElseThrow();
                    check(!SpellDetails.forSpell(spell).description().isBlank(), "Full species spell information");
                    if (level >= grant.characterLevel()) {
                        check(selection.selected(spell) && selection.available(spell), "Species grants are automatically selected");
                        check(!selection.choose(spell, false).isEmpty(), "Cannot deselect species grant");
                    }
                }
            }
        }
        SpellSelection gnome = new SpellSelection();
        gnome.setCharacter(9, SorcererRules.Subclass.ABERRANT, Species.FOREST_GNOME);
        check(Species.FOREST_GNOME.castingNote("Speak with Animals", 9).contains("4 /"), "Forest Gnome uses proficiency for free casts");
        SwingUtilities.invokeAndWait(() -> {
            SorcererPanel main = new SorcererPanel();
            SpellBookPanel all = main.createSpellBook();
            SpellBookPanel known = all.createKnownSpells();
            main.selectSpecies(Species.WOOD_ELF);
            JTable table = find(known, JTable.class);
            check(table.getRowCount() == 1 && table.getModel().getValueAt(0, 2).equals("Druidcraft"),
                    "Main species selector updates the open known list");
            find(main, JSlider.class).setValue(5);
            check(table.getRowCount() == 10, "Level 5 combines seven subclass and three Wood Elf grants");
            main.selectSpecies(Species.INFERNAL_TIEFLING);
            check(table.getRowCount() == 11, "Species change removes old grants and adds new ones");
            main.selectSpecies(Species.HUMAN);
            check(table.getRowCount() == 7 && main.snapshot().chosenSpells().isEmpty(), "Species change never writes grants into manual choices");
            main.selectSpecies(Species.WOOD_ELF);
            main.setSize(940, Math.max(740, main.getPreferredSize().height));
            layout(main);
            BufferedImage image = new BufferedImage(main.getWidth(), main.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = image.createGraphics();
            main.printAll(graphics);
            graphics.dispose();
            try { ImageIO.write(image, "png", new File("target/species-preview.png")); }
            catch (Exception error) { throw new RuntimeException(error); }
            all.dispose();
            known.dispose();
        });
        System.out.println("Passed: species and lineages at all levels, free-cast counts, complete details, selection limits, and live UI updates.");
    }
    private static void check(boolean condition, String message) {
        if (!condition) { throw new AssertionError(message); }
    }
    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) { return type.cast(child); }
            if (child instanceof Container nested) {
                T found = find(nested, type);
                if (found != null) { return found; }
            }
        }
        return null;
    }
    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) { if (child instanceof Container nested) { layout(nested); } }
    }
}
