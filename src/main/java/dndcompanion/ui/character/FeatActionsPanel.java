package dndcompanion.ui.character;

import dndcompanion.domain.character.FeatActions;
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

/** Triggers are confirmed explicitly because the app does not own enemy or turn state. */
public final class FeatActionsPanel extends JPanel {
    private final PlaySession session;
    private final IntSupplier charisma;
    private final Consumer<String> log;
    private final JPanel actions = AppStyle.section(new GridLayout(0, 2, 5, 5));
    private final JLabel status = AppStyle.label("", 12, false);
    private final Map<FeatActions.Action, JButton> buttons = new LinkedHashMap<>();

    public FeatActionsPanel(PlaySession session, IntSupplier charisma, Consumer<String> log) {
        super(new BorderLayout(0, 6));
        this.session = session;
        this.charisma = charisma;
        this.log = log;
        setOpaque(false);
        add(AppStyle.label("FEAT ACTIONS", 14, true), BorderLayout.NORTH);
        add(actions, BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
        for (var action : FeatActions.all()) {
            JButton button = AppStyle.compactButton(action.label(), () -> use(action));
            button.setToolTipText(action.feat() + ": " + action.trigger());
            button.getAccessibleContext().setAccessibleName(action.feat() + " " + action.label());
            buttons.put(action, button);
        }
    }

    public void refresh() {
        var active =
                buttons.entrySet().stream()
                        .filter(e -> session.feats().containsKey(e.getKey().feat()))
                        .toList();
        ((GridLayout) actions.getLayout()).setColumns(active.size() == 1 ? 1 : 2);
        if (actions.getComponentCount() != active.size()
                || active.stream().anyMatch(e -> e.getValue().getParent() != actions)) {
            actions.removeAll();
            active.forEach(e -> actions.add(e.getValue()));
            actions.revalidate();
        }
        active.forEach(
                e -> {
                    int remaining = session.remainingFeatUses(e.getKey());
                    e.getValue().setEnabled(session.featActionAvailable(e.getKey()));
                    e.getValue()
                            .setToolTipText(
                                    e.getKey().feat().equals("Telekinetic")
                                                    && !session.featActionAvailable(e.getKey())
                                            ? "Cast Mage Hand first."
                                            : e.getKey().feat() + ": " + e.getKey().trigger());
                    e.getValue()
                            .setText(
                                    e.getKey().label()
                                            + (remaining == Integer.MAX_VALUE
                                                    ? ""
                                                    : " (" + remaining + ")"));
                });
        setVisible(!active.isEmpty());
    }

    private void use(FeatActions.Action action) {
        if (JOptionPane.showConfirmDialog(
                        this,
                        action.trigger() + "\n\nRecord this use?",
                        action.feat(),
                        JOptionPane.OK_CANCEL_OPTION)
                != JOptionPane.OK_OPTION) return;
        int sides = dndcompanion.domain.character.EquipmentRules.weapon(session.sheet()).die();
        String item = "";
        if (action.feat().equals("Crafter")) {
            var items = FeatActions.craftingItems(session.grantedTools());
            if (items.isEmpty()) {
                status.setText("Choose proficient crafting tools in Character first.");
                return;
            }
            item =
                    (String)
                            JOptionPane.showInputDialog(
                                    this,
                                    "Choose temporary gear",
                                    "Fast Crafting",
                                    JOptionPane.PLAIN_MESSAGE,
                                    null,
                                    items.toArray(String[]::new),
                                    items.getFirst());
            if (item == null) return;
        }
        if (action.feat().equals("Piercer")) {
            Integer selected =
                    (Integer)
                            JOptionPane.showInputDialog(
                                    this,
                                    "Choose the damage die being rerolled",
                                    "Piercer",
                                    JOptionPane.PLAIN_MESSAGE,
                                    null,
                                    new Integer[] {4, 6, 8, 10, 12},
                                    sides);
            if (selected == null) return;
            sides = selected;
        }
        var outcome =
                session.useFeatAction(
                        action,
                        charisma.getAsInt(),
                        java.util.random.RandomGenerator.getDefault(),
                        sides,
                        item);
        status.setText(outcome.error().isEmpty() ? "Recorded in combat log" : outcome.error());
        if (outcome.error().isEmpty()) log.accept(outcome.event());
        refresh();
    }
}
