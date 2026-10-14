package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellMechanics;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.LiveView;
import dndcompanion.ui.spells.SpellDetailPanel;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/** A stable inspector instance per feat: refreshing never replaces editable controls. */
final class FeatInspector extends JPanel implements LiveView {
    private final FeatCatalog.Feat feat;
    private final PlaySession session;
    private final Supplier<CharacterState> character;
    private final Consumer<String> message;
    private final JLabel status = AppStyle.label("", 12, false);
    private final JCheckBox confirm = new JCheckBox("Other prerequisites met on my sheet");
    private final JTextArea notes = new JTextArea(3, 18);
    private final JSpinner previewLevel;
    private final JTextArea previewResult = FeatComponents.text("", 12);
    private final JPanel connections = new FeatComponents.WrapPanel(4);
    private final JButton select, remove;
    private final JPanel confirmationRow = AppStyle.section(new BorderLayout());
    private List<SpellCatalog.Spell> lastConnections = List.of();
    private String savedNotes;
    private long restoreRevision;
    private final Runnable listener = this::refresh;

    FeatInspector(
            FeatCatalog.Feat feat,
            PlaySession session,
            Supplier<CharacterState> character,
            Consumer<String> message,
            Runnable openDetails) {
        super(new BorderLayout(0, 10));
        this.feat = feat;
        this.session = session;
        this.character = character;
        this.message = message;
        setOpaque(false);
        savedNotes = session.feats().get(feat.name());
        restoreRevision = session.restoreRevision();
        JPanel card = FeatComponents.card(new BorderLayout(0, 10));
        add(card, BorderLayout.CENTER);
        JPanel heading = AppStyle.section(new BorderLayout(0, 6));
        JPanel band = AppStyle.surfacePanel(new BorderLayout(), "badgeDark");
        band.setBorder(new EmptyBorder(10, 9, 10, 9));
        JLabel caption = AppStyle.mono("FEAT CODEX INSPECTOR", 11);
        AppStyle.setColorRole(caption, "badgeYellowText");
        band.add(caption, BorderLayout.WEST);
        band.add(FeatComponents.tag("PHB 2024", true), BorderLayout.EAST);
        heading.add(band, BorderLayout.NORTH);
        JTextArea name = FeatComponents.text(feat.name().toUpperCase(Locale.ROOT), 26);
        name.setFont(new Font("Segoe UI", Font.BOLD, 26));
        heading.add(name, BorderLayout.CENTER);
        JPanel tags = new FeatComponents.WrapPanel(4);
        tags.add(FeatComponents.tag(feat.category().toString().toUpperCase(Locale.ROOT), true));
        if (feat.repeatable()) tags.add(FeatComponents.tag("REPEATABLE", false));
        heading.add(tags, BorderLayout.SOUTH);
        card.add(heading, BorderLayout.NORTH);
        JPanel body = AppStyle.section(new GridBagLayout());
        JPanel prerequisites = AppStyle.section(new BorderLayout(0, 4));
        prerequisites.add(
                FeatComponents.text(
                        "Level "
                                + feat.minimumLevel()
                                + "+"
                                + (feat.prerequisite().isEmpty()
                                        ? ""
                                        : " · " + feat.prerequisite()),
                        12),
                BorderLayout.NORTH);
        prerequisites.add(status, BorderLayout.CENTER);
        AppStyle.roundButton(confirm);
        confirm.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        confirmationRow.add(confirm, BorderLayout.CENTER);
        prerequisites.add(confirmationRow, BorderLayout.SOUTH);
        FeatComponents.row(body, FeatComponents.block("PREREQUISITE STATUS", prerequisites));
        FeatComponents.row(
                body, FeatComponents.block("BENEFITS", FeatComponents.text(feat.summary(), 12)));
        if (!feat.abilityIncrease().isEmpty())
            FeatComponents.row(
                    body,
                    FeatComponents.block(
                            "ABILITY INCREASE", FeatComponents.text(feat.abilityIncrease(), 12)));
        FeatComponents.row(body, FeatComponents.block("PREPARED SPELL CONNECTIONS", connections));
        notes.setText(savedNotes == null ? "" : savedNotes);
        notes.setLineWrap(true);
        notes.setWrapStyleWord(true);
        notes.getAccessibleContext().setAccessibleName("Notes for " + feat.name());
        JPanel notesBody = AppStyle.section(new BorderLayout(0, 6));
        notesBody.add(
                FeatComponents.text(
                        "Record ability choices, bonus spells and proficiencies here; apply their"
                                + " effects on your sheet."
                                + (feat.repeatable()
                                        ? " Record additional instances under the feat's"
                                                + " repeatability conditions."
                                        : ""),
                        11),
                BorderLayout.NORTH);
        notesBody.add(new JScrollPane(notes), BorderLayout.CENTER);
        FeatComponents.row(body, fold("Choices & notes", notesBody));
        int level = character.get().level(), next = FeatCatalog.nextMilestone(level);
        previewLevel = new JSpinner(new SpinnerNumberModel(Math.max(level, next), 1, 20, 1));
        previewLevel.getAccessibleContext().setAccessibleName("Feat preview level");
        JPanel previewBody = AppStyle.section(new BorderLayout(8, 6));
        previewBody.add(AppStyle.label("Preview level", 12, false), BorderLayout.WEST);
        previewBody.add(previewLevel, BorderLayout.EAST);
        previewBody.add(previewResult, BorderLayout.SOUTH);
        FeatComponents.row(body, fold("Milestone preview", previewBody));
        previewLevel.addChangeListener(event -> refreshPreview());
        confirm.addActionListener(event -> updateActions());
        card.add(body, BorderLayout.CENTER);
        JPanel actions = AppStyle.section(new BorderLayout(0, 6));
        select =
                AppStyle.primaryButton(
                        "Equip feat",
                        () ->
                                message.accept(
                                        session.equipFeat(
                                                feat,
                                                notes.getText(),
                                                character.get().charisma(),
                                                confirm.isSelected())));
        select.putClientProperty("tactileRole", "red");
        select.getAccessibleContext().setAccessibleName("Equip " + feat.name());
        actions.add(select, BorderLayout.NORTH);
        JPanel secondary = new FeatComponents.WrapPanel(4);
        remove = AppStyle.compactButton("Remove feat", () -> session.removeFeat(feat.name()));
        secondary.add(remove);
        secondary.add(AppStyle.compactButton("Full PHB entry", this::openSource));
        if (openDetails != null)
            secondary.add(AppStyle.compactButton("Open details window", openDetails));
        actions.add(secondary, BorderLayout.CENTER);
        card.add(actions, BorderLayout.SOUTH);
        session.addListener(listener);
        session.selection().addChangeListener(listener);
        refresh();
    }

    private JPanel fold(String title, JComponent content) {
        JPanel panel = AppStyle.section(new BorderLayout(0, 6));
        content.setVisible(false);
        JButton toggle =
                AppStyle.compactButton("Show " + title.toLowerCase(Locale.ROOT) + " +", () -> {});
        toggle.addActionListener(
                event -> {
                    content.setVisible(!content.isVisible());
                    toggle.setText(
                            (content.isVisible() ? "Hide " : "Show ")
                                    + title.toLowerCase(Locale.ROOT)
                                    + (content.isVisible() ? " -" : " +"));
                    panel.revalidate();
                    panel.repaint();
                });
        panel.add(toggle, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);
        return panel;
    }

    void refresh() {
        String saved = session.feats().get(feat.name());
        boolean restored = restoreRevision != session.restoreRevision();
        restoreRevision = session.restoreRevision();
        if (restored || !Objects.equals(saved, savedNotes)) {
            String value = saved == null ? "" : saved;
            if (!notes.getText().equals(value)) notes.setText(value);
            savedNotes = saved;
            if (restored) confirm.setSelected(saved != null);
        }
        updateActions();
        refreshPreview();
        List<SpellCatalog.Spell> spells = connections();
        if (!spells.equals(lastConnections) || connections.getComponentCount() == 0) {
            lastConnections = spells;
            connections.removeAll();
            for (SpellCatalog.Spell spell : spells)
                connections.add(
                        AppStyle.compactButton(
                                spell.name(),
                                () ->
                                        AppStyle.openWindow(
                                                this, spell.name(), new SpellDetailPanel(spell))));
            if (spells.isEmpty())
                connections.add(AppStyle.label("No matching prepared spells", 11, false));
            connections.revalidate();
            connections.repaint();
        }
        AppStyle.applyAppearance(this);
    }

    private void updateActions() {
        CharacterState state = character.get();
        boolean selected = session.feats().containsKey(feat.name());
        status.setText(FeatListModel.status(feat, state));
        status.setToolTipText(status.getText());
        confirmationRow.setVisible(feat.needsConfirmation(state.charisma()));
        select.setText(selected ? "Save feat notes" : "Equip feat");
        remove.setVisible(
                selected
                        && (!session.backgroundFeat(feat.name())
                                || session.snapshot().feats().containsKey(feat.name())));
        select.setEnabled(
                selected
                        || feat.requirements(state.level(), state.charisma()).isEmpty()
                                && (!feat.needsConfirmation(state.charisma())
                                        || confirm.isSelected()));
    }

    private void refreshPreview() {
        CharacterState state = character.get();
        int level = (Integer) previewLevel.getValue();
        String required = feat.requirements(level, state.charisma());
        String result = required.isEmpty() ? "Level requirement met" : required;
        if (feat.needsConfirmation(state.charisma())) result += "; confirm other prerequisites.";
        if (feat.abilityIncrease().contains("CHA") || feat.abilityIncrease().startsWith("Any")) {
            int bonus = feat.name().equals("Ability Score Improvement") ? 2 : 1;
            int charisma =
                    Math.max(
                            state.charisma(),
                            Math.min(
                                    feat.category() == FeatCatalog.Category.EPIC_BOON ? 30 : 20,
                                    state.charisma() + bonus));
            var stats = SorcererRules.calculate(level, charisma, state.subclass());
            result +=
                    "\nIf allocated to CHA: "
                            + charisma
                            + ", save DC "
                            + stats.spellSaveDc()
                            + ", attack "
                            + String.format("%+d", stats.spellAttack());
        }
        result += "\nPreview leaves your character unchanged.";
        if (!previewResult.getText().equals(result)) previewResult.setText(result);
    }

    private List<SpellCatalog.Spell> connections() {
        return session.selection().catalog().stream()
                .filter(session.selection()::selected)
                .filter(session.selection()::available)
                .filter(
                        spell ->
                                switch (feat.name()) {
                                    case "War Caster" ->
                                            SpellDetails.forSpell(spell).concentration();
                                    case "Spell Sniper" -> SpellMechanics.hasAttack(spell);
                                    case "Elemental Adept" ->
                                            SpellMechanics.hasElementalDamage(spell);
                                    case "Telekinetic" -> spell.name().equals("Mage Hand");
                                    case "Telepathic" -> spell.name().equals("Detect Thoughts");
                                    case "Fey-Touched" ->
                                            spell.name().equals("Misty Step")
                                                    || spell.level() == 1
                                                            && Set.of("Divination", "Enchantment")
                                                                    .contains(
                                                                            SpellDetails.forSpell(
                                                                                            spell)
                                                                                    .school());
                                    case "Shadow-Touched" ->
                                            spell.name().equals("Invisibility")
                                                    || spell.level() == 1
                                                            && Set.of("Illusion", "Necromancy")
                                                                    .contains(
                                                                            SpellDetails.forSpell(
                                                                                            spell)
                                                                                    .school());
                                    default -> false;
                                })
                .toList();
    }

    private void openSource() {
        try {
            Desktop.getDesktop().browse(URI.create(feat.sourceUrl()));
        } catch (Exception error) {
            message.accept("Could not open browser. Source: " + feat.sourceUrl());
        }
    }

    String draft() {
        return notes.getText();
    }

    public void dispose() {
        session.removeListener(listener);
        session.selection().removeChangeListener(listener);
    }
}
