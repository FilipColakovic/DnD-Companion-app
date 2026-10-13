package org.example.ui.spells;

import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.spells.DamageDice;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellMechanics;
import org.example.ui.common.AppStyle;
import org.example.ui.common.LiveView;

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
import java.util.Set;
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
    private final CombatTierFilters filters = new CombatTierFilters(this::rebuildArsenal);
    private final CombatDossier dossier = new CombatDossier();
    private final CombatLog combatLog = new CombatLog();
    private final CastControls castControls = new CastControls(this::cast);
    private final JTextArea preview = dossier.preview;
    private final JLabel dossierTitle = dossier.dossierTitle,
            dossierSubtitle = dossier.dossierSubtitle,
            cost = castControls.cost;
    private final JTextArea result = castControls.result;
    private final JButton castButton = castControls.castButton;
    private final CombatDiceTray diceTray;
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
                                                new org.example.domain.character.CharacterState(
                                                        session.selection().level(),
                                                        charisma.getAsInt(),
                                                        session.selection().subclass(),
                                                        session.selection().chosenSpells(),
                                                        session.selection().species(),
                                                        session.snapshot())));
        setOpaque(true);
        setBackground(AppStyle.BACKGROUND);
        setBorder(new EmptyBorder(14, 14, 14, 14));
        JPanel columns = AppStyle.section(new GridBagLayout());
        Column right = new Column();
        JPanel spellList = arsenalCard();
        spellList.setMinimumSize(new Dimension(350, 200));
        right.addCard(dossier);
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
        right.addCard(combatLog);
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = 0;
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1;
        c.gridx = 0;
        c.weightx = .55;
        c.insets = new Insets(0, 0, 0, 6);
        columns.add(spellList, c);
        c.gridx = 1;
        c.weightx = .45;
        c.insets = new Insets(0, 6, 0, 0);
        columns.add(columnScroll(right), c);
        add(columns, BorderLayout.CENTER);
        add(castControls, BorderLayout.SOUTH);
        castButton.putClientProperty("tactileRole", "red");
        session.addListener(listener);
        session.selection().addChangeListener(listener);
        installShortcuts();
        refresh();
    }

    private JPanel arsenalCard() {
        JPanel card = AppStyle.card(new BorderLayout(0, 12));
        card.add(filters, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(arsenal);
        AppStyle.scrollPane(scroll);
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
        updating = false;
        refreshSlots();
    }

    private void refreshSlots() {
        if (updating) {
            return;
        }
        SpellCatalog.Spell spell = selectedSpell();
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
                                    ? "No available spells. Choose your prepared spells and"
                                            + " cantrips to fill the arsenal."
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
        return SorcererRules.calculate(
                session.selection().level(), charisma.getAsInt(), session.selection().subclass());
    }

    private int attackBonus() {
        return stats().proficiencyBonus()
                + (selectedSpell() == null
                        ? stats().charismaModifier()
                        : session.spellcastingModifier(selectedSpell(), charisma.getAsInt()));
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
        if (spell == null) {
            dossierTitle.setText("Spell dossier");
            dossierSubtitle.setText("Your chosen spell appears here");
            preview.setText(
                    "Open the Spellbook and choose cantrips and prepared spells to get started.");
            cost.setText("No spell selected");
            castButton.setEnabled(false);
            damageButton.setEnabled(false);
            criticalButton.setEnabled(false);
            return;
        }
        SpellDetails d = SpellDetails.forSpell(spell);
        boolean innate = session.innateActive() && sorcererSpell(spell);
        dossierTitle.setText(spell.name());
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
        cost.setText(slotCost + " + 0 SP · " + session.remainingPoints() + " SP available");
        String enhancements =
                compatibleChosen(spell).isEmpty()
                        ? "No chosen Metamagic matches."
                        : "Compatible chosen Metamagic: "
                                + compatibleNames(spell)
                                + ". View these options in the Metamagic.";
        String checks =
                (hasSave(spell) ? "Save DC " + saveDc() : "")
                        + (hasSave(spell) && hasAttack(spell) ? " · " : "")
                        + (hasAttack(spell)
                                ? "Attack "
                                        + String.format("%+d", attackBonus())
                                        + (innate ? " with Advantage" : "")
                                : "");
        if (checks.isBlank()) checks = "No target saving throw or caster attack roll.";
        String mechanics =
                "Casting: "
                        + d.castingTime()
                        + "\nRange: "
                        + d.range()
                        + "\nDuration: "
                        + d.duration()
                        + "\nComponents: "
                        + d.components()
                        + "\n"
                        + checks
                        + "\nConcentration: "
                        + (d.concentration() ? "required" : "not required")
                        + (d.ritual() ? " · Ritual" : "")
                        + "\n\n"
                        + damageSummary(spell, effectiveLevel(), session.selection().level())
                        + "\n\n"
                        + enhancements
                        + "\n\nSPELL MECHANICS\n"
                        + d.description().replaceAll("<[^>]*>", " ")
                        + "\n\nHIGHER LEVELS\n"
                        + (d.higherLevels().isBlank()
                                ? "No additional scaling listed."
                                : d.higherLevels());
        boolean same = Objects.equals(preview.getClientProperty("spellName"), spell.name());
        org.example.ui.common.TextUpdates.set(preview, mechanics, same);
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
                : DamageDice.profile(spell, effectiveLevel(), session.selection().level());
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
        String error = session.cast(spell, selectedLevel, false, Set.of());
        result.setText(error.isEmpty() ? "Cast " + spell.name() + ". Resources updated." : error);
        if (error.isEmpty())
            recordEvent(
                    "Cast "
                            + spell.name()
                            + " · "
                            + (spell.level() == 0
                                    ? "cantrip"
                                    : "level " + selectedLevel + " slot"));
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
        DamageDice.Roll rolled =
                f.roll(
                        random,
                        critical,
                        Math.max(
                                0,
                                session.spellcastingModifier(
                                        selectedSpell(), charisma.getAsInt())));
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
                        + rolled.total());
    }

    public static boolean saveSucceeds(int roll, int modifier, int dc) {
        return roll + modifier >= dc;
    }

    private void rollDie(int sides) {
        SpellCatalog.Spell spell = selectedSpell();
        if (spell == null) return;
        if (sides == 20 && hasAttack(spell)) rollAttack();
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
        return scroll;
    }

    private static final class Column extends JPanel implements Scrollable {
        Column() {
            super(new GridBagLayout());
            setOpaque(false);
        }

        void addCard(JPanel panel) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = 0;
            c.gridy = getComponentCount();
            c.weightx = 1;
            c.weighty = c.gridy == 0 ? 1 : 0;
            c.fill = GridBagConstraints.BOTH;
            c.insets = new Insets(c.gridy == 0 ? 0 : 12, 0, 0, 0);
            add(panel, c);
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
            return getParent() != null && getParent().getHeight() >= getPreferredSize().height;
        }
    }
}
