package dndcompanion.ui.character;

import dndcompanion.application.CharacterController;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

final class CharacterSaveControls extends JPanel {
    private final JButton save, discard;

    CharacterSaveControls(CharacterController editor, JLabel status, Runnable commit) {
        super(new BorderLayout());
        setOpaque(false);
        JPanel footer = FeatComponents.card(new BorderLayout(8, 0));
        footer.add(status, BorderLayout.CENTER);
        JPanel actions = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        discard = AppStyle.compactButton("Discard edits", editor::restoreSaved);
        actions.add(discard);
        save = AppStyle.primaryButton("Commit to grimoire", commit);
        save.putClientProperty("tactileRole", "red");
        actions.add(save);
        footer.add(actions, BorderLayout.EAST);
        add(footer);
    }

    void refresh(boolean canSave, boolean canRestore) {
        save.setEnabled(canSave);
        discard.setEnabled(canRestore);
        save.setToolTipText(
                canSave
                        ? "Save the current character"
                        : "Unlock the saved character before committing edits");
    }
}
