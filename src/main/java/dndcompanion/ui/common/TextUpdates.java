package dndcompanion.ui.common;

import javax.swing.text.JTextComponent;

/**
 * Avoid document replacement when a refresh has no text change; retain selection on real updates.
 */
public final class TextUpdates {
    private TextUpdates() {}

    public static void set(JTextComponent component, String value, boolean preserveSelection) {
        if (component.getText().equals(value)) return;
        int dot = preserveSelection ? component.getCaret().getDot() : 0;
        int mark = preserveSelection ? component.getCaret().getMark() : 0;
        component.setText(value);
        int length = component.getDocument().getLength();
        component.setCaretPosition(Math.min(mark, length));
        component.moveCaretPosition(Math.min(dot, length));
    }
}
