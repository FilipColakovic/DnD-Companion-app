package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.labelled;
import static dndcompanion.ui.character.CharacterSheetComponents.text;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.BackgroundCatalog;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Note;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.PillSelector;

import java.awt.BorderLayout;
import java.awt.GridBagLayout;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

final class CharacterIdentityPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final JTextField name = new JTextField(20),
            background = new JTextField(15),
            alignment = new JTextField(12);
    private final SubclassSelector subclass;
    private final PillSelector<Species> species;
    private final JSpinner level;

    CharacterIdentityPanel(CharacterFormBindings bindings) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel identity = FeatComponents.card(new GridBagLayout());
        name.setName("character-name");
        name.getAccessibleContext().setAccessibleName("Character name");
        name.setText(editor.characterName());
        bindings.bindText(
                name,
                value -> {
                    String clean = value.strip();
                    if (!clean.isEmpty() && clean.length() <= 60) editor.setCharacterName(clean);
                });
        FeatComponents.row(identity, labelled("CHARACTER NAME & EPITHET", name));
        JPanel origin = new FeatComponents.WrapPanel(8);
        subclass = new SubclassSelector(editor::selectSubclass);
        species =
                new PillSelector<>(List.of(Species.values()), Species.HUMAN, editor::selectSpecies);
        species.getAccessibleContext().setAccessibleName("Select PHB 2024 species and lineage");
        origin.add(labelled("SUBCLASS", subclass));
        if (!session.sorcerer()) {
            origin.remove(origin.getComponentCount() - 1);
            origin.add(
                    labelled(
                            "CLASS / SUBCLASS",
                            AppStyle.label("Fighter / Champion (level 3+)", 13, true)));
        }
        origin.add(labelled("SPECIES & LINEAGE", species));
        level = bindings.spinner("Character level", 1, 1, 20, value -> editor.setLevel(value));
        origin.add(labelled("CLASS LEVEL", level));
        origin.add(labelled("BACKGROUND", background));
        origin.add(labelled("ALIGNMENT", alignment));
        FeatComponents.row(identity, origin);
        origin.add(AppStyle.compactButton("Choose background", workspace::showBackgrounds));
        bindings.bindText(
                background,
                value -> session.setSheet(session.sheet().withNote(Note.BACKGROUND, value)));
        bindings.bindText(
                alignment,
                value -> session.setSheet(session.sheet().withNote(Note.ALIGNMENT, value)));
        add(identity);
    }

    void refresh(
            CharacterState state,
            CharacterSheet sheet,
            SorcererRules.Stats stats,
            boolean restored) {
        if (restored || !name.hasFocus()) text(name, state.name());
        text(background, sheet.note(Note.BACKGROUND));
        text(alignment, sheet.note(Note.ALIGNMENT));
        background.setEditable(BackgroundCatalog.selected(sheet).isEmpty());
        background.setToolTipText(
                BackgroundCatalog.selected(sheet).isPresent()
                        ? "Selected PHB background; change it in Feats → Backgrounds"
                        : "Free-form background note; use Choose background to select a PHB"
                                + " background");
        subclass.setSubclass(state.subclass());
        species.setSelectedOption(state.species());
        level.setValue(state.level());
    }

    boolean hasName() {
        return !name.getText().strip().isEmpty();
    }
}
