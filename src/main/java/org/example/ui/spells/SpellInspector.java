package org.example.ui.spells;

import static org.example.ui.spells.SpellLibraryComponents.block;
import static org.example.ui.spells.SpellLibraryComponents.glue;
import static org.example.ui.spells.SpellLibraryComponents.row;
import static org.example.ui.spells.SpellLibraryComponents.tag;
import static org.example.ui.spells.SpellLibraryComponents.wrapped;

import org.example.domain.character.CharacterState;
import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.character.SorcererRules;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.ui.common.AppStyle;
import org.example.ui.common.TextUpdates;
import org.example.ui.spells.SpellLibraryComponents.Wrap;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Scrollable;

/**
 * Persistent inspector widgets: unrelated refreshes never replace text areas or their scroll state.
 */
final class SpellInspector extends JPanel implements Scrollable {
    private enum Spec {
        CASTING_SPEED,
        RANGE_AREA,
        COMPONENTS,
        DURATION,
        SPELLCASTING,
        SPELL_FLAGS
    }

    private final PlaySession session;
    private final Supplier<CharacterState> character;
    private final JLabel title = AppStyle.label("", 23, true), subtitle = tag("", true);
    private final JPanel stats = AppStyle.section(new GridLayout(0, 2, 8, 8));
    private final EnumMap<Spec, JTextArea> values = new EnumMap<>(Spec.class);
    private final JTextArea effect = wrapped(""), rules = wrapped(""), scaling = wrapped("");
    private final JPanel effectBlock = block("DAMAGE / EFFECT", effect),
            rulesBlock = block("OPERATIONAL SPELL RULES", rules),
            scalingBlock = block("HIGHER-LEVEL SCALING", scaling);
    private final Wrap compatible = new Wrap();
    private final JPanel compatibleBlock = block("COMPATIBLE CHOSEN METAMAGIC", compatible);
    private final JButton details;
    private SpellCatalog.Spell spell;
    private String compatibilityStamp = "";

    SpellInspector(PlaySession session, Supplier<CharacterState> character) {
        super(new GridBagLayout());
        this.session = session;
        this.character = character;
        putClientProperty("surfaceRole", "background");
        JPanel codex = AppStyle.card(new GridBagLayout());
        codex.putClientProperty("surfaceRole", "lowest");
        row(codex, tag("SPELL CODEX INSPECTOR // PHB 2024", true));
        row(codex, title);
        row(codex, subtitle);
        for (Spec spec : Spec.values()) {
            JTextArea value = wrapped("");
            values.put(spec, value);
            stats.add(
                    block(
                            spec.name().replace('_', ' ').replace("RANGE AREA", "RANGE / AREA"),
                            value));
        }
        row(codex, stats);
        row(codex, effectBlock);
        row(codex, rulesBlock);
        row(codex, scalingBlock);
        row(codex, compatibleBlock);
        details =
                AppStyle.compactButton(
                        "Full details",
                        () -> {
                            if (spell != null)
                                AppStyle.openWindow(
                                        this, spell.name(), new SpellDetailPanel(spell));
                        });
        row(codex, details);
        row(this, codex);
        glue(this);
    }

    void refresh(SpellCatalog.Spell focused) {
        boolean sameSpell = java.util.Objects.equals(spell, focused);
        spell = focused;
        boolean present = spell != null;
        for (JComponent component :
                List.of(subtitle, stats, effectBlock, rulesBlock, compatibleBlock, details))
            component.setVisible(present);
        scalingBlock.setVisible(false);
        if (!present) {
            title.setText("Select a spell to inspect its rules.");
            return;
        }
        var data = SpellDetails.forSpell(spell);
        title.setText(spell.name().toUpperCase(java.util.Locale.ROOT));
        subtitle.setText(
                (spell.levelName() + " // " + data.school()).toUpperCase(java.util.Locale.ROOT));
        set(Spec.CASTING_SPEED, data.castingTime().split(":", 2)[0], sameSpell);
        ((JComponent) values.get(Spec.CASTING_SPEED).getParent())
                .setToolTipText(data.castingTime());
        set(Spec.RANGE_AREA, data.range(), sameSpell);
        set(Spec.COMPONENTS, data.components(), sameSpell);
        set(Spec.DURATION, data.duration(), sameSpell);
        var state = character.get();
        int attack =
                SorcererRules.calculate(
                                        session.selection().level(),
                                        state.charisma(),
                                        session.selection().subclass())
                                .proficiencyBonus()
                        + session.spellcastingModifier(spell, state.charisma());
        set(
                Spec.SPELLCASTING,
                "Save DC "
                        + (8 + attack)
                        + " // Attack "
                        + String.format("%+d", attack)
                        + " // "
                        + session.spellcastingAbility(spell),
                sameSpell);
        set(
                Spec.SPELL_FLAGS,
                (data.concentration() ? "Concentration" : "No concentration")
                        + (data.ritual() ? " // Ritual" : ""),
                sameSpell);
        updateText(
                effect,
                CastingPanel.damageSummary(spell, spell.level(), session.selection().level()),
                sameSpell);
        updateText(rules, data.description(), sameSpell);
        updateText(scaling, data.higherLevels(), sameSpell);
        scalingBlock.setVisible(!data.higherLevels().isBlank());
        String next = spell.name() + session.metamagic();
        if (!next.equals(compatibilityStamp)) {
            compatibilityStamp = next;
            compatible.removeAll();
            for (Metamagic option :
                    session.metamagic().stream()
                            .sorted(Comparator.comparing(Metamagic::toString))
                            .toList()) {
                if (option.applies(spell)) {
                    JLabel chip = tag(option + " // " + option.cost() + " SP", false);
                    chip.setToolTipText(option.eligibilityNote(spell));
                    compatible.add(chip);
                }
            }
            if (compatible.getComponentCount() == 0)
                compatible.add(wrapped("No compatible chosen Metamagic."));
        }
        revalidate();
    }

    private void set(Spec spec, String value, boolean sameSpell) {
        updateText(values.get(spec), value, sameSpell);
    }

    private static void updateText(JTextArea area, String text, boolean preserve) {
        TextUpdates.set(area, text, preserve);
    }

    @Override
    public Dimension getPreferredScrollableViewportSize() {
        return new Dimension(450, 600);
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
        return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
        return getParent() != null && getPreferredSize().height < getParent().getHeight();
    }

    @Override
    public int getScrollableUnitIncrement(Rectangle bounds, int orientation, int direction) {
        return 24;
    }

    @Override
    public int getScrollableBlockIncrement(Rectangle bounds, int orientation, int direction) {
        return Math.max(24, bounds.height - 24);
    }
}
