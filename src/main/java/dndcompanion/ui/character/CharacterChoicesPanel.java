package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterChoices;
import dndcompanion.domain.character.CharacterChoices.Choice;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.ChoiceIds;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.Species;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JViewport;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;

/**
 * Stable reminder forms: drafts survive resource refreshes; confirmation persists a complete
 * choice.
 */
final class CharacterChoicesPanel extends JPanel {
    private final PlaySession session;
    private final JPanel pending = FeatComponents.cardGrid(),
            completed = AppStyle.section(new GridBagLayout());
    private final JLabel count = AppStyle.badge("");
    private final JButton review;
    private final Map<String, ChoiceCard> cards = new HashMap<>();
    private final Set<String> editing = new HashSet<>();
    private boolean reviewing;
    private String layoutStamp = "";
    private long restoreRevision = -1;

    CharacterChoicesPanel(PlaySession session) {
        super(new BorderLayout(0, 8));
        this.session = session;
        setOpaque(false);
        setName("character-choices");
        JPanel heading = AppStyle.section(new BorderLayout(8, 0));
        heading.add(count, BorderLayout.WEST);
        review =
                AppStyle.compactButton(
                        "Review saved choices",
                        () -> {
                            reviewing = !reviewing;
                            layoutStamp = "";
                            refresh();
                        });
        heading.add(review, BorderLayout.EAST);
        add(heading, BorderLayout.NORTH);
        JPanel content = AppStyle.section(new BorderLayout(0, 8));
        content.add(pending, BorderLayout.NORTH);
        content.add(completed, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    void refresh() {
        CharacterSheet sheet = session.sheet();
        Species species = session.selection().species();
        List<Choice> required = session.requiredChoices();
        boolean restored = restoreRevision != session.restoreRevision();
        restoreRevision = session.restoreRevision();
        if (restored) {
            editing.clear();
            cards.values().forEach(card -> card.saved = null);
        }
        int remaining = session.pendingDecisions();
        count.setText(
                remaining == 0
                        ? "CREATION CHOICES COMPLETE"
                        : remaining + " CHOICE" + (remaining == 1 ? "" : "S") + " TO COMPLETE");
        review.setText(reviewing ? "Hide saved choices" : "Review saved choices");
        for (Choice choice : required)
            cards.computeIfAbsent(choice.id(), id -> new ChoiceCard(choice)).sync(choice, sheet);
        String stamp =
                species
                        + ":"
                        + reviewing
                        + ":"
                        + editing
                        + ":"
                        + required.stream()
                                .map(
                                        choice ->
                                                choice.id()
                                                        + ":"
                                                        + choice.complete(sheet)
                                                        + ":"
                                                        + sheet.choice(choice.id()))
                                .toList();
        if (!stamp.equals(layoutStamp)) {
            layoutStamp = stamp;
            pending.removeAll();
            completed.removeAll();
            for (Choice choice : required) {
                if (!choice.complete(sheet) || editing.contains(choice.id()))
                    pending.add(cards.get(choice.id()).panel);
                else if (reviewing) {
                    JPanel row = FeatComponents.card(new BorderLayout(8, 4));
                    row.putClientProperty("surfaceRole", "low");
                    JPanel text = AppStyle.section(new BorderLayout(0, 4));
                    text.add(AppStyle.label(choice.title(), 13, true), BorderLayout.NORTH);
                    text.add(
                            FeatComponents.text(String.join(", ", sheet.choice(choice.id())), 12),
                            BorderLayout.CENTER);
                    row.add(text);
                    row.add(
                            AppStyle.compactButton(
                                    "Edit",
                                    () -> {
                                        editing.add(choice.id());
                                        layoutStamp = "";
                                        refresh();
                                    }),
                            BorderLayout.EAST);
                    FeatComponents.row(completed, row);
                }
            }
            revalidate();
            repaint();
        }
        AppStyle.applyAppearance(this);
    }

    private final class ChoiceCard {
        final JPanel panel = FeatComponents.card(new BorderLayout(0, 5));
        final JPanel options = new ChoiceOptions();
        final Map<String, JCheckBox> checks = new LinkedHashMap<>();
        final JLabel counter = AppStyle.mono("", 12), error = AppStyle.label("", 12, false);
        final JTextArea explanation = FeatComponents.text("", 12);
        final JScrollPane optionScroll;
        final JButton select;
        Choice choice;
        List<String> saved;

        ChoiceCard(Choice choice) {
            this.choice = choice;
            panel.setName("choice-reminder-" + choice.id());
            panel.putClientProperty("surfaceRole", "lowest");
            panel.setBorder(new EmptyBorder(6, 6, 10, 10));
            select = AppStyle.compactButton("Select", () -> confirm());
            select.putClientProperty("tactileRole", "yellow");
            select.getAccessibleContext().setAccessibleName("Confirm " + choice.id());
            JPanel heading = AppStyle.surfacePanel(new BorderLayout(6, 0), "slotAvailable");
            heading.setBorder(new EmptyBorder(4, 6, 4, 6));
            JTextArea title = FeatComponents.text(choice.title().toUpperCase(Locale.ROOT), 13);
            title.setFont(new Font("Segoe UI", Font.BOLD, 13));
            AppStyle.setColorRole(title, "badgeText");
            heading.add(title);
            JPanel actions = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 6, 0));
            actions.add(counter);
            actions.add(select);
            heading.add(actions, BorderLayout.EAST);
            AppStyle.setColorRole(counter, "badgeText");
            panel.add(heading, BorderLayout.NORTH);
            JPanel body = AppStyle.section(new BorderLayout(0, 4));
            body.add(explanation, BorderLayout.NORTH);
            optionScroll =
                    new JScrollPane(options) {
                        @Override
                        public Dimension getPreferredSize() {
                            return new Dimension(
                                    350, Math.min(128, options.getPreferredSize().height + 4));
                        }
                    };
            AppStyle.scrollPane(optionScroll);
            optionScroll.getViewport().putClientProperty("surfaceRole", "lowest");
            body.add(optionScroll);
            panel.add(body);
            AppStyle.setColorRole(error, "error");
            error.setVisible(false);
            panel.add(error, BorderLayout.SOUTH);
        }

        void sync(Choice next, CharacterSheet sheet) {
            if (!choice.options().equals(next.options()) || checks.isEmpty()) {
                options.removeAll();
                checks.clear();
                saved = null;
                for (String option : next.options()) {
                    JCheckBox box = new JCheckBox(option);
                    AppStyle.roundButton(box);
                    box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
                    box.setBorder(new EmptyBorder(2, 3, 2, 3));
                    box.getAccessibleContext().setAccessibleName(next.id() + " option " + option);
                    box.addActionListener(event -> update());
                    checks.put(option, box);
                    options.add(box);
                }
            }
            choice = next;
            explanation.setText(next.explanation());
            List<String> stored = sheet.choice(next.id());
            if (!Objects.equals(stored, saved)) {
                saved = stored;
                checks.forEach((name, box) -> box.setSelected(stored.contains(name)));
                error.setText("");
                error.setVisible(false);
            }
            update();
        }

        List<String> selected() {
            return checks.entrySet().stream()
                    .filter(entry -> entry.getValue().isSelected())
                    .map(Map.Entry::getKey)
                    .toList();
        }

        void update() {
            checks.forEach(
                    (name, box) -> {
                        boolean available = session.proficiencyOptionAvailable(choice.id(), name);
                        if (!available) box.setSelected(false);
                        box.setToolTipText(available ? null : "Already proficient in this skill.");
                    });
            int size = selected().size();
            counter.setText(size + " / " + choice.count());
            select.setEnabled(choice.accepts(selected()));
            checks.forEach(
                    (name, box) ->
                            box.setEnabled(
                                    session.proficiencyOptionAvailable(choice.id(), name)
                                            && (box.isSelected() || size < choice.count())));
        }

        void confirm() {
            List<String> values = selected();
            if (!choice.accepts(values)
                    || values.stream()
                            .anyMatch(
                                    value ->
                                            !session.proficiencyOptionAvailable(
                                                    choice.id(), value))) {
                update();
                return;
            }
            if (choice.id().equals(CharacterChoices.HUMAN_FEAT.id())) {
                var feat = FeatCatalog.named(values.getFirst());
                if (session.snapshot().feats().containsKey(feat.name()) && !feat.repeatable()) {
                    error.setText("Already recorded; choose a different Origin feat.");
                    error.setVisible(true);
                    panel.revalidate();
                    return;
                }
                if (session.backgroundFeat(feat.name()) && !feat.repeatable()) {
                    error.setText(
                            "Already granted by your background; choose a different Origin feat.");
                    error.setVisible(true);
                    panel.revalidate();
                    return;
                }
            }
            editing.remove(choice.id());
            error.setText("");
            error.setVisible(false);
            CharacterSheet updated = session.sheet().withChoice(choice.id(), values);
            if (choice.id().equals(ChoiceIds.FEAT_KEEN_MIND)
                    || choice.id().equals(ChoiceIds.FEAT_OBSERVANT)) {
                var skill =
                        Arrays.stream(CharacterSheet.Skill.values())
                                .filter(item -> item.label.equals(values.getFirst()))
                                .findFirst()
                                .orElseThrow();
                String feat = choice.id().substring(5);
                updated =
                        updated.withChoice(
                                choice.id() + ".tier",
                                List.of(
                                        session.skillTierExcluding(skill, feat) > 0
                                                ? "Expertise"
                                                : "Proficiency"));
            }
            if (choice.id().equals(ChoiceIds.FEAT_MAGIC_INITIATE_LIST))
                updated =
                        updated.withChoice(ChoiceIds.FEAT_MAGIC_INITIATE_CANTRIPS, List.of())
                                .withChoice(ChoiceIds.FEAT_MAGIC_INITIATE_SPELL, List.of());
            session.setSheet(updated);
            refresh();
        }
    }

    private static final class ChoiceOptions extends JPanel implements Scrollable {
        ChoiceOptions() {
            super(new GridLayout(0, 1, 8, 2));
            setOpaque(false);
        }

        private void updateColumns() {
            int width =
                    getParent() instanceof JViewport viewport ? viewport.getWidth() : getWidth();
            ((GridLayout) getLayout()).setColumns(width >= 440 ? 2 : 1);
        }

        @Override
        public void doLayout() {
            updateColumns();
            super.doLayout();
        }

        @Override
        public Dimension getPreferredSize() {
            updateColumns();
            return super.getPreferredSize();
        }

        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }

        public int getScrollableUnitIncrement(Rectangle bounds, int orientation, int direction) {
            return 32;
        }

        public int getScrollableBlockIncrement(Rectangle bounds, int orientation, int direction) {
            return Math.max(32, bounds.height - 32);
        }
    }
}
