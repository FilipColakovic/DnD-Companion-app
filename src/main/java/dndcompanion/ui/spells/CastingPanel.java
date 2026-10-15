package dndcompanion.ui.spells;

import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.domain.spells.DamageDice;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.domain.spells.SpellMechanics;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.LiveView;
import dndcompanion.ui.common.TextUpdates;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;

/** Cast & Combat workstation. Every resource and selection belongs to the shared character. */
public final class CastingPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final IntSupplier charisma;
    private SpellCatalog.Spell selected;
    private int castingLevel;
    private final JPanel arsenal = new CombatArsenalGrid();
    private final JPanel spellList;
    private final CombatTierFilters filters = new CombatTierFilters(this::rebuildArsenal);
    private final CombatDossier dossier = new CombatDossier();
    private final CombatMetamagic metamagic;
    private final CombatLog combatLog = new CombatLog();
    private final CastControls castControls = new CastControls(this::cast, this::rollHealing);
    private final JTextArea preview = dossier.preview;
    private final JLabel dossierTitle = dossier.dossierTitle,
            dossierSubtitle = dossier.dossierSubtitle,
            cost = castControls.cost;
    private final JTextArea result = castControls.result;
    private final JButton castButton = castControls.castButton;
    private final CombatDiceTray diceTray;
    private final dndcompanion.ui.character.FeatActionsPanel featActions;
    private final dndcompanion.ui.character.ClassActionsPanel classActions;
    private final dndcompanion.ui.character.FeatCombatPanel equipmentActions;
    private final JButton damageButton, criticalButton;
    private Runnable spellbookAction;
    private final RandomGenerator random = RandomGenerator.getDefault();
    private final Runnable listener = this::refresh;
    private boolean updating, disposed;
    private List<SpellCatalog.Spell> available = List.of(), visible = List.of();
    private final Map<String, CombatSpellCard> spellCards = new HashMap<>();

    public CastingPanel(PlaySession session, IntSupplier charisma) {
        super(new BorderLayout(0, 12));
        this.session = session;
        this.charisma = charisma;
        spellbookAction =
                () ->
                        AppStyle.openWindow(
                                this,
                                "Spell Library",
                                new SpellLibraryPanel(
                                        session,
                                        () ->
                                                new CharacterState(
                                                        session.selection().level(),
                                                        charisma.getAsInt(),
                                                        session.selection().subclass(),
                                                        session.selection().chosenSpells(),
                                                        session.selection().species(),
                                                        session.snapshot())));
        setOpaque(true);
        setBackground(AppStyle.BACKGROUND);
        setBorder(new EmptyBorder(14, 14, 14, 14));
        JPanel columns = new CombatColumns();
        Column right = new Column();
        classActions =
                new dndcompanion.ui.character.ClassActionsPanel(
                        session, charisma, this::recordEvent);
        spellList = arsenalCard();
        spellList.setMinimumSize(new Dimension(350, 200));
        right.addCard(dossier);
        metamagic = new CombatMetamagic(session, this::preview);
        right.addCard(metamagic);
        diceTray =
                new CombatDiceTray(
                        this::selectedSpell,
                        this::effectiveLevel,
                        () -> session.selection().level(),
                        this::damageProfile,
                        this::rollDie,
                        () -> rollDamage(false),
                        () -> rollDamage(true));
        damageButton = diceTray.damageButton;
        criticalButton = diceTray.criticalButton;
        diceTray.setAlignmentX(LEFT_ALIGNMENT);
        right.addCard(diceTray);
        featActions =
                new dndcompanion.ui.character.FeatActionsPanel(
                        session, charisma, this::recordEvent);
        right.addCard(featActions);
        equipmentActions =
                new dndcompanion.ui.character.FeatCombatPanel(session, charisma, this::recordEvent);
        right.addCard(equipmentActions);
        right.addCard(combatLog);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = .55;
        c.insets = new Insets(0, 0, 0, 6);
        if (session.sorcerer()) columns.add(spellList, c);
        else {
            Column left = new Column();
            left.addCard(classActions);
            left.addCard(spellList);
            columns.add(columnScroll(left), c);
        }
        c.gridx = 1;
        c.weightx = .45;
        c.insets = new Insets(0, 6, 0, 0);
        columns.add(columnScroll(right), c);
        add(columns, BorderLayout.CENTER);
        add(castControls, BorderLayout.SOUTH);
        castButton.putClientProperty("tactileRole", "red");
        castControls.source.addActionListener(
                event -> {
                    if (!updating) preview();
                });
        castControls.damageType.addActionListener(
                event -> {
                    if (!updating) preview();
                });
        session.addListener(listener);
        session.selection().addChangeListener(listener);
        installShortcuts();
        refresh();
    }

    private JPanel arsenalCard() {
        JPanel card = AppStyle.card(new BorderLayout(0, 12));
        card.setName("spell-arsenal-card");
        card.putClientProperty("surfaceRole", "background");
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(arsenal);
        AppStyle.scrollPane(scroll);
        scroll.getViewport().putClientProperty("surfaceRole", "background");
        scroll.setPreferredSize(new Dimension(400, 235));
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    public boolean isDiceTrayVisible() {
        return diceTray.isVisible();
    }

    public List<Integer> visibleDiceSides() {
        return diceTray.visibleDiceSides();
    }

    public SpellCatalog.Spell selectedSpell() {
        return selected;
    }

    public boolean selectSpell(String name) {
        for (SpellCatalog.Spell spell : available) {
            if (spell.name().equals(name)) {
                selected = spell;
                refreshSlots();
                return true;
            }
        }
        return false;
    }

    public void setSpellbookAction(Runnable action) {
        spellbookAction = Objects.requireNonNull(action);
    }

    public int visibleSpellCount() {
        return visible.size();
    }

    public String combatLogText() {
        return combatLog.text();
    }

    public void refresh() {
        if (updating || disposed) {
            return;
        }
        updating = true;
        SpellCatalog.Spell previous = selectedSpell();
        List<SpellCatalog.Spell> next =
                session.selection().catalog().stream()
                        .filter(session.selection()::selected)
                        .filter(session.selection()::available)
                        .toList();
        if (!available.equals(next)) {
            available = next;
            selected =
                    available.contains(previous)
                            ? previous
                            : available.isEmpty() ? null : available.getFirst();
        }
        filters.refresh(available);
        featActions.refresh();
        classActions.refresh();
        spellList.setVisible(session.sorcerer() || !available.isEmpty());
        equipmentActions.refresh();
        updating = false;
        refreshSlots();
    }

    private void refreshSlots() {
        if (updating) {
            return;
        }
        SpellCatalog.Spell spell = selectedSpell();
        castControls.healing.setVisible(
                dndcompanion.domain.spells.HealingDice.profile(
                                spell, slotLevel(), session.selection().level())
                        .isPresent());
        if (spell == null || spell.level() == 0) {
            castingLevel = 0;
        } else if (castingLevel < spell.level()
                || castingLevel > 9
                || session.maxSlots(castingLevel) == 0) {
            castingLevel = spell.level();
        }
        rebuildArsenal();
        preview();
        AppStyle.applyAppearance(this);
    }

    private void rebuildArsenal() {
        List<SpellCatalog.Spell> next =
                available.stream()
                        .filter(
                                s ->
                                        filters.selectedLevels.isEmpty()
                                                || filters.selectedLevels.contains(s.level()))
                        .toList();
        boolean changed = !visible.equals(next) || arsenal.getComponentCount() == 0;
        visible = next;
        if (!changed && !visible.isEmpty()) {
            spellCards.values().forEach(CombatSpellCard::refreshCard);
            arsenal.revalidate();
            arsenal.repaint();
            return;
        }
        arsenal.removeAll();
        for (SpellCatalog.Spell spell : visible) {
            CombatSpellCard card =
                    spellCards.computeIfAbsent(
                            spell.name(),
                            name ->
                                    new CombatSpellCard(
                                            spell,
                                            this::selectedSpell,
                                            this::compatibleChosen,
                                            this::selectSpell));
            card.refreshCard();
            arsenal.add(card);
        }
        if (visible.isEmpty()) {
            JPanel empty = AppStyle.section(new BorderLayout(0, 8));
            JTextArea text =
                    AppStyle.text(
                            available.isEmpty()
                                    ? session.sorcerer()
                                            ? "No available spells. Choose your prepared spells and"
                                                    + " cantrips to fill the arsenal."
                                            : "Champion has no class spellcasting. Spells granted"
                                                    + " by species or feats appear here."
                                    : "No spells match this filter.");
            text.setRows(2);
            empty.add(text, BorderLayout.CENTER);
            empty.add(
                    AppStyle.button(
                            available.isEmpty() ? "Choose spells" : "Show all spells",
                            () -> {
                                if (available.isEmpty()) {
                                    spellbookAction.run();
                                } else {
                                    filters.selectedLevels.clear();
                                    filters.refresh(available);
                                    rebuildArsenal();
                                }
                            }),
                    BorderLayout.SOUTH);
            arsenal.add(empty);
        }
        arsenal.revalidate();
        arsenal.repaint();
    }

    private int slotLevel() {
        return castingLevel;
    }

    private int effectiveLevel() {
        return castingLevel;
    }

    private boolean sorcererSpell(SpellCatalog.Spell spell) {
        return SpellCatalog.alwaysPrepared(
                        spell, session.selection().level(), session.selection().subclass())
                || session.selection().chosenSpells().contains(spell.name())
                        && spell.sorcererSpell();
    }

    public List<Metamagic> compatibleChosen(SpellCatalog.Spell spell) {
        if (session.selection().level() < 2) {
            return List.of();
        }
        return Arrays.stream(Metamagic.values())
                .filter(session.metamagic()::contains)
                .filter(option -> option.applies(spell))
                .toList();
    }

    private String compatibleNames(SpellCatalog.Spell spell) {
        return String.join(
                ", ", compatibleChosen(spell).stream().map(Metamagic::toString).toList());
    }

    private SorcererRules.Stats stats() {
        return session.stats(
                session.effectiveScore(
                        dndcompanion.domain.character.CharacterSheet.Ability.CHA,
                        charisma.getAsInt()));
    }

    private int attackBonus() {
        return stats().proficiencyBonus()
                + (selectedSpell() == null
                        ? stats().charismaModifier()
                        : session.spellcastingModifier(
                                selectedSpell(),
                                charisma.getAsInt(),
                                (String) castControls.source.getSelectedItem()));
    }

    private int saveDc() {
        return 8
                + attackBonus()
                + (selectedSpell() != null
                                && session.innateActive()
                                && sorcererSpell(selectedSpell())
                        ? 1
                        : 0);
    }

    private void preview() {
        diceTray.refresh();
        SpellCatalog.Spell spell = selectedSpell();
        dossier.setVisible(spell != null || session.sorcerer());
        castControls.setVisible(spell != null || session.sorcerer());
        if (spell == null) {
            dossier.entry(null);
            dossier.compatibility.setText("");
            dossier.specifications.setVisible(false);
            metamagic.refresh(null, List.of());
            dossierTitle.setText("Spell dossier");
            dossierSubtitle.setText("Your chosen spell appears here");
            preview.setText("Open the Spell Library and learn spells to get started.");
            cost.setText("No spell selected");
            castButton.setEnabled(false);
            damageButton.setEnabled(false);
            criticalButton.setEnabled(false);
            return;
        }
        SpellDetails d = SpellDetails.forSpell(spell);
        dossier.entry(spell);
        metamagic.refresh(spell, compatibleChosen(spell));
        var baseDamage = DamageDice.profile(spell, effectiveLevel(), session.selection().level());
        boolean chosenDamage =
                baseDamage.isPresent() && baseDamage.get().type().equals("chosen type");
        castControls.damageType.setVisible(chosenDamage);
        if (chosenDamage) {
            List<String> types =
                    spell.name().equals("Sorcerous Burst")
                            ? List.of(
                                    "Acid",
                                    "Cold",
                                    "Fire",
                                    "Lightning",
                                    "Poison",
                                    "Psychic",
                                    "Thunder")
                            : spell.name().equals("Dragon's Breath")
                                    ? List.of("Acid", "Cold", "Fire", "Lightning", "Poison")
                                    : List.of(
                                            "Acid",
                                            "Cold",
                                            "Fire",
                                            "Lightning",
                                            "Poison",
                                            "Thunder");
            List<String> current =
                    java.util.stream.IntStream.range(0, castControls.damageType.getItemCount())
                            .mapToObj(castControls.damageType::getItemAt)
                            .toList();
            if (!types.equals(current)) {
                boolean wasUpdating = updating;
                updating = true;
                String previous = (String) castControls.damageType.getSelectedItem();
                castControls.damageType.removeAllItems();
                types.forEach(castControls.damageType::addItem);
                if (previous != null && types.contains(previous))
                    castControls.damageType.setSelectedItem(previous);
                updating = wasUpdating;
            }
            diceTray.refresh();
        }
        List<String> sources = session.castSources(spell);
        String previousSource = (String) castControls.source.getSelectedItem();
        List<String> currentSources =
                java.util.stream.IntStream.range(0, castControls.source.getItemCount())
                        .mapToObj(castControls.source::getItemAt)
                        .toList();
        if (!sources.equals(currentSources)) {
            boolean wasUpdating = updating;
            updating = true;
            castControls.source.removeAllItems();
            sources.forEach(castControls.source::addItem);
            if (previousSource != null && sources.contains(previousSource))
                castControls.source.setSelectedItem(previousSource);
            updating = wasUpdating;
        }
        boolean innate = session.innateActive() && sorcererSpell(spell);
        dossierTitle.setText(spell.name().toUpperCase(java.util.Locale.ROOT));
        dossierSubtitle.setText(
                spell.levelName()
                        + " · "
                        + d.school()
                        + " · "
                        + (session.selection().speciesSpell(spell)
                                ? "Species magic"
                                : "Sorcerer magic"));
        dossierSubtitle.setToolTipText(dossierSubtitle.getText());
        String slotCost =
                spell.level() == 0
                        ? "No slot"
                        : "Level "
                                + slotLevel()
                                + " slot ("
                                + session.remainingSlots(slotLevel())
                                + " left)";
        String source = (String) castControls.source.getSelectedItem();
        castControls.source.setToolTipText(PlaySession.castSourceLabel(source));
        boolean free = source != null && !source.equals("Spell slot") && !source.equals("Cantrip");
        cost.setText(
                (free
                                ? PlaySession.castSourceLabel(source)
                                        + " · "
                                        + (source.startsWith("Ritual (")
                                                ? "unlimited"
                                                : session.freeCasts(spell, source) + " left")
                                : slotCost)
                        + " + "
                        + session.metamagicCost(metamagic.selected())
                        + " SP · "
                        + session.remainingPoints()
                        + " SP available");
        String enhancements =
                compatibleChosen(spell).isEmpty()
                        ? "No chosen Metamagic matches."
                        : "Compatible chosen Metamagic: "
                                + compatibleNames(spell)
                                + ". Select modifiers below.";
        String checks =
                (hasSave(spell) ? "Save DC " + saveDc() + savingAbilityLabel(spell) : "")
                        + (hasSave(spell) && hasAttack(spell) ? " · " : "")
                        + (hasAttack(spell)
                                ? "Attack "
                                        + String.format("%+d", attackBonus())
                                        + (innate ? " with Advantage" : "")
                                : "");
        if (checks.isBlank()) checks = "No target saving throw or caster attack roll.";
        dossier.specifications(
                d.castingTime(),
                session.spellRange(spell),
                d.duration(),
                session.spellComponents(spell, source),
                checks,
                d.concentration());
        String effect = damageSummary(spell, effectiveLevel(), session.selection().level());
        String[] opening =
                d.description()
                        .replaceAll("<[^>]*>", " ")
                        .replaceAll("[*_]", "")
                        .split("(?<=\\.)\\s+", 3);
        String summary =
                String.join(" ", java.util.Arrays.copyOf(opening, Math.min(2, opening.length)));
        String mechanics =
                effect.equals("No direct damage. Apply the spell's described effects.")
                        ? "No direct damage. " + summary
                        : effect;
        boolean same = Objects.equals(preview.getClientProperty("spellName"), spell.name());
        TextUpdates.set(preview, mechanics, same);
        TextUpdates.set(dossier.compatibility, enhancements, same);
        preview.putClientProperty("spellName", spell.name());
        var formula = damageProfile();
        damageButton.setEnabled(formula.isPresent());
        criticalButton.setEnabled(formula.isPresent() && hasAttack(spell));
        criticalButton.setToolTipText(
                "Double the dice for an attack that scored a critical hit; modifiers are added"
                        + " once.");
        castButton.setEnabled(true);
        castButton.setToolTipText(
                "Cast "
                        + spell.name()
                        + " · "
                        + slotCost
                        + ". Enter when the workspace has focus.");
    }

    private Optional<DamageDice.Formula> damageProfile() {
        SpellCatalog.Spell spell = selectedSpell();
        return spell == null
                ? Optional.empty()
                : DamageDice.profile(spell, effectiveLevel(), session.selection().level())
                        .map(
                                formula ->
                                        formula.type().equals("chosen type")
                                                        && castControls.damageType.getSelectedItem()
                                                                != null
                                                ? new DamageDice.Formula(
                                                        formula.count(),
                                                        formula.sides(),
                                                        formula.modifier(),
                                                        (String)
                                                                castControls.damageType
                                                                        .getSelectedItem(),
                                                        formula.exploding(),
                                                        formula.scope())
                                                : formula);
    }

    private static String savingAbilityLabel(SpellCatalog.Spell spell) {
        String label = dndcompanion.domain.spells.SpellProfile.forSpell(spell).saveLabel();
        return label.isEmpty() ? "" : " " + label;
    }

    public static String damageSummary(
            SpellCatalog.Spell spell, int effective, int characterLevel) {
        if (spell.name().equals("True Strike")) {
            int extra =
                    (characterLevel >= 5 ? 1 : 0)
                            + (characterLevel >= 11 ? 1 : 0)
                            + (characterLevel >= 17 ? 1 : 0);
            return "Damage: weapon damage + spellcasting ability modifier"
                    + (extra > 0 ? " + " + extra + "d6 Radiant" : "")
                    + ". The spell replaces the weapon's normal ability modifier; choose Radiant or"
                    + " the weapon's normal type for its base damage.";
        }
        return DamageDice.profile(spell, effective, characterLevel)
                .map(
                        f ->
                                "Damage roll: "
                                        + f.notation()
                                        + ". Follow the spell text for additional effects.")
                .orElseGet(() -> SpellMechanics.damageNote(spell));
    }

    private void cast() {
        SpellCatalog.Spell spell = selectedSpell();
        if (spell == null) return;
        if (SpellDetails.forSpell(spell).concentration()
                && !session.concentration().isBlank()
                && JOptionPane.showConfirmDialog(
                                this,
                                "Replace concentration on " + session.concentration() + "?",
                                "Concentration",
                                JOptionPane.YES_NO_OPTION)
                        != JOptionPane.YES_OPTION) return;
        int selectedLevel = slotLevel();
        String source = (String) castControls.source.getSelectedItem();
        String error = session.castFromSource(spell, selectedLevel, source, metamagic.selected());
        result.setText(error.isEmpty() ? "Cast " + spell.name() + ". Resources updated." : error);
        if (error.isEmpty())
            recordEvent(
                    "Cast "
                            + spell.name()
                            + " · "
                            + (spell.level() == 0
                                    ? "cantrip"
                                    : source.equals("Spell slot")
                                            ? "level " + selectedLevel + " slot"
                                            : PlaySession.castSourceLabel(source))
                            + (session.lastCastDetails().isBlank()
                                    ? ""
                                    : " · " + session.lastCastDetails()));
        else recordEvent("Cast blocked: " + error);
    }

    private static boolean hasAttack(SpellCatalog.Spell spell) {
        return SpellMechanics.hasAttack(spell);
    }

    private static boolean hasSave(SpellCatalog.Spell spell) {
        return SpellMechanics.forcesSave(spell);
    }

    private void rollAttack() {
        SpellCatalog.Spell spell = selectedSpell();
        if (spell == null || !hasAttack(spell)) return;
        boolean advantage = session.innateActive() && sorcererSpell(spell);
        int first = random.nextInt(1, 21),
                second = advantage ? random.nextInt(1, 21) : first,
                die = advantage ? Math.max(first, second) : first;
        int total = die + attackBonus();
        String outcome = die == 20 ? " · Critical hit" : die == 1 ? " · Automatic miss" : "";
        recordEvent(
                spell.name()
                        + " attack · "
                        + (advantage ? "Advantage" : "Normal")
                        + " ["
                        + first
                        + (advantage ? ", " + second : "")
                        + "] "
                        + String.format("%+d", attackBonus())
                        + " = "
                        + total
                        + outcome);
    }

    private void rollDamage(boolean critical) {
        var profile = damageProfile();
        if (profile.isEmpty()) {
            result.setText("Resolve this spell's damage using its full text.");
            return;
        }
        DamageDice.Formula f = profile.get();
        boolean adept =
                session.feats().containsKey("Elemental Adept")
                        && dndcompanion.domain.character.FeatEffects.elementalTypes(session.sheet())
                                .stream()
                                .anyMatch(type -> type.equalsIgnoreCase(f.type()));
        DamageDice.Roll rolled =
                f.roll(
                        random,
                        critical,
                        Math.max(
                                0,
                                session.spellcastingModifier(selectedSpell(), charisma.getAsInt())),
                        adept ? 2 : 1);
        String formula =
                critical
                        ? f.count() * 2
                                + "d"
                                + f.sides()
                                + (f.modifier() == 0 ? "" : " + " + f.modifier())
                                + (f.type().isBlank() ? "" : " " + f.type())
                                + (f.scope().isBlank() ? "" : " · " + f.scope())
                        : f.notation();
        recordEvent(
                selectedSpell().name()
                        + (critical ? " critical damage" : " damage")
                        + " · "
                        + formula
                        + " "
                        + rolled.dice()
                        + " = "
                        + rolled.total()
                        + (adept ? " · Elemental Adept: 1s treated as 2; ignores resistance" : "")
                        + (f.type().equalsIgnoreCase("Poison")
                                        && session.feats().containsKey("Poisoner")
                                ? " · ignores Poison resistance"
                                : ""));
    }

    private void rollHealing() {
        var spell = selectedSpell();
        var formula =
                dndcompanion.domain.spells.HealingDice.profile(
                        spell, effectiveLevel(), session.selection().level());
        if (formula.isEmpty()) return;
        int modifier =
                session.spellcastingModifier(
                        spell, charisma.getAsInt(), (String) castControls.source.getSelectedItem());
        var rolled =
                dndcompanion.domain.spells.HealingDice.roll(
                        formula.get(), modifier, session.feats().containsKey("Healer"), random);
        recordEvent(
                spell.name()
                        + " healing · "
                        + rolled.dice()
                        + " "
                        + String.format("%+d", modifier)
                        + " = "
                        + rolled.total()
                        + " · "
                        + rolled.notes()
                        + "; apply to the chosen target after casting.");
    }

    public static boolean saveSucceeds(int roll, int modifier, int dc) {
        return roll + modifier >= dc;
    }

    private void rollDie(int sides) {
        SpellCatalog.Spell spell = selectedSpell();
        if (spell == null) return;
        if (sides == 20 && hasAttack(spell)) rollAttack();
        else if (dndcompanion.domain.spells.HealingDice.profile(
                        spell, effectiveLevel(), session.selection().level())
                .isPresent()) rollHealing();
        else
            recordEvent(spell.name() + " · d" + sides + ": [" + random.nextInt(1, sides + 1) + "]");
    }

    public void recordEvent(String event) {
        combatLog.record(event);
    }

    public void clearLog() {
        combatLog.clear();
    }

    private void installShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "castSpell");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "castSpell");
        getActionMap()
                .put(
                        "castSpell",
                        new AbstractAction() {
                            @Override
                            public void actionPerformed(ActionEvent e) {
                                if (shortcutAllowed()) {
                                    cast();
                                }
                            }
                        });
    }

    private boolean shortcutAllowed() {
        Component focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
        return isShowing()
                && !(focus instanceof JTextComponent
                        || focus instanceof AbstractButton
                        || focus instanceof JComboBox<?>
                        || focus instanceof JSpinner
                        || focus instanceof JSlider);
    }

    @Override
    public void dispose() {
        if (disposed) return;
        disposed = true;
        session.removeListener(listener);
        session.selection().removeChangeListener(listener);
    }

    private static JScrollPane columnScroll(Column column) {
        JScrollPane scroll = new JScrollPane(column);
        AppStyle.scrollPane(scroll);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setMinimumSize(new Dimension(350, 200));
        scroll.setPreferredSize(new Dimension(400, 600));
        return scroll;
    }

    /** Stack the arsenal and dossier when the sidebar/text size leaves too little column width. */
    private static final class CombatColumns extends JPanel {
        private Boolean stacked;

        CombatColumns() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        @Override
        public void doLayout() {
            boolean narrow = getWidth() < 900;
            if (getComponentCount() == 2 && (stacked == null || stacked != narrow)) {
                stacked = narrow;
                GridBagLayout layout = (GridBagLayout) getLayout();
                for (int index = 0; index < 2; index++) {
                    Component child = getComponent(index);
                    GridBagConstraints constraints = layout.getConstraints(child);
                    constraints.gridx = narrow ? 0 : index;
                    constraints.gridy = narrow ? index : 0;
                    constraints.weightx = narrow ? 1 : index == 0 ? .55 : .45;
                    constraints.weighty = narrow ? index == 0 ? .35 : .65 : 1;
                    constraints.insets =
                            narrow
                                    ? new Insets(index == 0 ? 0 : 12, 0, 0, 0)
                                    : new Insets(0, index == 0 ? 0 : 6, 0, index == 0 ? 6 : 0);
                    layout.setConstraints(child, constraints);
                }
            }
            super.doLayout();
        }
    }

    private static final class Column extends JPanel implements Scrollable {
        Column() {
            setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));
            setOpaque(false);
        }

        void addCard(JPanel panel) {
            int gap = getComponentCount() > 0 ? 12 : 0;
            JPanel wrapper =
                    new JPanel(new BorderLayout()) {
                        @Override
                        public Dimension getPreferredSize() {
                            return panel.isVisible()
                                    ? super.getPreferredSize()
                                    : new Dimension(0, 0);
                        }

                        @Override
                        public Dimension getMaximumSize() {
                            return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
                        }
                    };
            wrapper.setOpaque(false);
            wrapper.setBorder(new EmptyBorder(gap, 0, 0, 0));
            wrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
            wrapper.add(panel);
            add(wrapper);
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 28;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return visible.height - 28;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }
}
