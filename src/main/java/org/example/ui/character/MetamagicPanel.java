package org.example.ui.character;

import org.example.domain.character.Metamagic;
import org.example.domain.character.PlaySession;
import org.example.domain.spells.SpellCatalog;
import org.example.ui.common.AppStyle;
import org.example.ui.common.LiveView;
import org.example.ui.common.MetamagicIcon;
import org.example.ui.spells.SpellDetailPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;

/** Live cards for known Metamagic and its compatible prepared spells. */
public final class MetamagicPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final JLabel counts = AppStyle.badge("");
    private final JLabel message = AppStyle.label(" ", 14, false);
    private final EnumMap<Metamagic, OptionCard> cards = new EnumMap<>(Metamagic.class);
    private final CardGrid grid = new CardGrid();
    private final Runnable listener = this::refresh;

    public MetamagicPanel(PlaySession session) {
        super(new BorderLayout(0, 16));
        this.session = session;
        setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel header = AppStyle.card(new BorderLayout());
        header.setBorder(new EmptyBorder(14, 16, 18, 20));
        JPanel counter = AppStyle.surfacePanel(new BorderLayout(), "high");
        counter.setBorder(BorderFactory.createMatteBorder(1, 1, 3, 3, AppStyle.LINE));
        JLabel title = AppStyle.label("METAMAGIC SELECTION", 10, true);
        AppStyle.setColorRole(title, "muted");
        title.setBorder(new EmptyBorder(5, 8, 5, 8));
        counter.add(title, BorderLayout.WEST);
        counter.add(counts, BorderLayout.CENTER);
        counts.getAccessibleContext().setAccessibleName("Metamagic selection count");
        header.add(counter, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);
        for (Metamagic option : Metamagic.values()) {
            OptionCard card = new OptionCard(option);
            cards.put(option, card);
            grid.add(card.panel);
        }
        JScrollPane scroll = new JScrollPane(grid);
        AppStyle.scrollPane(scroll);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().putClientProperty("surfaceRole", "background");
        scroll.getVerticalScrollBar().setUnitIncrement(24);
        add(scroll, BorderLayout.CENTER);
        add(message, BorderLayout.SOUTH);
        session.addListener(listener);
        session.selection().addChangeListener(listener);
        refresh();
    }

    private void details(Metamagic option) {
        AppStyle.openWindow(this, option.toString(), new MetamagicDetailPanel(option, session));
    }

    private void refresh() {
        int limit = Metamagic.limit(session.selection().level());
        counts.setText(session.metamagic().size() + " / " + limit + " SELECTED");
        counts.setToolTipText(
                session.metamagic().size() > limit
                        ? "Over limit: remove options to match your current level"
                        : "Select Metamagic options below; click a card title for full details");
        cards.values().forEach(card -> card.refresh(limit));
        List<OptionCard> ordered =
                cards.values().stream()
                        .sorted(
                                Comparator.comparing(
                                                (OptionCard card) ->
                                                        !session.metamagic().contains(card.option))
                                        .thenComparing(card -> card.option.toString()))
                        .toList();
        for (int index = 0; index < ordered.size(); index++) {
            JPanel card = ordered.get(index).panel;
            if (grid.getComponentZOrder(card) != index) grid.setComponentZOrder(card, index);
        }
        AppStyle.applyAppearance(this);
        grid.revalidate();
        grid.repaint();
    }

    private final class OptionCard {
        final Metamagic option;
        final JPanel panel = AppStyle.card(new BorderLayout(0, 12));
        final JLabel status = AppStyle.mono("", 10);
        final JLabel cost;
        final JToggleButton choose = new EquipButton();
        final JPanel synergies = new SynergyChips();
        List<SpellCatalog.Spell> shownSpells;

        OptionCard(Metamagic option) {
            this.option = option;
            panel.setBorder(new EmptyBorder(4, 4, 8, 8));
            JPanel header = AppStyle.surfacePanel(new BorderLayout(8, 8), "slotAvailable");
            header.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createMatteBorder(0, 0, 2, 0, AppStyle.LINE),
                            new EmptyBorder(18, 16, 16, 16)));
            JButton title =
                    AppStyle.textButton(
                            option.toString().toUpperCase(Locale.ROOT), () -> details(option));
            title.setFont(new Font("Segoe UI", Font.BOLD, 15));
            AppStyle.setColorRole(title, "badgeText");
            title.getAccessibleContext().setAccessibleName("Details for " + option);
            title.setToolTipText("Open full rules and the collapsible compatible-spell list");
            cost = AppStyle.mono(option.cost() + " SP", 12);
            cost.setOpaque(true);
            cost.setBorder(
                    BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(AppStyle.LINE),
                            new EmptyBorder(3, 7, 3, 7)));
            JPanel identity = AppStyle.section(new BorderLayout(0, 4));
            identity.add(title, BorderLayout.NORTH);
            JLabel subtitle = AppStyle.mono(noteTitle(option), 10);
            AppStyle.setColorRole(subtitle, "accent");
            identity.add(subtitle, BorderLayout.CENTER);
            JPanel top = AppStyle.section(new BorderLayout(10, 0));
            JLabel marker = new JLabel(new MetamagicIcon(option, 40));
            marker.setToolTipText(option.toString());
            marker.getAccessibleContext().setAccessibleName(option + " icon");
            JPanel markerHolder = AppStyle.section(new FlowLayout(FlowLayout.LEFT, 0, 0));
            markerHolder.add(marker);
            top.add(markerHolder, BorderLayout.WEST);
            top.add(identity, BorderLayout.CENTER);
            top.add(cost, BorderLayout.EAST);
            header.add(top, BorderLayout.CENTER);
            panel.add(header, BorderLayout.NORTH);

            JPanel body = AppStyle.section(new GridBagLayout());
            body.setBorder(new EmptyBorder(12, 20, 0, 20));
            JPanel protocol = AppStyle.section(new BorderLayout(6, 0));
            protocol.add(AppStyle.mono("STATUS PROTOCOL", 10), BorderLayout.WEST);
            status.setOpaque(true);
            status.setBorder(new EmptyBorder(6, 8, 6, 8));
            protocol.add(status, BorderLayout.EAST);
            cardRow(body, protocol);
            cardRow(body, text(option.description(), 14));
            JPanel note = AppStyle.card(new BorderLayout(0, 6));
            note.putClientProperty("surfaceRole", "noteBlue");
            note.setBorder(new EmptyBorder(10, 10, 14, 14));
            JLabel noteTitle = AppStyle.mono(noteTitle(option), 10);
            AppStyle.setColorRole(noteTitle, "accent");
            note.add(noteTitle, BorderLayout.NORTH);
            note.add(text(note(option), 12), BorderLayout.CENTER);
            cardRow(body, note);
            JPanel prepared = AppStyle.section(new BorderLayout(0, 6));
            prepared.add(AppStyle.mono("PREPARED SYNERGIES", 10), BorderLayout.NORTH);
            JPanel chips = AppStyle.section(new BorderLayout());
            chips.add(synergies, BorderLayout.NORTH);
            prepared.add(chips, BorderLayout.CENTER);
            cardRow(body, prepared);
            GridBagConstraints glue = new GridBagConstraints();
            glue.gridx = 0;
            glue.gridy = body.getComponentCount();
            glue.weighty = 1;
            body.add(Box.createVerticalGlue(), glue);
            panel.add(body, BorderLayout.CENTER);

            JPanel footer = AppStyle.section(new BorderLayout(0, 10));
            footer.setBorder(new EmptyBorder(0, 20, 16, 20));
            footer.add(separator(), BorderLayout.NORTH);
            footer.add(choose, BorderLayout.CENTER);
            choose.getAccessibleContext().setAccessibleName("Choose " + option);
            choose.addActionListener(
                    event -> {
                        message.setText(session.choose(option, choose.isSelected()));
                        MetamagicPanel.this.refresh();
                    });
            panel.add(footer, BorderLayout.SOUTH);
        }

        void refresh(int limit) {
            boolean selected = session.metamagic().contains(option);
            panel.putClientProperty(
                    "surfaceRole", selected && limit > 0 ? "metamagicActive" : "metamagicCard");
            choose.setSelected(selected);
            choose.setText(selected ? "EQUIPPED (ACTIVE)" : "+ SELECT");
            choose.setIcon(selected ? EquipButton.CHECK : null);
            choose.setEnabled(limit > 0 || selected);
            choose.setToolTipText(
                    selected
                            ? "Remove " + option
                            : limit == 0
                                    ? "Metamagic unlocks at Sorcerer level 2"
                                    : "Select " + option);
            status.setText(selected ? "EQUIPPED ACTIVE" : "AVAILABLE");
            status.putClientProperty("surfaceRole", selected ? "badgeDark" : "high");
            AppStyle.setColorRole(status, selected ? "badgeYellowText" : "ink");
            cost.putClientProperty("surfaceRole", "high");
            AppStyle.setColorRole(cost, "ink");
            List<SpellCatalog.Spell> compatible =
                    session.selection().catalog().stream()
                            .filter(session.selection()::selected)
                            .filter(session.selection()::available)
                            .filter(option::applies)
                            .toList();
            if (compatible.equals(shownSpells)) return;
            shownSpells = compatible;
            synergies.removeAll();
            for (SpellCatalog.Spell spell : compatible.stream().limit(3).toList()) {
                JButton chip =
                        spellChip(
                                spell.name(),
                                () ->
                                        AppStyle.openWindow(
                                                MetamagicPanel.this,
                                                spell.name(),
                                                new SpellDetailPanel(spell)));
                chip.setToolTipText(spell.name() + ": " + option.eligibilityNote(spell));
                synergies.add(chip);
            }
            if (compatible.size() > 3) {
                JButton more =
                        spellChip("+ " + (compatible.size() - 3) + " more", () -> details(option));
                more.setToolTipText("Open " + option + " details to browse all compatible spells");
                synergies.add(more);
            } else if (compatible.isEmpty()) {
                synergies.add(AppStyle.label("No compatible prepared spells", 11, false));
            }
        }
    }

    private static JButton spellChip(String title, Runnable action) {
        JButton chip = AppStyle.compactButton(title, action);
        chip.setFont(new Font("Consolas", Font.BOLD, 12));
        chip.setBorder(new EmptyBorder(4, 6, 7, 9));
        return chip;
    }

    private static void cardRow(JPanel body, JComponent child) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = body.getComponentCount();
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTH;
        c.insets = new Insets(0, 0, 12, 0);
        body.add(child, c);
    }

    private static final class SynergyChips extends JPanel {
        SynergyChips() {
            super(new FlowLayout(FlowLayout.LEFT, 7, 6));
            setOpaque(false);
        }

        @Override
        public Dimension getPreferredSize() {
            int width = getWidth() > 0 ? getWidth() : 240, x = 0, line = 0, height = 0;
            for (Component child : getComponents()) {
                Dimension size = child.getPreferredSize();
                if (x > 0 && x + size.width > width - 14) {
                    height += line + 6;
                    x = 0;
                    line = 0;
                }
                x += size.width + 7;
                line = Math.max(line, size.height);
            }
            return new Dimension(width, height + line + 12);
        }

        @Override
        public Dimension getMinimumSize() {
            return new Dimension(0, getPreferredSize().height);
        }
    }

    private static JTextArea text(String value, int size) {
        JTextArea text =
                new JTextArea(value) {
                    @Override
                    public Dimension getPreferredSize() {
                        int width = getWidth() > 0 ? getWidth() : 230;
                        getUI().getRootView(this).setSize(Math.max(1, width), Float.MAX_VALUE);
                        return new Dimension(
                                width,
                                (int)
                                                Math.ceil(
                                                        getUI().getRootView(this)
                                                                .getPreferredSpan(
                                                                        javax.swing.text.View
                                                                                .Y_AXIS))
                                        + getFontMetrics(getFont()).getHeight());
                    }

                    @Override
                    public Dimension getMinimumSize() {
                        return new Dimension(0, getPreferredSize().height);
                    }
                };
        text.setEditable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        text.setOpaque(false);
        text.setFont(new Font("Segoe UI", Font.PLAIN, size));
        AppStyle.setColorRole(text, "ink");
        return text;
    }

    private static JComponent separator() {
        JPanel line = AppStyle.section(new BorderLayout());
        line.setPreferredSize(new Dimension(0, 1));
        line.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppStyle.LINE));
        return line;
    }

    private static String noteTitle(Metamagic option) {
        return switch (option) {
            case CAREFUL, HEIGHTENED, TWINNED -> "SAVES & TARGETS";
            case DISTANT -> "RANGE PROTOCOL";
            case EMPOWERED -> "DAMAGE RECOVERY";
            case EXTENDED -> "DURATION & CONCENTRATION";
            case QUICKENED -> "ACTION ECONOMY";
            case SEEKING -> "AFTER A MISS";
            case SUBTLE -> "COMPONENT RULES";
            case TRANSMUTED -> "ELEMENTAL UTILITY";
        };
    }

    private static String note(Metamagic option) {
        return switch (option) {
            case CAREFUL ->
                    "Protect chosen creatures on saving throws. Damage without a save is"
                            + " unaffected.";
            case DISTANT ->
                    "Changes range, not area size. Self range and separate reaction triggers stay"
                            + " unchanged.";
            case EMPOWERED ->
                    "Use after rolling damage. Reroll damage dice, not attack or effect-selection"
                            + " dice.";
            case EXTENDED ->
                    "When concentration applies, its saves gain Advantage. Duration caps at 24"
                            + " hours.";
            case HEIGHTENED ->
                    "Choose one target. Disadvantage applies to its saves against this spell.";
            case QUICKENED ->
                    "Only Action spells qualify. Preserve your Action while respecting the"
                            + " leveled-spell restriction.";
            case SEEKING ->
                    "Keep the new attack result. Can combine with another Metamagic option.";
            case SUBTLE ->
                    "Priced or consumed materials remain required. Other spell components are"
                            + " removed.";
            case TRANSMUTED ->
                    "Replace a listed elemental damage type. Other damage types and effects stay"
                            + " unchanged.";
            case TWINNED ->
                    "Requires additional creature targets on upcasting. Extra rays, darts and orb"
                            + " leaps do not qualify.";
        };
    }

    /** Black equipped state mirrors the reference without platform focus outlines. */
    private static final class EquipButton extends JToggleButton {
        private static final Icon CHECK =
                new Icon() {
                    public int getIconWidth() {
                        return 12;
                    }

                    public int getIconHeight() {
                        return 12;
                    }

                    public void paintIcon(Component component, Graphics graphics, int x, int y) {
                        Graphics2D g = (Graphics2D) graphics.create();
                        g.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g.setColor(new Color(0xffcc00));
                        g.drawRect(x + 1, y + 1, 10, 10);
                        g.drawPolyline(
                                new int[] {x + 3, x + 5, x + 9},
                                new int[] {y + 6, y + 8, y + 4},
                                3);
                        g.dispose();
                    }
                };

        EquipButton() {
            setUI(new BasicButtonUI());
            putClientProperty("tactileCustomPaint", true);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setContentAreaFilled(false);
            setFocusPainted(false);
            setBorderPainted(false);
            setBorder(new EmptyBorder(10, 8, 12, 11));
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            Graphics2D g = (Graphics2D) graphics.create();
            int shift = getModel().isPressed() && getModel().isArmed() ? 2 : 0;
            g.setColor(Color.BLACK);
            g.fillRect(3, 3, getWidth() - 3, getHeight() - 3);
            g.setColor(
                    isSelected()
                            ? new Color(0x242424)
                            : getModel().isRollover() ? AppStyle.HIGH : AppStyle.LOW);
            g.fillRect(shift, shift, getWidth() - 4, getHeight() - 4);
            g.setColor(AppStyle.LINE);
            g.drawRect(shift, shift, getWidth() - 5, getHeight() - 5);
            g.dispose();
            setForeground(
                    !isEnabled()
                            ? AppStyle.MUTED
                            : isSelected() ? new Color(0xffcc00) : AppStyle.INK);
            super.paintComponent(graphics);
        }
    }

    /** Viewport-width layout: three cards on wide screens, fewer at large text sizes. */
    private static final class CardGrid extends JPanel implements Scrollable {
        CardGrid() {
            super(new GridLayout(0, 3, 14, 14));
            putClientProperty("surfaceRole", "background");
            setOpaque(true);
        }

        private int fontDelta() {
            return Math.max(0, AppStyle.textSize() - 14);
        }

        private int columns(int width) {
            return Math.max(1, Math.min(3, (width + 14) / (280 + fontDelta() * 16 + 14)));
        }

        @Override
        public void doLayout() {
            ((GridLayout) getLayout()).setColumns(columns(getWidth()));
            super.doLayout();
        }

        @Override
        public Dimension getPreferredSize() {
            int width =
                    getParent() instanceof JViewport viewport
                            ? viewport.getExtentSize().width
                            : 960;
            int rows = (getComponentCount() + columns(width) - 1) / columns(width);
            return new Dimension(
                    width, rows * (500 + fontDelta() * 26) + Math.max(0, rows - 1) * 14);
        }

        public Dimension getPreferredScrollableViewportSize() {
            return new Dimension(960, 700);
        }

        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 24;
        }

        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(24, visible.height - 24);
        }

        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    public void dispose() {
        session.removeListener(listener);
        session.selection().removeChangeListener(listener);
    }
}
