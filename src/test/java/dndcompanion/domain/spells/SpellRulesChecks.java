package dndcompanion.domain.spells;

import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.character.PlaySession;
import dndcompanion.domain.character.SorcererRules;
import dndcompanion.ui.spells.CastingPanel;

import java.awt.Component;
import java.awt.Container;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;

/** Rules regressions use reviewed examples and exercise the actual compatible-option controls. */
public final class SpellRulesChecks {
    public static void main(String[] args) throws Exception {
        descriptions();
        damage();
        metamagic();
        atomicCasting();
        SwingUtilities.invokeAndWait(SpellRulesChecks::castingControls);
        System.out.println(
                "Passed: all 219 spell entries and 2,190 compatibility decisions, corrected source"
                        + " text, conditional damage, 2024 Metamagic regressions, and live casting"
                        + " selections.");
    }

    private static void descriptions() {
        check(
                SpellCatalog.all().size() == 219,
                "Review coverage must be updated when the catalog changes");
        for (var spell : SpellCatalog.all()) {
            var d = SpellDetails.forSpell(spell);
            check(
                    !d.description().isBlank() && !d.shortDescription().isBlank(),
                    "Description exists: " + spell.name());
            check(
                    !d.components().contains("M ()"),
                    "Material components are complete: " + spell.name());
            for (Metamagic option : Metamagic.values()) {
                option.applies(spell);
                check(
                        !option.eligibilityNote(spell).isBlank(),
                        "Compatibility explains its condition: " + spell.name() + " / " + option);
            }
        }
        check(
                details("Clairvoyance").castingTime().equals("10 minutes"),
                "Clairvoyance casting time");
        check(
                details("Legend Lore").castingTime().equals("10 minutes"),
                "Legend Lore casting time");
        check(
                details("Jump").components().contains("grasshopper"),
                "Jump's missing component restored");
        check(
                details("Command").description().contains("Approach."),
                "Command's missing option restored");
        check(
                details("Knock").description().startsWith("Choose an object"),
                "Knock targeting text restored");
        check(
                details("Chain Lightning").higherLevels().contains("additional bolt"),
                "Chain Lightning target scaling restored");
        check(
                details("Dissonant Whispers").higherLevels().contains("1d6"),
                "Dissonant Whispers damage scaling restored");
    }

    private static void damage() {
        for (String name :
                new String[] {
                    "Web",
                    "Dimension Door",
                    "Teleport",
                    "Earthquake",
                    "Etherealness",
                    "Power Word Kill",
                    "Wish",
                    "Alter Self",
                    "Enlarge/Reduce",
                    "Summon Dragon",
                    "Summon Construct",
                    "Animate Objects",
                    "Shield"
                }) {
            for (int slot = spell(name).level(); slot <= 9; slot++) {
                check(
                        DamageDice.profile(spell(name), slot, 20).isEmpty(),
                        "No invented primary roll: " + name);
            }
        }
        check(
                CastingPanel.damageSummary(spell("Web"), 2, 3).contains("No damage on casting"),
                "Web separates ignited webs from casting");
        check(
                CastingPanel.damageSummary(spell("Web"), 9, 20).contains("separate fire"),
                "Web's conditional damage does not scale");
        check(
                CastingPanel.damageSummary(spell("Teleport"), 7, 20).contains("mishap"),
                "Teleport damage is conditional");
        check(
                CastingPanel.damageSummary(spell("Ice Storm"), 5, 20).contains("Only Bludgeoning"),
                "Mixed upcast damage is explained");
        check(
                DamageDice.profile(spell("Dissonant Whispers"), 4, 7).orElseThrow().count() == 6,
                "Dissonant Whispers uses 6d6 at slot 4");
        check(
                DamageDice.profile(spell("Synaptic Static"), 5, 9).orElseThrow().count() == 8,
                "Debuff d6 is not additional damage");
        check(
                DamageDice.profile(spell("Cloud of Daggers"), 4, 7).orElseThrow().count() == 8,
                "Cloud of Daggers uses 8d4 at slot 4");
        for (var spell : SpellCatalog.all())
            for (int characterLevel = 1; characterLevel <= 20; characterLevel++) {
                for (int slot = Math.max(0, spell.level()); slot <= 9; slot++) {
                    var formula = DamageDice.profile(spell, slot, characterLevel);
                    if (formula.isPresent()) {
                        check(
                                SpellMechanics.hasDamageRoll(spell),
                                "Quick rolls require actual caster spell damage: " + spell.name());
                        check(
                                formula.get().count() > 0 && formula.get().sides() >= 4,
                                "Damage dice stay valid: " + spell.name());
                    }
                }
            }
    }

    private static void metamagic() {
        Set<String> twinExpected =
                Set.of(
                        "Banishment",
                        "Blindness/Deafness",
                        "Chain Lightning",
                        "Charm Monster",
                        "Charm Person",
                        "Command",
                        "Enhance Ability",
                        "Etherealness",
                        "Fly",
                        "Freedom of Movement",
                        "Gaseous Form",
                        "Hold Monster",
                        "Hold Person",
                        "Invisibility",
                        "Jump",
                        "Longstrider",
                        "Spider Climb",
                        "Animal Friendship",
                        "Bane",
                        "Bless",
                        "Heroism",
                        "Tasha's Hideous Laughter");
        Set<String> twinActual =
                SpellCatalog.all().stream()
                        .filter(Metamagic.TWINNED::applies)
                        .map(SpellCatalog.Spell::name)
                        .collect(Collectors.toSet());
        check(
                twinActual.equals(twinExpected),
                "Twinned's complete reviewed additional-target list");
        String[][] examples = {
            {"CAREFUL", "Web", "Haste"},
            {"DISTANT", "Chill Touch", "Burning Hands"},
            {"EMPOWERED", "Fireball", "Web"},
            {"EXTENDED", "Creation", "Synaptic Static"},
            {"HEIGHTENED", "Synaptic Static", "Poison Spray"},
            {"QUICKENED", "Web", "Dragon's Breath"},
            {"SEEKING", "Poison Spray", "Summon Construct"},
            {"SUBTLE", "True Strike", ""},
            {"TRANSMUTED", "Ice Storm", "Protection from Energy"},
            {"TWINNED", "Hold Person", "Chromatic Orb"}
        };
        for (String[] row : examples) {
            var option = Metamagic.valueOf(row[0]);
            check(
                    option.applies(spell(row[1])),
                    "2024 positive example: " + option + " / " + row[1]);
            if (!row[2].isEmpty())
                check(
                        !option.applies(spell(row[2])),
                        "2024 negative example: " + option + " / " + row[2]);
        }
        for (String name :
                new String[] {
                    "Ray of Enfeeblement", "Enlarge/Reduce", "Summon Dragon", "Blade Ward", "Wish"
                })
            check(
                    !Metamagic.EMPOWERED.applies(spell(name)),
                    "Damage/attack modifiers are not caster damage dice: " + name);
        for (String name : new String[] {"True Strike", "Sorcerous Burst", "Bigby's Hand"})
            check(
                    Metamagic.SEEKING.applies(spell(name)),
                    "Recognize caster attack rolls beyond literal 'spell attack': " + name);
        check(
                Metamagic.HEIGHTENED.applies(spell("Power Word Stun")),
                "Later forced saves qualify, despite no initial save");
        check(
                Metamagic.TRANSMUTED
                        .eligibilityNote(spell("Sorcerous Burst"))
                        .contains("Psychic cannot"),
                "Elemental choice condition is visible");
        check(
                Metamagic.EMPOWERED
                        .eligibilityNote(spell("Power Word Kill"))
                        .contains("above 100 HP"),
                "Conditional damage requirement is visible");
    }

    private static void atomicCasting() {
        var selection = new SpellSelection();
        selection.setCharacter(3, SorcererRules.Subclass.WILD_MAGIC);
        selection.restoreChoices(Set.of("Chromatic Orb", "Hold Person"));
        var session = new PlaySession(selection);
        session.choose(Metamagic.TWINNED, true);
        var before = session.snapshot();
        check(
                !session.cast(spell("Chromatic Orb"), 1, false, Set.of(Metamagic.TWINNED))
                        .isEmpty(),
                "Reject an Orb cast with Twinned");
        check(
                session.snapshot().equals(before),
                "Rejected Metamagic spends neither points nor slots");
        check(
                session.cast(spell("Hold Person"), 2, false, Set.of(Metamagic.TWINNED)).isEmpty(),
                "Allow Hold Person with Twinned");
        check(
                session.remainingPoints() == 2 && session.remainingSlots(2) == 1,
                "Twinned spends 1 SP and the actual level-2 slot");
    }

    private static void castingControls() {
        var selection = new SpellSelection();
        selection.setCharacter(3, SorcererRules.Subclass.WILD_MAGIC);
        selection.restoreChoices(Set.of("Web", "Chromatic Orb", "Hold Person"));
        var session = new PlaySession(selection);
        session.choose(Metamagic.TWINNED, true);
        session.choose(Metamagic.HEIGHTENED, true);
        var panel = new CastingPanel(session, () -> 17);
        try {
            check(panel.selectSpell("Web"), "Select Web in arsenal");
            check(
                    panel.compatibleChosen(spell("Web"))
                            .equals(java.util.List.of(Metamagic.HEIGHTENED)),
                    "Web matches Heightened, not Twinned");
            check(panel.selectSpell("Chromatic Orb"), "Switch to Orb");
            check(
                    panel.compatibleChosen(spell("Chromatic Orb")).isEmpty(),
                    "Orb matches neither chosen option");
            check(panel.selectSpell("Hold Person"), "Switch to Hold Person");
            check(
                    panel.compatibleChosen(spell("Hold Person")).contains(Metamagic.TWINNED),
                    "Hold Person matches Twinned");
            session.choose(Metamagic.HEIGHTENED, false);
            session.choose(Metamagic.QUICKENED, true);
            check(
                    panel.compatibleChosen(spell("Hold Person")).contains(Metamagic.QUICKENED),
                    "Compatibility follows newly chosen options");
        } finally {
            panel.dispose();
        }
    }

    private static <T extends Component> T component(Container parent, Class<T> type, String name) {
        for (Component c : parent.getComponents()) {
            if (type.isInstance(c) && name.equals(c.getName())) return type.cast(c);
            if (c instanceof Container child) {
                T found = find(child, type, name);
                if (found != null) return found;
            }
        }
        throw new AssertionError("Missing component: " + name);
    }

    private static <T extends Component> T find(Container parent, Class<T> type, String name) {
        for (Component c : parent.getComponents()) {
            if (type.isInstance(c) && name.equals(c.getName())) return type.cast(c);
            if (c instanceof Container child) {
                T found = find(child, type, name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(s -> s.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static SpellDetails details(String name) {
        return SpellDetails.forSpell(spell(name));
    }

    private static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
