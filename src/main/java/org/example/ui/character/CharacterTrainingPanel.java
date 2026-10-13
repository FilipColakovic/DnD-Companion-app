package org.example.ui.character;

import static org.example.ui.character.CharacterSheetComponents.card;
import static org.example.ui.character.CharacterSheetComponents.labelled;
import static org.example.ui.character.CharacterSheetComponents.text;

import org.example.application.CharacterController;
import org.example.domain.character.CharacterSheet;
import org.example.domain.character.CharacterSheet.Note;
import org.example.domain.character.CharacterState;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

final class CharacterTrainingPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
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
        text(training, sheet.note(Note.TRAINING));
        text(languages, sheet.note(Note.LANGUAGES));
        List<String> tools = session.grantedTools();
        grantedTools.setText(
                tools.isEmpty() ? "" : "Granted tool proficiencies: " + String.join(", ", tools));
        grantedTools.setVisible(!tools.isEmpty());
    }
}
