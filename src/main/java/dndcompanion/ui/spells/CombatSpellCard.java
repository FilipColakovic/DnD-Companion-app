package dndcompanion.ui.spells;

import dndcompanion.domain.character.Metamagic;
import dndcompanion.domain.spells.SpellCatalog;
import dndcompanion.domain.spells.SpellDetails;
import dndcompanion.ui.common.AppStyle;
import dndcompanion.ui.common.TactileStyle;

import java.awt.Color;
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
        setPreferredSize(new Dimension(260, cardHeight()));
        setFocusPainted(false);
        setContentAreaFilled(false);
        setBorder(new EmptyBorder(6, 10, 6, 10));
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
        setPreferredSize(new Dimension(260, cardHeight()));
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
        int titleHeight = g.getFontMetrics(titleFont()).getHeight();
        int detailHeight = g.getFontMetrics(detailFont()).getHeight();
        int titleY = 7 + g.getFontMetrics(titleFont()).getAscent();
        int metadataY = 10 + titleHeight + g.getFontMetrics(detailFont()).getAscent();
        int divider = 14 + titleHeight + 2 * detailHeight;
        int enhancementY = divider + 4 + g.getFontMetrics(detailFont()).getAscent();
        boolean selected = spell.equals(this.selected.get());
        g.setColor(AppStyle.LINE);
        g.fillRect(3, 3, getWidth() - 3, getHeight() - 3);
        g.setColor(selected ? new Color(0xf5a000) : AppStyle.LOWEST);
        g.fillRect(0, 0, getWidth() - 3, getHeight() - 3);
        g.setColor(AppStyle.LINE);
        g.drawRect(0, 0, getWidth() - 3, getHeight() - 3);
        g.drawRect(1, 1, getWidth() - 5, getHeight() - 5);
        String badge = spell.level() == 0 ? "CANTRIP" : "LVL " + spell.level();
        g.setFont(getFont().deriveFont(Font.BOLD, Math.max(10, AppStyle.textSize() - 3f)));
        int badgeWidth = g.getFontMetrics().stringWidth(badge) + 12;
        int badgeX = getWidth() - badgeWidth - 14;
        g.setColor(
                spell.level() == 0
                        ? selected ? new Color(0xe8e3da) : AppStyle.SOFT
                        : spell.level() == 1 ? TactileStyle.BLUE : new Color(0xdc2626));
        g.fillRect(badgeX, 8, badgeWidth, 19);
        g.setColor(spell.level() == 0 ? (selected ? Color.BLACK : AppStyle.INK) : Color.WHITE);
        g.drawString(badge, badgeX + 6, 22);
        g.setFont(titleFont());
        g.setColor(selected ? Color.BLACK : AppStyle.INK);
        g.drawString(
                fit(g, spell.name().toUpperCase(java.util.Locale.ROOT), badgeX - 20), 10, titleY);
        g.setFont(detailFont());
        g.setColor(selected ? Color.BLACK : AppStyle.MUTED);
        g.drawString(
                fit(
                        g,
                        spell.levelName() + " · " + SpellDetails.forSpell(spell).school(),
                        getWidth() - 26),
                10,
                metadataY);
        if (SpellDetails.forSpell(spell).concentration())
            g.drawString("Concentration", 10, metadataY + detailHeight);
        g.setColor(selected ? new Color(0xc68100) : AppStyle.SOFT);
        g.drawLine(10, divider, getWidth() - 14, divider);
        if (!compatible.apply(spell).isEmpty()) {
            String enhancement =
                    "Enhance: "
                            + String.join(
                                    " · ",
                                    compatible.apply(spell).stream()
                                            .map(m -> m.toString().replace(" Spell", ""))
                                            .toList());
            g.setColor(selected ? Color.BLACK : AppStyle.MUTED);
            g.drawString(fit(g, enhancement, getWidth() - (selected ? 58 : 24)), 10, enhancementY);
        }
        if (selected) {
            g.setColor(new Color(0xe8e3da));
            g.fillRect(getWidth() - 42, getHeight() - 27, 27, 17);
            g.setColor(Color.BLACK);
            g.drawRect(getWidth() - 42, getHeight() - 27, 27, 17);
            g.drawString("SEL", getWidth() - 39, enhancementY);
        }
        g.dispose();
    }

    private int cardHeight() {
        return 28
                + getFontMetrics(titleFont()).getHeight()
                + 3 * getFontMetrics(detailFont()).getHeight();
    }

    private Font titleFont() {
        return getFont().deriveFont(Font.BOLD, AppStyle.textSize());
    }

    private Font detailFont() {
        return getFont().deriveFont(Font.PLAIN, Math.max(11, AppStyle.textSize() - 3f));
    }

    private static String fit(Graphics2D g, String text, int width) {
        if (g.getFontMetrics().stringWidth(text) <= width) return text;
        while (!text.isEmpty() && g.getFontMetrics().stringWidth(text + "…") > width)
            text = text.substring(0, text.length() - 1);
        return text + "…";
    }
}
