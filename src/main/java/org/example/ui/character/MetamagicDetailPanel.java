package org.example.ui.character;

import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.spells.SpellCatalog;
import org.example.ui.common.AppStyle;
import org.example.ui.common.LiveView;
import org.example.ui.spells.SpellDetailPanel;

import java.awt.BorderLayout;
import java.awt.GridLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;

public final class MetamagicDetailPanel extends JPanel implements LiveView {
    private final Metamagic option;
    private final PlaySession session;
    private final JPanel list = AppStyle.section(new GridLayout(0, 1, 0, 8));
    private final JScrollPane scroll = new JScrollPane(list);
    private final JButton expand;
    private final Runnable listener = this::refresh;

    public MetamagicDetailPanel(Metamagic option, PlaySession session) {
        super(new BorderLayout(0, 16));
        this.option = option;
        this.session = session;
        setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel header = AppStyle.card(new BorderLayout(0, 16));
        header.add(
                AppStyle.label(option + " · " + option.cost() + " Sorcery Points", 22, true),
                BorderLayout.NORTH);
        JTextArea description = AppStyle.text(option.description());
        description.setRows(5);
        header.add(description, BorderLayout.CENTER);
        JTextArea note =
                AppStyle.text(
                        "Choose targets and resolve rolls at the table. Seeking is used after a"
                            + " miss; Empowered after rolling damage. The list identifies spell"
                            + " candidates; conditions in the description still apply.");
        note.setRows(3);
        header.add(note, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);
        JPanel body = AppStyle.card(new BorderLayout(0, 14));
        expand = new JButton("Show compatible spells ▸");
        AppStyle.roundButton(expand);
        expand.addActionListener(
                event -> {
                    scroll.setVisible(!scroll.isVisible());
                    expand.setText(
                            scroll.isVisible()
                                    ? "Hide compatible spells ▾"
                                    : "Show compatible spells ▸");
                    revalidate();
                    repaint();
                });
        body.add(expand, BorderLayout.NORTH);
        AppStyle.scrollPane(scroll);
        scroll.setVisible(false);
        body.add(scroll, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);
        session.selection().addChangeListener(listener);
        refresh();
    }

    private void refresh() {
        list.removeAll();
        for (SpellCatalog.Spell spell : session.selection().catalog()) {
            if (option.applies(spell)) {
                JButton name =
                        AppStyle.button(
                                spell.levelName() + " · " + spell.name(),
                                () ->
                                        AppStyle.openWindow(
                                                this, spell.name(), new SpellDetailPanel(spell)));
                name.setToolTipText(option.eligibilityNote(spell));
                list.add(name);
            }
        }
        list.revalidate();
        list.repaint();
        AppStyle.applyAppearance(list);
    }

    public boolean isSpellListExpanded() {
        return scroll.isVisible();
    }

    public void dispose() {
        session.selection().removeChangeListener(listener);
    }
}
