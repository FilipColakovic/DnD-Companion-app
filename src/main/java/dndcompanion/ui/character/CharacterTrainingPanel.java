package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.card;
import static dndcompanion.ui.character.CharacterSheetComponents.labelled;
import static dndcompanion.ui.character.CharacterSheetComponents.text;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Note;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.EquipmentRules;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class CharacterTrainingPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final Map<String, JComboBox<String>> equipment = new HashMap<>();
    private final javax.swing.JLabel equipmentSummary = AppStyle.label("", 12, false);
    private final JTextArea languages = new JTextArea(2, 16),
            training = new JTextArea(3, 16),
            grantedTools = FeatComponents.text("", 12);

    CharacterTrainingPanel(CharacterFormBindings bindings) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        JPanel gear = card("TRAINING & GEAR");
        for (var choice : EquipmentRules.choices()) {
            JComboBox<String> selector = new JComboBox<>(choice.options().toArray(String[]::new));
            selector.getAccessibleContext().setAccessibleName(choice.title());
            selector.addActionListener(
                    event -> {
                        if (!bindings.updating.getAsBoolean())
                            session.setSheet(
                                    session.sheet()
                                            .withChoice(
                                                    choice.id(),
                                                    List.of((String) selector.getSelectedItem())));
                    });
            equipment.put(choice.id(), selector);
            FeatComponents.row(
                    gear, labelled(choice.title().toUpperCase(java.util.Locale.ROOT), selector));
        }
        FeatComponents.row(gear, equipmentSummary);
        FeatComponents.row(
                gear,
                labelled(
                        "HEALER'S KIT USES",
                        bindings.field(CharacterSheet.Field.HEALER_KIT_USES, "Healer's Kit uses")));
        training.setLineWrap(true);
        training.setWrapStyleWord(true);
        languages.setLineWrap(true);
        languages.setWrapStyleWord(true);
        training.getAccessibleContext().setAccessibleName("Training and equipment notes");
        languages.getAccessibleContext().setAccessibleName("Languages and tools");
        bindings.bindText(
                training,
                value -> session.setSheet(session.sheet().withNote(Note.TRAINING, value)));
        bindings.bindText(
                languages,
                value -> session.setSheet(session.sheet().withNote(Note.LANGUAGES, value)));
        JScrollPane trainingInput = FeatComponents.scroll(training),
                languageInput = FeatComponents.scroll(languages);
        trainingInput.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
        languageInput.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
        FeatComponents.row(gear, labelled("WEAPON / ARMOR TRAINING & EQUIPMENT", trainingInput));
        grantedTools.getAccessibleContext().setAccessibleName("Granted tool proficiencies");
        FeatComponents.row(gear, grantedTools);
        FeatComponents.row(gear, labelled("LANGUAGES & TOOLS", languageInput));
        add(gear);
    }

    void refresh(
            CharacterState state,
            CharacterSheet sheet,
            SorcererRules.Stats stats,
            boolean restored) {
        text(
                training,
                !session.sorcerer() && !sheet.values().containsKey("note.TRAINING")
                        ? "Simple and martial weapons; all armor and shields"
                        : sheet.note(Note.TRAINING));
        text(languages, sheet.note(Note.LANGUAGES));
        equipment.forEach(
                (id, selector) ->
                        selector.setSelectedItem(
                                sheet.choice(id).stream()
                                        .findFirst()
                                        .orElse(
                                                id.equals("equipment.armor")
                                                        ? "Unarmored"
                                                        : id.equals("equipment.weapon")
                                                                ? "Quarterstaff"
                                                                : id.equals("equipment.offhand")
                                                                        ? "None"
                                                                        : "No shield")));
        var weapon = EquipmentRules.weapon(sheet);
        equipmentSummary.setText(
                "AC "
                        + session.armorClass(state.charisma())
                        + " · Speed "
                        + session.speed()
                        + " ft · "
                        + weapon.damage()
                        + " / "
                        + weapon.mastery());
        equipmentSummary.setToolTipText(session.equipmentCastingError());
        if (!sheet.note(Note.CRAFTED_ITEM).isBlank())
            equipmentSummary.setToolTipText(
                    "Temporary crafted gear: "
                            + sheet.note(Note.CRAFTED_ITEM)
                            + " (expires at next Long Rest)");
        List<String> tools = session.grantedTools();
        grantedTools.setText(
                "Granted tools: "
                        + String.join(", ", tools)
                        + "\n"
                        + dndcompanion.domain.character.FeatEffects.passives(
                                sheet, session.feats().keySet()));
        grantedTools.setVisible(!grantedTools.getText().isBlank());
    }
}
