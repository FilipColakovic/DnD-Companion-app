package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.card;
import static dndcompanion.ui.character.CharacterSheetComponents.column;
import static dndcompanion.ui.character.CharacterSheetComponents.labelled;
import static dndcompanion.ui.character.CharacterSheetComponents.metric;
import static dndcompanion.ui.character.CharacterSheetComponents.signed;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterChoices;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterSheet.Field;
import dndcompanion.domain.character.CharacterSheet.Note;
import dndcompanion.domain.character.CharacterSheet.Skill;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.FeatCatalog;
import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.LiveView;
import dndcompanion.ui.spells.SpellDetailPanel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.beans.PropertyChangeListener;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.EmptyBorder;

/** Live single-character sheet based on the supplied three-column Bauhaus editor. */
public final class CharacterSheetPanel extends JPanel implements LiveView {
    private final GrimoirePanel workspace;
    private final CharacterController editor;
    private final PlaySession session;
    private final CharacterChoicesPanel choices;
    private final CharacterFormBindings bindings;
    private final CharacterIdentityPanel identity;
    private final CharacterVitalsPanel vitals;
    private final CharacterAbilityPanel abilities;
    private final CharacterSkillsPanel skills;
    private final CharacterTrainingPanel training;
    private final FeatActionsPanel featActions;
    private final CharacterSaveControls saveControls;
    private final EnumMap<Skill, JLabel> passives = new EnumMap<>(Skill.class);
    private final JSpinner sorceryPoints;
    private final Map<Integer, JSpinner> spellSlots = new LinkedHashMap<>();
    private final JLabel status = AppStyle.label("", 11, false);
    private final JPanel features = AppStyle.section(new GridBagLayout()),
            featList = AppStyle.section(new GridBagLayout());
    private final JButton innate, shortRest, longRest;
    private final PropertyChangeListener stateListener = event -> refresh();
    private boolean updating = true, disposed;
    private String featureSignature = "", featSignature = "";
    private long restoreRevision;

    public CharacterSheetPanel(GrimoirePanel workspace) {
        super(new BorderLayout());
        this.workspace = workspace;
        editor = workspace.characterPanel();
        session = workspace.playSession();
        bindings = new CharacterFormBindings(workspace, () -> updating);
        setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel content = new FeatComponents.WidthPanel(new GridBagLayout());
        JPanel heading = FeatComponents.card(new BorderLayout(10, 6));
        JPanel title = AppStyle.section(new BorderLayout(0, 5));
        title.add(AppStyle.label("CHARACTER & SHEET", 22, true), BorderLayout.NORTH);
        title.add(
                FeatComponents.tag(session.selection().classId() + " · PHB 2024", true),
                BorderLayout.WEST);
        heading.add(title, BorderLayout.CENTER);
        JPanel restActions = new FeatComponents.WrapPanel(5);
        shortRest = AppStyle.compactButton("Short Rest", () -> status.setText(session.shortRest()));
        restActions.add(shortRest);
        longRest = AppStyle.compactButton("Long Rest", session::longRest);
        restActions.add(longRest);
        innate =
                AppStyle.compactButton(
                        "Innate Sorcery", () -> status.setText(session.toggleInnate()));
        innate.getAccessibleContext().setAccessibleName("Toggle Innate Sorcery");
        innate.putClientProperty("tactileRole", "blue");
        restActions.add(innate);
        innate.setVisible(session.sorcerer());
        restActions.add(AppStyle.compactButton("Export sheet", this::printSheet));
        heading.add(restActions, BorderLayout.SOUTH);
        FeatComponents.row(content, heading);

        identity = new CharacterIdentityPanel(bindings);
        FeatComponents.row(content, identity);
        choices = new CharacterChoicesPanel(session);
        FeatComponents.row(content, choices);

        JPanel left = column(), middle = column(), right = column();
        vitals = new CharacterVitalsPanel(bindings, status::setText);
        FeatComponents.row(left, vitals);
        abilities = new CharacterAbilityPanel(bindings);
        FeatComponents.row(left, abilities);
        JPanel senses = card("PASSIVE SENSES");
        JPanel passiveTiles = new JPanel(new GridLayout(1, 3, 6, 0));
        passiveTiles.setOpaque(false);
        for (Skill skill : List.of(Skill.PERCEPTION, Skill.INSIGHT, Skill.INVESTIGATION)) {
            JLabel value = AppStyle.label("", 20, true);
            value.setName("passive-" + skill);
            passives.put(skill, value);
            passiveTiles.add(metric(skill.label.toUpperCase(Locale.ROOT), value));
        }
        FeatComponents.row(senses, passiveTiles);
        FeatComponents.row(middle, senses);
        skills = new CharacterSkillsPanel(bindings, this::rollSkill);
        FeatComponents.row(middle, skills);
        training = new CharacterTrainingPanel(bindings);
        FeatComponents.row(right, training);
        featActions =
                new FeatActionsPanel(
                        session,
                        () -> editor.snapshot().charisma(),
                        event -> workspace.combatPanel().recordEvent(event));
        FeatComponents.row(right, featActions);
        JPanel resources = card("SPELLCASTING RESOURCES");
        sorceryPoints =
                bindings.spinner(
                        "Remaining Sorcery Points",
                        session.remainingPoints(),
                        0,
                        20,
                        session::setRemainingPoints);
        FeatComponents.row(resources, labelled("SORCERY POINTS REMAINING", sorceryPoints));
        JPanel slots = new JPanel(new GridLayout(0, 3, 6, 6));
        slots.setOpaque(false);
        for (int slot = 1; slot <= 9; slot++) {
            int tier = slot;
            JSpinner remaining =
                    bindings.spinner(
                            "Remaining level " + tier + " slots",
                            session.remainingSlots(tier),
                            0,
                            4,
                            value -> session.setRemainingSlots(tier, value));
            spellSlots.put(tier, remaining);
            slots.add(labelled("LEVEL " + tier, remaining));
        }
        FeatComponents.row(resources, slots);
        FeatComponents.row(right, resources);
        resources.setVisible(session.sorcerer());
        JPanel traits = card("FEATURES & TRAITS");
        FeatComponents.row(traits, features);
        JPanel links = new FeatComponents.WrapPanel(5);
        if (session.sorcerer())
            links.add(
                    AppStyle.compactButton(
                            "Metamagic", () -> workspace.showPage(GrimoirePanel.METAMAGIC)));
        links.add(AppStyle.compactButton("Prepared spells", workspace::showPreparedSpells));
        links.add(
                AppStyle.compactButton(
                        "Choose feats", () -> workspace.showPage(GrimoirePanel.FEATS)));
        FeatComponents.row(traits, links);
        FeatComponents.row(right, traits);
        for (JPanel column : List.of(left, middle, right)) {
            GridBagConstraints glue = new GridBagConstraints();
            glue.gridx = 0;
            glue.gridy = column.getComponentCount();
            glue.weighty = 1;
            glue.fill = GridBagConstraints.VERTICAL;
            column.add(Box.createVerticalGlue(), glue);
        }
        JPanel columns =
                new JPanel(null) {
                    private boolean stacked() {
                        return getWidth() > 0
                                && getWidth() < 1050 + Math.max(0, AppStyle.textSize() - 14) * 50;
                    }

                    @Override
                    public Dimension getPreferredSize() {
                        int height = 0;
                        for (Component child : getComponents())
                            height =
                                    stacked()
                                            ? height + child.getPreferredSize().height
                                            : Math.max(height, child.getPreferredSize().height);
                        return new Dimension(
                                getWidth() > 0 ? getWidth() : 1100, height + (stacked() ? 24 : 0));
                    }

                    @Override
                    public void doLayout() {
                        int width = stacked() ? getWidth() : (getWidth() - 24) / 3, offset = 0;
                        for (Component child : getComponents()) {
                            int height = stacked() ? child.getPreferredSize().height : getHeight();
                            child.setBounds(
                                    stacked() ? 0 : offset, stacked() ? offset : 0, width, height);
                            offset += (stacked() ? height : width) + 12;
                        }
                    }
                };
        columns.setOpaque(false);
        columns.add(left);
        columns.add(middle);
        columns.add(right);
        FeatComponents.row(content, columns);
        JPanel selectedFeats = card("SELECTED FEATS");
        FeatComponents.row(selectedFeats, featList);
        FeatComponents.row(content, selectedFeats);
        add(FeatComponents.scroll(content), BorderLayout.CENTER);
        saveControls = new CharacterSaveControls(editor, status, this::commit);
        add(saveControls, BorderLayout.SOUTH);
        editor.addPropertyChangeListener("characterState", stateListener);
        updating = false;
        refresh();
    }

    public void refresh() {
        if (updating || disposed) return;
        updating = true;
        try {
            CharacterState state = editor.snapshot();
            CharacterSheet sheet = session.sheet();
            var stats =
                    session.stats(
                            session.effectiveScore(
                                    dndcompanion.domain.character.CharacterSheet.Ability.CHA,
                                    state.charisma()));
            boolean restored = restoreRevision != session.restoreRevision();
            restoreRevision = session.restoreRevision();
            identity.refresh(state, sheet, stats, restored);
            bindings.refresh(sheet);
            vitals.refresh(state, sheet, stats, restored);
            abilities.refresh(state, sheet, stats, restored);
            skills.refresh(state, sheet, stats, restored);
            passives.forEach(
                    (skill, value) ->
                            value.setText(
                                    Integer.toString(
                                            10 + session.skillBonus(skill, state.charisma()))));
            choices.refresh();
            training.refresh(state, sheet, stats, restored);
            featActions.refresh();
            boolean canRest = session.maxHitPoints() == 0 || session.hitPoints() > 0;
            shortRest.setEnabled(canRest);
            longRest.setEnabled(canRest);
            shortRest.setToolTipText(
                    canRest
                            ? "Record a completed Short Rest"
                            : "A rest must start with at least 1 HP");
            longRest.setToolTipText(
                    canRest
                            ? "Record a completed Long Rest"
                            : "A rest must start with at least 1 HP");
            ((SpinnerNumberModel) sorceryPoints.getModel()).setMaximum(session.maxPoints());
            sorceryPoints.setValue(session.remainingPoints());
            sorceryPoints.setEnabled(session.maxPoints() > 0);
            spellSlots.forEach(
                    (slot, spinner) -> {
                        ((SpinnerNumberModel) spinner.getModel())
                                .setMaximum(session.maxSlots(slot));
                        spinner.setValue(session.remainingSlots(slot));
                        spinner.setEnabled(session.maxSlots(slot) > 0);
                    });
            innate.setText(
                    session.innateActive()
                            ? "Innate active"
                            : "Innate Sorcery "
                                    + Math.max(0, 2 - state.play().innateUses())
                                    + " / 2");
            innate.putClientProperty("tactileRole", session.innateActive() ? "red" : "blue");
            saveControls.refresh(workspace.canSaveNow(), workspace.canRestoreSaved());
            status.setText(editor.saveStatusText());
            refreshFeatures(state);
            AppStyle.applyAppearance(this);
        } finally {
            updating = false;
        }
    }

    private void refreshFeatures(CharacterState state) {
        String configured =
                session.sheet().values().entrySet().stream()
                        .filter(entry -> entry.getKey().startsWith("choice."))
                        .sorted(Map.Entry.comparingByKey())
                        .toList()
                        .toString();
        String signature =
                state.level()
                        + ":"
                        + state.subclass()
                        + ":"
                        + state.species()
                        + ":"
                        + session.metamagic()
                        + ":"
                        + session.feats().keySet()
                        + ":"
                        + configured;
        if (!signature.equals(featureSignature)) {
            featureSignature = signature;
            features.removeAll();
            String speciesChoices =
                    CharacterChoices.speciesSummary(session.sheet(), state.species());
            FeatComponents.row(
                    features,
                    FeatComponents.block(
                            "SUBCLASS & LINEAGE",
                            FeatComponents.text(
                                    state.classes().getFirst().subclassLabel()
                                            + "\n"
                                            + state.species()
                                            + (speciesChoices.isBlank()
                                                    ? ""
                                                    : "\n" + speciesChoices)
                                            + (state.level() < 3
                                                    ? "\nSubclass features unlock at level 3."
                                                    : ""),
                                    12)));
            for (Metamagic option :
                    session.metamagic().stream()
                            .sorted(Comparator.comparing(Metamagic::toString))
                            .toList()) {
                JLabel meta = FeatComponents.tag(option + " · " + option.cost() + " SP", false);
                meta.setToolTipText(option.description());
                FeatComponents.row(features, meta);
            }
            if (!session.sorcerer())
                FeatComponents.row(
                        features,
                        FeatComponents.block(
                                "CLASS FEATURES",
                                FeatComponents.text(
                                        dndcompanion.domain.character.FighterRules.features(
                                                state.level()),
                                        12)));
            List<SpellCatalog.Spell> grants =
                    session.selection().catalog().stream()
                            .filter(session.selection()::automatic)
                            .filter(session.selection()::available)
                            .toList();
            if (!grants.isEmpty()) {
                JPanel buttons = new FeatComponents.WrapPanel(4);
                for (SpellCatalog.Spell spell : grants)
                    buttons.add(
                            AppStyle.compactButton(
                                    spell.name(),
                                    () ->
                                            AppStyle.openWindow(
                                                    this,
                                                    spell.name(),
                                                    new SpellDetailPanel(spell))));
                FeatComponents.row(
                        features, FeatComponents.block("AUTOMATIC SPELL GRANTS", buttons));
            }
            features.revalidate();
            features.repaint();
        }
        String feats = session.feats() + ":" + configured;
        if (!feats.equals(featSignature)) {
            featSignature = feats;
            featList.removeAll();
            session.feats().entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(
                            entry -> {
                                JPanel row = AppStyle.section(new BorderLayout(8, 4));
                                JButton title =
                                        AppStyle.textButton(
                                                entry.getKey()
                                                        + " · "
                                                        + FeatCatalog.named(entry.getKey())
                                                                .category(),
                                                () -> workspace.inspectFeat(entry.getKey()));
                                String config = session.featConfiguration(entry.getKey());
                                row.add(title, BorderLayout.NORTH);
                                row.add(
                                        FeatComponents.text(
                                                (entry.getValue().isBlank()
                                                                ? FeatCatalog.named(entry.getKey())
                                                                        .summary()
                                                                : entry.getValue())
                                                        + (config.isBlank() ? "" : "\n" + config),
                                                12),
                                        BorderLayout.CENTER);
                                FeatComponents.row(featList, row);
                            });
            if (session.feats().isEmpty())
                FeatComponents.row(featList, AppStyle.label("No feats selected", 12, false));
            featList.revalidate();
            featList.repaint();
        }
    }

    private void rollSkill(Skill skill) {
        CharacterState state = editor.snapshot();
        int bonus = session.skillBonus(skill, state.charisma());
        int die = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 21);
        boolean advantage = skills.advantage(skill);
        int second =
                advantage ? java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 21) : die;
        workspace
                .combatPanel()
                .recordEvent(
                        skill.label
                                + " check: d20 ["
                                + die
                                + (advantage ? ", " + second + " · Advantage" : "")
                                + "] "
                                + signed(bonus)
                                + " = "
                                + (Math.max(die, second) + bonus));
        status.setText(skill.label + " roll recorded in combat log");
    }

    private void commit() {
        if (!identity.hasName()) {
            status.setText("Enter a character name before saving.");
            return;
        }
        workspace.saveNow();
    }

    private void printSheet() {
        CharacterState state = editor.snapshot();
        CharacterSheet sheet = session.sheet();
        var stats =
                session.stats(
                        session.effectiveScore(
                                dndcompanion.domain.character.CharacterSheet.Ability.CHA,
                                state.charisma()));
        JTextArea document =
                new JTextArea(
                        state.name()
                                + " // "
                                + state.classId()
                                + " "
                                + state.level()
                                + "\n"
                                + state.classes().getFirst().subclassLabel()
                                + " // "
                                + state.species()
                                + "\n\n");
        document.setLineWrap(true);
        document.setWrapStyleWord(true);
        document.append(
                "Background: "
                        + dndcompanion.domain.character.BackgroundCatalog.selected(sheet)
                                .map(
                                        dndcompanion.domain.character.BackgroundCatalog.Background
                                                ::name)
                                .orElse(sheet.note(Note.BACKGROUND))
                        + "; Alignment: "
                        + sheet.note(Note.ALIGNMENT)
                        + "\n");
        for (Ability ability : Ability.values())
            document.append(
                    ability
                            + " "
                            + session.effectiveScore(ability, state.charisma())
                            + " ("
                            + signed(
                                    CharacterSheet.modifier(
                                            session.effectiveScore(ability, state.charisma())))
                            + ")\n");
        for (Skill skill : Skill.values())
            document.append(
                    skill.label + " " + signed(session.skillBonus(skill, state.charisma())) + "\n");
        document.append(
                "\nHP "
                        + session.hitPoints()
                        + " / "
                        + session.maxHitPoints()
                        + "; Temp HP "
                        + sheet.field(Field.TEMP_HP)
                        + "\n"
                        + sheet.note(Note.TRAINING)
                        + "\n"
                        + sheet.note(Note.LANGUAGES)
                        + "\n\nFeats\n");
        session.feats().forEach((feat, notes) -> document.append(feat + ": " + notes + "\n"));
        document.append("\nConfirmed creation choices\n");
        session.requiredChoices().stream()
                .filter(choice -> choice.complete(sheet))
                .forEach(
                        choice ->
                                document.append(
                                        choice.title()
                                                + ": "
                                                + String.join(", ", sheet.choice(choice.id()))
                                                + "\n"));
        for (var pool : session.resources())
            document.append(
                    "\n" + pool.label() + " " + pool.remaining() + " / " + pool.maximum() + "\n");
        for (int tier = 1; tier <= 9; tier++)
            if (session.maxSlots(tier) > 0)
                document.append(
                        "Level "
                                + tier
                                + " slots: "
                                + session.remainingSlots(tier)
                                + " / "
                                + session.maxSlots(tier)
                                + "\n");
        document.append("\nKnown / prepared spells\n");
        session.selection().catalog().stream()
                .filter(session.selection()::selected)
                .filter(session.selection()::available)
                .forEach(spell -> document.append(spell.levelName() + ": " + spell.name() + "\n"));
        try {
            document.print();
        } catch (java.awt.print.PrinterException error) {
            status.setText("Could not print sheet: " + error.getMessage());
        }
    }

    public void dispose() {
        disposed = true;
        editor.removePropertyChangeListener("characterState", stateListener);
    }
}
