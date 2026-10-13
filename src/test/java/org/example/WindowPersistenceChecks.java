package org.example;

import org.example.application.CharacterController;
import org.example.domain.character.CharacterState;
import org.example.domain.character.Metamagic;
import org.example.domain.character.SorcererRules;
import org.example.domain.character.Species;
import org.example.persistence.CharacterStore;
import org.example.ui.GrimoirePanel;
import org.example.ui.spells.CastingPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Exercise the production close handler and a new app window against the same real SQLite file. */
public final class WindowPersistenceChecks {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory(Path.of("target"), "window-save-checks-");
        Path database = directory.resolve("character.db");
        try {
            CharacterStore store = new CharacterStore(database);
            store.save(
                    new CharacterState(
                            5, 17, SorcererRules.Subclass.ABERRANT, Set.of("Fireball", "Shield")));
            SwingUtilities.invokeAndWait(
                    () -> {
                        JFrame window = null;
                        JFrame reopened = null;
                        try {
                            window = Main.createWindow(store);
                            CharacterController panel =
                                    ((GrimoirePanel) window.getContentPane()).characterPanel();
                            panel.setLevel(8);
                            panel.setCharismaScore(panel.snapshot().charisma() + 1);
                            panel.selectSubclass(SorcererRules.Subclass.CLOCKWORK);
                            panel.selectSpecies(Species.INFERNAL_TIEFLING);
                            panel.playSession()
                                    .selection()
                                    .choose(
                                            org.example.domain.spells.SpellCatalog.all().stream()
                                                    .filter(spell -> spell.name().equals("Shield"))
                                                    .findFirst()
                                                    .orElseThrow(),
                                            false);
                            panel.playSession().choose(Metamagic.QUICKENED, true);
                            panel.playSession()
                                    .setSheet(
                                            panel.playSession()
                                                    .sheet()
                                                    .withAbility(
                                                            org.example.domain.character
                                                                    .CharacterSheet.Ability.WIS,
                                                            12)
                                                    .withSkill(
                                                            org.example.domain.character
                                                                    .CharacterSheet.Skill
                                                                    .PERSUASION,
                                                            1));
                            panel.playSession().setRemainingSlots(3, 0);
                            panel.playSession().setRemainingPoints(1);
                            panel.playSession().shortRest();
                            CharacterState expected =
                                    new CharacterState(
                                            8,
                                            18,
                                            SorcererRules.Subclass.CLOCKWORK,
                                            Set.of("Fireball"),
                                            Species.INFERNAL_TIEFLING,
                                            panel.playSession().snapshot());
                            window.dispatchEvent(
                                    new WindowEvent(window, WindowEvent.WINDOW_CLOSING));
                            if (window.isDisplayable()) {
                                throw new AssertionError(
                                        "Successful save must close the app window");
                            }
                            if (!store.load().equals(expected)) {
                                throw new AssertionError(
                                        "Close handler did not save current values");
                            }
                            reopened = Main.createWindow(new CharacterStore(database));
                            if (!((GrimoirePanel) reopened.getContentPane())
                                    .snapshot()
                                    .equals(expected)) {
                                throw new AssertionError(
                                        "New window did not restore the last save");
                            }
                            CharacterController experimenting =
                                    ((GrimoirePanel) reopened.getContentPane()).characterPanel();
                            experimenting.setCharismaScore(experimenting.snapshot().charisma() + 1);
                            CharacterState checkpoint = experimenting.snapshot();
                            GrimoirePanel workspace = (GrimoirePanel) reopened.getContentPane();
                            JCheckBox lock =
                                    find(workspace, JCheckBox.class, "Lock saved character");
                            lock.doClick(0);
                            check(
                                    store.load().equals(checkpoint) && store.isSaveLocked(),
                                    "Checking save protection immediately commits a checkpoint and"
                                            + " its lock");
                            var linked = workspace.libraryPanel();
                            workspace.showPreparedSpells();
                            check(
                                    workspace.currentPage().equals(GrimoirePanel.LIBRARY),
                                    "Prepared-spell review opens the filtered library");
                            find(workspace, JButton.class, GrimoirePanel.METAMAGIC).doClick(0);
                            check(
                                    workspace.currentPage().equals(GrimoirePanel.METAMAGIC),
                                    "Metamagic navigation opens the shared page");
                            find(workspace, JButton.class, GrimoirePanel.COMBAT).doClick(0);

                            experimenting.playSession().longRest();
                            CastingPanel casting = workspace.combatPanel();
                            check(
                                    casting.selectSpell("Fireball"),
                                    "Casting page selects a prepared spell through its public API");
                            find(casting, JButton.class, "Cast").doClick(0);
                            check(
                                    experimenting.playSession().remainingPoints() == 8
                                            && experimenting.playSession().remainingSlots(3) == 2,
                                    "Casting window spends resources in main-window model");
                            experimenting.playSession().appearance(true, 16);
                            experimenting.setLevel(20);
                            experimenting.setCharismaScore(experimenting.snapshot().charisma() - 1);
                            experimenting.selectSubclass(SorcererRules.Subclass.WILD_MAGIC);
                            experimenting.selectSpecies(Species.WOOD_ELF);
                            experimenting
                                    .playSession()
                                    .selection()
                                    .choose(
                                            org.example.domain.spells.SpellCatalog.all().stream()
                                                    .filter(
                                                            spell ->
                                                                    spell.name().equals("Fireball"))
                                                    .findFirst()
                                                    .orElseThrow(),
                                            false);
                            check(
                                    !experimenting.snapshot().equals(checkpoint)
                                            && store.load().equals(checkpoint),
                                    "Experimental inputs and spell choices do not change the"
                                            + " checkpoint");
                            find(workspace, JButton.class, "Restore saved").doClick(0);
                            check(
                                    experimenting.snapshot().equals(checkpoint)
                                            && experimenting.isSaveLocked(),
                                    "Restore saved returns all inputs and spell choices and keeps"
                                            + " protection enabled");
                            workspace.showPreparedSpells();
                            check(
                                    linked.visibleSpellCount()
                                            == experimenting
                                                            .playSession()
                                                            .selection()
                                                            .automaticCount()
                                                    + checkpoint.chosenSpells().size(),
                                    "Restore updates the existing prepared library");
                            experimenting.setLevel(1);
                            reopened.dispatchEvent(
                                    new WindowEvent(reopened, WindowEvent.WINDOW_CLOSING));
                            check(
                                    !reopened.isDisplayable() && store.load().equals(checkpoint),
                                    "Closing a protected session discards experiments without"
                                            + " overwriting the save");
                            reopened = Main.createWindow(new CharacterStore(database));
                            experimenting =
                                    ((GrimoirePanel) reopened.getContentPane()).characterPanel();
                            check(
                                    experimenting.isSaveLocked()
                                            && experimenting.snapshot().equals(checkpoint),
                                    "Save protection and checkpoint survive reopening");
                            experimenting.setLevel(10);
                            experimenting.selectSpecies(Species.FOREST_GNOME);
                            CharacterState unlockedChanges = experimenting.snapshot();
                            find(
                                            (GrimoirePanel) reopened.getContentPane(),
                                            JCheckBox.class,
                                            "Lock saved character")
                                    .doClick(0);
                            check(
                                    !store.isSaveLocked() && store.load().equals(checkpoint),
                                    "Unchecking enables normal saving without immediately"
                                            + " overwriting the checkpoint");
                            reopened.dispatchEvent(
                                    new WindowEvent(reopened, WindowEvent.WINDOW_CLOSING));
                            check(
                                    store.load().equals(unlockedChanges),
                                    "Unlocked session saves new inputs on exit");
                        } catch (Exception error) {
                            throw new RuntimeException(error);
                        } finally {
                            if (window != null) {
                                window.dispose();
                            }
                            if (reopened != null) {
                                reopened.dispose();
                            }
                        }
                    });
            System.out.println(
                    "Passed: save/reopen, checkpoint lock, experimentation, live restore, protected"
                            + " close, persistent lock, and unlock/save.");
        } finally {
            Files.deleteIfExists(database);
            Files.delete(directory);
        }
    }

    private static <T extends Component> T find(Container root, Class<T> type, String text) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)
                    && (text == null
                            || child instanceof AbstractButton button
                                    && text.equals(button.getText()))) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T result = find(nested, type, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
