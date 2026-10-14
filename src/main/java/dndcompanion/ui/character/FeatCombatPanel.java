package dndcompanion.ui.character;

import dndcompanion.domain.character.CharacterSheet;
import dndcompanion.domain.character.FeatCombatRules;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.ui.common.AppStyle;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

/** Compact controls for player-owned equipment, saves and incoming damage. */
public final class FeatCombatPanel extends JPanel {
    private final PlaySession session;
    private final IntSupplier charisma;
    private final Consumer<String> log;
    private final RandomGenerator random = RandomGenerator.getDefault();
    private final JCheckBox critical = new JCheckBox("Critical hit"),
            action = new JCheckBox("Attack action", true),
            twoHands = new JCheckBox("Two hands"),
            extra = new JCheckBox("Light extra attack"),
            thrown = new JCheckBox("Thrown attack"),
            unarmed = new JCheckBox("Unarmed"),
            attackDamage = new JCheckBox("From attack", true),
            grappled = new JCheckBox("Target grappled by me"),
            mounted = new JCheckBox("Qualifying mounted target"),
            dim = new JCheckBox("Dim light / darkness"),
            concentration = new JCheckBox("Concentration save");
    private final JComboBox<String> type =
            new JComboBox<>(
                    new String[] {
                        "Acid",
                        "Bludgeoning",
                        "Cold",
                        "Fire",
                        "Force",
                        "Lightning",
                        "Necrotic",
                        "Piercing",
                        "Poison",
                        "Psychic",
                        "Radiant",
                        "Slashing",
                        "Thunder"
                    });
    private final JSpinner damage = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
    private final JComboBox<String> save =
            new JComboBox<>(new String[] {"STR", "DEX", "CON", "INT", "WIS", "CHA", "Death"});

    public FeatCombatPanel(PlaySession session, IntSupplier charisma, Consumer<String> log) {
        super(new BorderLayout());
        this.session = session;
        this.charisma = charisma;
        this.log = log;
        JPanel body = AppStyle.card(new java.awt.GridBagLayout());
        FeatComponents.row(body, AppStyle.label("EQUIPMENT & FEAT ROLLS", 14, true));
        JPanel flags = AppStyle.section(new GridLayout(0, 2, 4, 2));
        for (JCheckBox box :
                List.of(critical, action, twoHands, extra, thrown, unarmed, grappled, mounted))
            flags.add(box);
        FeatComponents.row(body, flags);
        FeatComponents.row(
                body, AppStyle.compactButton("Roll weapon attack & damage", this::weapon));
        FeatComponents.row(
                body,
                AppStyle.compactButton(
                        "Roll initiative",
                        () -> log.accept(session.rollInitiative(charisma.getAsInt(), random))));
        JPanel saves = AppStyle.section(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        saves.add(save);
        saves.add(concentration);
        saves.add(AppStyle.compactButton("Roll save", this::savingThrow));
        FeatComponents.row(body, saves);
        JPanel incoming = AppStyle.section(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 4, 0));
        damage.getAccessibleContext().setAccessibleName("Incoming damage");
        type.getAccessibleContext().setAccessibleName("Incoming damage type");
        incoming.add(damage);
        incoming.add(type);
        incoming.add(attackDamage);
        incoming.add(dim);
        FeatComponents.row(body, incoming);
        FeatComponents.row(body, AppStyle.compactButton("Apply incoming damage", this::damage));
        body.setVisible(false);
        var toggle = AppStyle.compactButton("Show equipment & feat rolls", () -> {});
        toggle.addActionListener(
                event -> {
                    body.setVisible(!body.isVisible());
                    toggle.setText(
                            body.isVisible()
                                    ? "Hide equipment & feat rolls"
                                    : "Show equipment & feat rolls");
                    revalidate();
                    if (getParent() != null) getParent().revalidate();
                });
        add(toggle, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        setOpaque(false);
    }

    private void weapon() {
        var selectedWeapon = dndcompanion.domain.character.EquipmentRules.weapon(session.sheet());
        boolean unarmedAttack =
                unarmed.isSelected() || selectedWeapon.properties().contains("Unarmed");
        boolean usingTwoHands =
                !unarmedAttack
                        && (twoHands.isSelected()
                                || selectedWeapon.properties().contains("Two-Handed")
                                        && (!selectedWeapon.name().equals("Lance")
                                                || !mounted.isSelected()));
        if (usingTwoHands
                && (dndcompanion.domain.character.EquipmentRules.shield(session.sheet())
                        || session.sheet().choice("equipment.offhand").stream()
                                .anyMatch(name -> !name.equals("None")))) {
            log.accept("Weapon roll blocked: stow the shield before using two hands.");
            return;
        }
        int die = random.nextInt(1, 21);
        boolean advantage =
                session.feats().containsKey("Grappler") && grappled.isSelected()
                        || session.feats().containsKey("Mounted Combatant") && mounted.isSelected();
        int second = advantage ? random.nextInt(1, 21) : die;
        int attackDie = Math.max(die, second);
        boolean criticalHit = critical.isSelected() || attackDie == 20;
        var rolled =
                FeatCombatRules.weapon(
                        session,
                        charisma.getAsInt(),
                        new FeatCombatRules.Context(
                                criticalHit,
                                action.isSelected(),
                                usingTwoHands,
                                extra.isSelected(),
                                thrown.isSelected(),
                                unarmedAttack),
                        random);
        int overwhelming =
                attackDie == 20
                                && session.feats().containsKey("Boon of Irresistible Offense")
                                && session.sheet()
                                                .choice("feat.Boon of Irresistible Offense.ability")
                                                .size()
                                        == 1
                        ? session.effectiveScore(
                                dndcompanion.domain.character.FeatEffects.ability(
                                        session.sheet(), "Boon of Irresistible Offense"),
                                charisma.getAsInt())
                        : 0;
        log.accept(
                "Weapon attack: d20 ["
                        + die
                        + (advantage ? ", " + second + " · Advantage" : "")
                        + "] "
                        + String.format("%+d", rolled.attackBonus())
                        + " = "
                        + (Math.max(die, second) + rolled.attackBonus())
                        + "; damage "
                        + rolled.dice()
                        + " "
                        + String.format("%+d", rolled.modifier())
                        + " = "
                        + (rolled.total() + overwhelming)
                        + (overwhelming > 0
                                ? " (includes +" + overwhelming + " Overwhelming Strike)"
                                : "")
                        + (criticalHit ? " (confirmed critical)" : " (apply only on a hit)")
                        + (attackDie == 1 ? " · Natural 1: automatic miss" : "")
                        + "; "
                        + rolled.notes());
    }

    private void savingThrow() {
        String selected = (String) save.getSelectedItem();
        boolean death = selected.equals("Death");
        var ability = death ? CharacterSheet.Ability.CON : CharacterSheet.Ability.valueOf(selected);
        boolean advantage =
                death
                        ? session.feats().containsKey("Durable")
                        : concentration.isSelected()
                                && ability == CharacterSheet.Ability.CON
                                && session.feats().containsKey("War Caster");
        int first = random.nextInt(1, 21), second = advantage ? random.nextInt(1, 21) : first;
        int bonus =
                death
                        ? 0
                        : CharacterSheet.modifier(
                                        session.effectiveScore(ability, charisma.getAsInt()))
                                + (session.saveProficiency(ability)
                                        ? 2 + (session.selection().level() - 1) / 4
                                        : 0);
        log.accept(
                selected
                        + " save: "
                        + (advantage ? "Advantage " : "")
                        + "["
                        + first
                        + (advantage ? ", " + second : "")
                        + "] "
                        + String.format("%+d", bonus)
                        + " = "
                        + (Math.max(first, second) + bonus));
    }

    private void damage() {
        int original = (Integer) damage.getValue();
        int actual =
                FeatCombatRules.incoming(
                        session,
                        (String) type.getSelectedItem(),
                        original,
                        attackDamage.isSelected(),
                        dim.isSelected());
        int temp = session.sheet().field(CharacterSheet.Field.TEMP_HP);
        int absorbed = Math.min(temp, actual);
        // Apply both temp HP and wounds as a single completed operation.
        session.takeDamage(actual);
        log.accept(
                "Incoming "
                        + type.getSelectedItem()
                        + ": "
                        + original
                        + " → "
                        + actual
                        + " after feat reduction/resistance; "
                        + absorbed
                        + " absorbed by temp HP; HP "
                        + session.hitPoints()
                        + "/"
                        + session.maxHitPoints());
    }
}
