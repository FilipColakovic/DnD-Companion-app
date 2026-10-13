package org.example.ui.spells;

import static org.example.ui.spells.SpellLibraryComponents.cardTag;
import static org.example.ui.spells.SpellLibraryComponents.coloredTag;
import static org.example.ui.spells.SpellLibraryComponents.row;
import static org.example.ui.spells.SpellLibraryComponents.wrapped;

import org.example.domain.character.SorcererRules;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.SpellLibraryComponents.SpellTitle;
import org.example.ui.spells.SpellLibraryComponents.Wrap;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

final class LibrarySpellCard extends JPanel {
    private final SpellSelection selection;
    private final Supplier<String> focused;
    private final Consumer<String> inspect;
    private final Consumer<SpellCatalog.Spell> choose;
    String stamp = "";
    final java.awt.event.MouseAdapter inspectClick =
            new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (SwingUtilities.isLeftMouseButton(event)) inspect.accept(spell.name());
                }
            };
    final SpellCatalog.Spell spell;
    final JPanel actions = AppStyle.section(new BorderLayout()), badges = new Wrap();

    LibrarySpellCard(
            SpellCatalog.Spell spell,
            SpellSelection selection,
            Supplier<String> focused,
            Consumer<String> inspect,
            Consumer<SpellCatalog.Spell> choose) {
        super(new BorderLayout());
        this.spell = spell;
        this.selection = selection;
        this.focused = focused;
        this.inspect = inspect;
        this.choose = choose;
        getAccessibleContext().setAccessibleName("Spell card " + spell.name());
        setOpaque(false);
        JPanel card = AppStyle.card(new GridBagLayout());
        card.putClientProperty("surfaceRole", "surface");
        card.setBorder(new EmptyBorder(10, 10, 14, 14));
        add(card);
        JPanel heading = AppStyle.section(new BorderLayout(8, 0));
        JPanel tile = AppStyle.card(new BorderLayout());
        tile.putClientProperty("surfaceRole", spell.level() > 0 ? "slotAvailable" : "low");
        tile.putClientProperty("shadowOffset", 2);
        tile.setBorder(new EmptyBorder(3, 3, 5, 5));
        tile.setPreferredSize(new Dimension(44, 44));
        JLabel level = AppStyle.mono("" + spell.level(), 20);
        level.setHorizontalAlignment(SwingConstants.CENTER);
        AppStyle.setColorRole(level, spell.level() > 0 ? "badgeText" : "ink");
        tile.add(level);
        JPanel tileHolder = AppStyle.section(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tileHolder.add(tile);
        heading.add(tileHolder, BorderLayout.WEST);
        JButton name =
                new SpellTitle(
                        spell.name().toUpperCase(Locale.ROOT), () -> inspect.accept(spell.name()));
        name.getAccessibleContext().setAccessibleName("Inspect " + spell.name());
        heading.add(name, BorderLayout.CENTER);
        heading.add(actions, BorderLayout.EAST);
        row(card, heading);
        row(card, badges);
        var d = SpellDetails.forSpell(spell);
        String time = d.castingTime().split(":", 2)[0];
        JTextArea metadata =
                wrapped(
                        (time
                                        + "  //  "
                                        + d.range()
                                        + "  //  "
                                        + d.components().replaceAll("\\s*\\(.*\\)", ""))
                                .toUpperCase(Locale.ROOT));
        metadata.setFont(new Font("Consolas", Font.BOLD, 12));
        AppStyle.setColorRole(metadata, "accent");
        metadata.setToolTipText(
                "Casting time: "
                        + d.castingTime()
                        + " | Range: "
                        + d.range()
                        + " | Components: "
                        + d.components());
        JPanel strip = AppStyle.surfacePanel(new BorderLayout(), "low");
        strip.setBorder(new EmptyBorder(4, 6, 4, 6));
        strip.add(metadata);
        row(card, strip);
        bindInspection(this);
    }

    private void bindInspection(Component component) {
        if (component instanceof JComponent child
                && !(component instanceof AbstractButton button && button.isEnabled())
                && !Boolean.TRUE.equals(child.getClientProperty("spellCardInspection"))) {
            child.putClientProperty("spellCardInspection", true);
            child.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            child.addMouseListener(inspectClick);
        }
        if (component instanceof Container container)
            for (Component child : container.getComponents()) bindInspection(child);
    }

    void update() {
        String next =
                selection.selected(spell)
                        + ":"
                        + selection.automatic(spell)
                        + ":"
                        + selection.available(spell)
                        + ":"
                        + spell.name().equals(focused.get());
        if (next.equals(stamp)) return;
        stamp = next;
        actions.removeAll();
        actions.add(selectionActions(spell));
        badges.removeAll();
        var d = SpellDetails.forSpell(spell);
        if (selection.selected(spell))
            badges.add(
                    coloredTag(
                            selection.automatic(spell)
                                    ? "GRANTED"
                                    : spell.level() == 0 ? "KNOWN" : "PREPARED",
                            "badgeDark"));
        if (selection.subclass() == SorcererRules.Subclass.ABERRANT
                && SpellCatalog.unlockLevel(spell, selection.subclass()) > 0)
            badges.add(coloredTag("PSIONIC ORIGIN", "badgeBlue"));
        JLabel schoolTag = cardTag(d.school(), false);
        if (d.school().equals("Evocation")) AppStyle.setColorRole(schoolTag, "error");
        badges.add(schoolTag);
        if (d.concentration()) badges.add(coloredTag("CONCENTRATION", "badgeRed"));
        if (d.castingTime().startsWith("Reaction")) badges.add(coloredTag("REACTION", "badgeRed"));
        if (d.castingTime().startsWith("Bonus Action"))
            badges.add(coloredTag("BONUS ACTION", "badgeRed"));
        if (spell.level() == 0) badges.add(cardTag("CANTRIP", true));
        if (d.ritual()) badges.add(coloredTag("RITUAL", "badgeBlue"));
        if (!selection.available(spell)) badges.add(cardTag("LOCKED", false));
        setToolTipText(
                spell.name().equals(focused.get())
                        ? "Selected in codex inspector"
                        : "Click anywhere on this card to inspect");
        bindInspection(this);
        revalidate();
        repaint();
    }

    private JPanel selectionActions(SpellCatalog.Spell spell) {
        JPanel actions = AppStyle.section(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        boolean selected = selection.selected(spell),
                automatic = selection.automatic(spell),
                active = selection.available(spell);
        JButton select =
                AppStyle.compactButton(
                        automatic ? "Always prepared" : selected ? "Unlearn" : "Learn",
                        () -> choose.accept(spell));
        select.setToolTipText(
                automatic
                        ? "Automatically prepared by your subclass or species"
                        : selected
                                ? "Remove this manual spell choice"
                                : active
                                        ? "Add this spell to your character"
                                        : "This spell requires a higher level");
        select.getAccessibleContext().setAccessibleName("Choose " + spell.name());
        select.setEnabled(!automatic && (selected || active));
        if (!selected && active) select.putClientProperty("tactileRole", "yellow");
        actions.add(select);
        return actions;
    }
}
