package org.example.ui.spells;

import org.example.domain.character.Metamagic;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.ui.common.AppStyle;
import org.example.ui.common.TactileStyle;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

final class CombatSpellCard extends JButton {
    private final SpellCatalog.Spell spell;
    private final Supplier<SpellCatalog.Spell> selected;
    private final Function<SpellCatalog.Spell, List<Metamagic>> compatible;

    CombatSpellCard(
            SpellCatalog.Spell spell,
            Supplier<SpellCatalog.Spell> selected,
            Function<SpellCatalog.Spell, List<Metamagic>> compatible,
            Consumer<String> select) {
        super(spell.name());
        this.spell = spell;
        this.selected = selected;
        this.compatible = compatible;
        putClientProperty("tactileCustomPaint", true);
        setPreferredSize(new Dimension(350, compatible.apply(spell).isEmpty() ? 58 : 78));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setToolTipText(
                SpellDetails.forSpell(spell).shortDescription()
                        + (compatible.apply(spell).isEmpty()
                                ? ""
                                : " Enhance with: "
                                        + String.join(
                                                ", ",
                                                compatible.apply(spell).stream()
                                                        .map(Object::toString)
                                                        .toList())));
        getAccessibleContext().setAccessibleDescription(getToolTipText());
        addActionListener(e -> select.accept(spell.name()));
    }

    void refreshCard() {
        setPreferredSize(new Dimension(350, compatible.apply(spell).isEmpty() ? 58 : 78));
        setToolTipText(
                SpellDetails.forSpell(spell).shortDescription()
                        + (compatible.apply(spell).isEmpty()
                                ? ""
                                : " Enhance with: "
                                        + String.join(
                                                ", ",
                                                compatible.apply(spell).stream()
                                                        .map(Object::toString)
                                                        .toList())));
        getAccessibleContext().setAccessibleDescription(getToolTipText());
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = AppStyle.graphics(graphics);
        boolean selected = spell.equals(this.selected.get());
        g.setColor(selected ? TactileStyle.YELLOW : AppStyle.SOFT);
        g.fillRect(0, 0, getWidth() - 3, getHeight() - 3);
        g.setColor(AppStyle.INK);
        g.drawRect(0, 0, getWidth() - 3, getHeight() - 3);
        if (selected) {
            g.setColor(TactileStyle.BLUE);
            g.setStroke(new BasicStroke(2));
            g.drawRect(2, 2, getWidth() - 7, getHeight() - 7);
        }
        g.setFont(getFont().deriveFont(Font.BOLD, AppStyle.textSize()));
        g.setColor(AppStyle.INK);
        g.drawString(spell.name(), 12, 22);
        g.setFont(getFont().deriveFont(Font.PLAIN, AppStyle.textSize() - 3f));
        g.setColor(selected ? TactileStyle.BLUE : AppStyle.MUTED);
        g.drawString(
                spell.levelName()
                        + " · "
                        + SpellDetails.forSpell(spell).school()
                        + (SpellDetails.forSpell(spell).concentration() ? " · Concentration" : ""),
                12,
                43);
        if (!compatible.apply(spell).isEmpty()) {
            String enhancement =
                    "Enhance: "
                            + String.join(
                                    " · ",
                                    compatible.apply(spell).stream()
                                            .map(m -> m.toString().replace(" Spell", ""))
                                            .toList());
            while (g.getFontMetrics().stringWidth(enhancement) > getWidth() - 24
                    && enhancement.length() > 10) {
                enhancement = enhancement.substring(0, enhancement.length() - 2) + "…";
            }
            g.setColor(AppStyle.INK);
            g.drawString(enhancement, 12, 64);
        }
        if (getWidth() > 390) {
            String range = SpellDetails.forSpell(spell).range();
            g.setColor(AppStyle.INK);
            g.drawString(range, getWidth() - g.getFontMetrics().stringWidth(range) - 12, 22);
        }
        g.dispose();
    }
}
