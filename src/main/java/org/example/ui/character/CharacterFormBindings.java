package org.example.ui.character;

import org.example.application.CharacterController;
import org.example.domain.character.CharacterSheet;
import org.example.domain.character.CharacterSheet.Field;
import org.example.domain.character.PlaySession;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

final class CharacterFormBindings {
    final GrimoirePanel workspace;
    final CharacterController editor;
    final PlaySession session;
    final BooleanSupplier updating;
    private final EnumMap<Field, JSpinner> fields = new EnumMap<>(Field.class);
    private final EnumMap<Field, List<JCheckBox>> deathMarkers = new EnumMap<>(Field.class);

    CharacterFormBindings(GrimoirePanel workspace, BooleanSupplier updating) {
        this.workspace = workspace;
        this.editor = workspace.characterController();
        this.session = workspace.playSession();
        this.updating = updating;
    }

    void refresh(CharacterSheet sheet) {
        fields.forEach((field, spinner) -> spinner.setValue(sheet.field(field)));
        deathMarkers.forEach(
                (field, markers) -> {
                    for (int index = 0; index < markers.size(); index++)
                        markers.get(index).setSelected(index < sheet.field(field));
                });
    }

    JSpinner spinner(String accessible, int initial, int min, int max, IntConsumer setter) {
        JSpinner spinner = new JSpinner(new SpinnerNumberModel(initial, min, max, 1));
        spinner.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
        spinner.setPreferredSize(new Dimension(76, 32));
        spinner.getAccessibleContext().setAccessibleName(accessible);
        spinner.addChangeListener(
                e -> {
                    if (!updating.getAsBoolean()) setter.accept((Integer) spinner.getValue());
                });
        return spinner;
    }

    JSpinner field(Field field, String title) {
        JSpinner spinner =
                spinner(
                        title,
                        field.initial,
                        field.minimum,
                        field.maximum,
                        value -> session.setSheet(session.sheet().withField(field, value)));
        fields.put(field, spinner);
        return spinner;
    }

    void bindText(JTextComponent component, Consumer<String> setter) {
        int limit =
                "Character name".equals(component.getAccessibleContext().getAccessibleName())
                        ? 60
                        : 2000;
        ((AbstractDocument) component.getDocument())
                .setDocumentFilter(
                        new DocumentFilter() {
                            @Override
                            public void insertString(
                                    FilterBypass b, int offset, String text, AttributeSet attrs)
                                    throws BadLocationException {
                                if (b.getDocument().getLength() + text.length() <= limit)
                                    super.insertString(b, offset, text, attrs);
                            }

                            @Override
                            public void replace(
                                    FilterBypass b,
                                    int offset,
                                    int length,
                                    String text,
                                    AttributeSet attrs)
                                    throws BadLocationException {
                                if (b.getDocument().getLength()
                                                - length
                                                + (text == null ? 0 : text.length())
                                        <= limit) super.replace(b, offset, length, text, attrs);
                            }
                        });
        component
                .getDocument()
                .addDocumentListener(
                        new DocumentListener() {
                            public void insertUpdate(DocumentEvent e) {
                                update();
                            }

                            public void removeUpdate(DocumentEvent e) {
                                update();
                            }

                            public void changedUpdate(DocumentEvent e) {
                                update();
                            }

                            private void update() {
                                if (!updating.getAsBoolean()) setter.accept(component.getText());
                            }
                        });
    }

    JPanel deathMarks(Field field, String title) {
        JPanel panel = AppStyle.section(new FlowLayout(FlowLayout.LEFT, 3, 0));
        List<JCheckBox> markers = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            int mark = index;
            JCheckBox box = new JCheckBox();
            AppStyle.roundButton(box);
            box.getAccessibleContext()
                    .setAccessibleName(
                            "Death save " + title.toLowerCase(Locale.ROOT) + " " + (index + 1));
            box.addActionListener(
                    event -> {
                        if (!updating.getAsBoolean())
                            session.setSheet(
                                    session.sheet()
                                            .withField(field, box.isSelected() ? mark + 1 : mark));
                    });
            markers.add(box);
            panel.add(box);
        }
        deathMarkers.put(field, markers);
        return panel;
    }
}
