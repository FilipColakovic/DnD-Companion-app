package dndcompanion.ui.character;

import static dndcompanion.ui.character.CharacterSheetComponents.card;
import static dndcompanion.ui.character.CharacterSheetComponents.labelled;
import static dndcompanion.ui.character.CharacterSheetComponents.metric;
import static dndcompanion.ui.character.CharacterSheetComponents.signed;

import dndcompanion.application.CharacterController;
import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.CharacterSheet.Ability;
import dndcompanion.domain.character.CharacterSheet.Field;
import dndcompanion.domain.character.CharacterState;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.ui.GrimoirePanel;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;

final class CharacterVitalsPanel extends JPanel {
    private final PlaySession session;
    private final CharacterController editor;
    private final GrimoirePanel workspace;
    private final JLabel armor = AppStyle.label("", 22, true),
            initiative = AppStyle.label("", 22, true),
            proficiency = AppStyle.label("", 22, true),
            hpLabel = AppStyle.label("", 14, true),
            hitDice = AppStyle.label("", 12, true);
    private final JProgressBar hpBar = new JProgressBar();
    private final JCheckBox inspiration = new JCheckBox("Heroic Inspiration"),
            automaticHp = new JCheckBox("Calculate maximum HP from class");
    private final JTextArea hpCalculation = FeatComponents.text("", 11);
    private final JSpinner hitPoints, maxHitPoints;
    private final JButton spendHitDie;
    private final Consumer<String> status;

    CharacterVitalsPanel(CharacterFormBindings bindings, Consumer<String> status) {
        super(new BorderLayout());
        setOpaque(false);
        session = bindings.session;
        editor = bindings.editor;
        workspace = bindings.workspace;
        this.status = status;
        JPanel vitals = card("COMBAT VITALS");
        JPanel metrics = new JPanel(new GridLayout(1, 3, 6, 0));
        metrics.setOpaque(false);
        metrics.add(metric("ARMOR CLASS", armor));
        metrics.add(metric("INITIATIVE", initiative));
        metrics.add(metric("PROF BONUS", proficiency));
        FeatComponents.row(vitals, metrics);
        JPanel basics = new FeatComponents.WrapPanel(7);
        basics.add(
                labelled(
                        "AC OVERRIDE (0 = UNARMORED)",
                        bindings.field(Field.AC_OVERRIDE, "Armor class override")));
        basics.add(labelled("SPEED (FEET)", bindings.field(Field.SPEED, "Speed")));
        FeatComponents.row(vitals, basics);
        JPanel hp = AppStyle.section(new BorderLayout(0, 5));
        hp.add(hpLabel, BorderLayout.NORTH);
        hpBar.setStringPainted(true);
        hpBar.setForeground(AppStyle.ERROR);
        hpBar.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
        hp.add(hpBar, BorderLayout.CENTER);
        JPanel hpInputs = new FeatComponents.WrapPanel(6);
        hitPoints =
                bindings.spinner("Current hit points", 0, 0, 9999, session::setCurrentHitPoints);
        maxHitPoints =
                bindings.spinner(
                        "Maximum hit points",
                        0,
                        0,
                        9999,
                        value -> session.setHitPoints(Math.min(value, session.hitPoints()), value));
        hpInputs.add(labelled("CURRENT HP", hitPoints));
        hpInputs.add(labelled("MAX HP", maxHitPoints));
        hpInputs.add(labelled("TEMP HP", bindings.field(Field.TEMP_HP, "Temporary hit points")));
        hp.add(hpInputs, BorderLayout.SOUTH);
        FeatComponents.row(vitals, FeatComponents.block("HIT POINT POOL", hp));
        automaticHp.getAccessibleContext().setAccessibleName("Automatic maximum HP");
        AppStyle.roundButton(automaticHp);
        automaticHp.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        automaticHp.addActionListener(
                event -> {
                    if (!bindings.updating.getAsBoolean())
                        session.setAutomaticHp(automaticHp.isSelected());
                });
        FeatComponents.row(vitals, automaticHp);
        FeatComponents.row(vitals, hpCalculation);
        JPanel dice = new FeatComponents.WrapPanel(6);
        dice.add(hitDice);
        dice.add(labelled("SPENT", bindings.field(Field.SPENT_HIT_DICE, "Spent hit dice")));
        spendHitDie = AppStyle.compactButton("Spend d6 Hit Die", this::spendHitDie);
        dice.add(spendHitDie);
        FeatComponents.row(vitals, dice);
        spendHitDie.setToolTipText(
                "During a Short Rest, spend one available Sorcerer Hit Die and heal by d6 + CON"
                        + " modifier");
        JPanel death = new FeatComponents.WrapPanel(6);
        death.add(
                labelled(
                        "DEATH SAVE SUCCESSES",
                        bindings.deathMarks(Field.DEATH_SUCCESSES, "Success")));
        death.add(
                labelled(
                        "DEATH SAVE FAILURES",
                        bindings.deathMarks(Field.DEATH_FAILURES, "Failure")));
        FeatComponents.row(vitals, death);
        AppStyle.roundButton(inspiration);
        inspiration.addActionListener(
                event -> {
                    if (!bindings.updating.getAsBoolean())
                        session.setSheet(
                                session.sheet()
                                        .withField(
                                                Field.INSPIRATION,
                                                inspiration.isSelected() ? 1 : 0));
                });
        FeatComponents.row(vitals, inspiration);
        add(vitals);
    }

    void refresh(
            CharacterState state,
            CharacterSheet sheet,
            SorcererRules.Stats stats,
            boolean restored) {
        ((SpinnerNumberModel) hitPoints.getModel()).setMaximum(session.maxHitPoints());
        hitPoints.setValue(session.hitPoints());
        maxHitPoints.setValue(session.maxHitPoints());
        automaticHp.setSelected(session.automaticHp());
        maxHitPoints.setEnabled(!session.automaticHp());
        maxHitPoints.setToolTipText(
                session.automaticHp()
                        ? "Calculated from level, Constitution and HP bonuses"
                        : "Manual maximum for rolled HP or a table override");
        hpCalculation.setText(
                (session.automaticHp() ? "Calculated: " : "Fixed-value reference: ")
                        + session.hpCalculation().explanation()
                        + " = "
                        + session.hpCalculation().maximum()
                        + " HP");
        inspiration.setSelected(sheet.field(Field.INSPIRATION) == 1);
        hpLabel.setText("HIT POINTS " + session.hitPoints() + " / " + session.maxHitPoints());
        hpBar.setMaximum(Math.max(1, session.maxHitPoints()));
        hpBar.setValue(session.hitPoints());
        hpBar.setString(
                session.maxHitPoints() == 0
                        ? "Set maximum HP"
                        : session.hitPoints() + " / " + session.maxHitPoints());
        armor.setText(Integer.toString(session.armorClass(state.charisma())));
        armor.setToolTipText(
                "Unarmored: 10 + DEX modifier unless AC override is set. Mage Armor: "
                        + (13
                                + CharacterSheet.modifier(
                                        session.effectiveScore(Ability.DEX, state.charisma()))));
        initiative.setText(
                signed(
                        CharacterSheet.modifier(
                                        session.effectiveScore(Ability.DEX, state.charisma()))
                                + (session.feats().containsKey("Alert")
                                        ? stats.proficiencyBonus()
                                        : 0)));
        proficiency.setText(signed(stats.proficiencyBonus()));
        int remaining = Math.max(0, state.level() - sheet.field(Field.SPENT_HIT_DICE));
        hitDice.setText("HIT DICE " + remaining + " / " + state.level() + " (d6)");
        spendHitDie.setEnabled(
                remaining > 0
                        && session.hitPoints() > 0
                        && session.hitPoints() < session.maxHitPoints());
    }

    private void spendHitDie() {
        CharacterState state = editor.snapshot();
        CharacterSheet sheet = session.sheet();
        int spent = sheet.field(Field.SPENT_HIT_DICE);
        if (spent >= state.level() || session.hitPoints() == 0) return;
        int die = java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 7),
                healing = session.effectiveSheet().hitDieHealing(die);
        session.setSheet(sheet.withField(Field.SPENT_HIT_DICE, spent + 1));
        session.setCurrentHitPoints(
                Math.min(session.maxHitPoints(), session.hitPoints() + healing));
        workspace
                .combatPanel()
                .recordEvent(
                        "Short Rest Hit Die: d6 ["
                                + die
                                + "] + CON = "
                                + healing
                                + " HP (limited by maximum HP)");
    }
}
