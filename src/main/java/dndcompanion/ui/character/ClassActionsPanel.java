package dndcompanion.ui.character;

import dndcompanion.domain.character.ClassActions;
import dndcompanion.domain.character.FighterRules;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

public final class ClassActionsPanel extends JPanel {
    private final PlaySession session;
    private final IntSupplier charisma;
    private final Consumer<String> log;
    private final Map<ClassActions.Action, JButton> actions = new LinkedHashMap<>();
    private final JPanel buttons = AppStyle.section(new GridLayout(0, 2, 6, 6));
    private final JLabel pools = AppStyle.mono("", 12);
    private final javax.swing.JTextArea features = AppStyle.text("");

    public ClassActionsPanel(PlaySession session, IntSupplier charisma, Consumer<String> log) {
        super(new BorderLayout(0, 8));
        this.session = session;
        this.charisma = charisma;
        this.log = log;
        setName("class-actions");
        setOpaque(false);
        JPanel card = AppStyle.card(new BorderLayout(0, 10));
        card.add(AppStyle.heading("FIGHTER // CHAMPION", 18), BorderLayout.NORTH);
        JPanel body = AppStyle.section(new BorderLayout(0, 8));
        body.add(pools, BorderLayout.NORTH);
        body.add(buttons);
        body.add(features, BorderLayout.SOUTH);
        card.add(body);
        add(card);
        for (var action : ClassActions.fighter()) {
            JButton button = AppStyle.compactButton(action.label(), () -> use(action));
            button.getAccessibleContext().setAccessibleName("Class action " + action.id());
            button.setToolTipText(action.trigger());
            actions.put(action, button);
            buttons.add(button);
        }
    }

    public void refresh() {
        setVisible(!session.sorcerer());
        if (session.sorcerer()) return;
        int level = session.selection().level();
        pools.setText(
                String.join(
                        " · ",
                        session.resources().stream()
                                .filter(p -> p.maximum() > 0)
                                .map(p -> p.label() + " " + p.remaining() + "/" + p.maximum())
                                .toList()));
        dndcompanion.ui.common.TextUpdates.set(features, FighterRules.features(level), false);
        actions.forEach(
                (action, button) -> {
                    button.setVisible(level >= action.level());
                    button.setEnabled(
                            action.pool().isBlank()
                                    || session.resource(action.pool()).remaining() > 0);
                });
        revalidate();
    }

    private void use(ClassActions.Action action) {
        JSpinner original = new JSpinner(new SpinnerNumberModel(0, -100, 100, 1));
        JSpinner dc = new JSpinner(new SpinnerNumberModel(10, 1, 100, 1));
        JPanel form = AppStyle.section(new BorderLayout(0, 8));
        form.add(
                AppStyle.label("<html>" + action.trigger() + "</html>", 13, false),
                BorderLayout.NORTH);
        JPanel inputs = AppStyle.section(new GridLayout(0, 2, 6, 6));
        if (action.id().equals("tacticalMind")) {
            inputs.add(new JLabel("Failed check total"));
            inputs.add(original);
            inputs.add(new JLabel("Target DC"));
            inputs.add(dc);
        } else if (action.id().equals("indomitable")) {
            inputs.add(new JLabel("Normal save modifier"));
            inputs.add(original);
        }
        form.add(inputs);
        if (JOptionPane.showConfirmDialog(this, form, action.label(), JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        var result =
                session.useClassAction(
                        action,
                        charisma.getAsInt(),
                        java.util.random.RandomGenerator.getDefault(),
                        (Integer) original.getValue(),
                        (Integer) dc.getValue());
        log.accept(
                result.error().isEmpty()
                        ? result.event()
                        : "Class action blocked: " + result.error());
        refresh();
    }
}
