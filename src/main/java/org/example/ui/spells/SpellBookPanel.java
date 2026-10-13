package org.example.ui.spells;

import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellSelection;
import org.example.ui.common.AppStyle;
import org.example.ui.common.SpellBookIcon;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;

import static org.example.ui.common.AppStyle.section;

/** Searchable spell selector; the shared selection model belongs to the main character window. */
public final class SpellBookPanel extends JPanel implements org.example.ui.common.LiveView {
    private final SpellSelection selection;
    private final JTextField search = new JTextField(18);
    private final JComboBox<String> spellLevel = new JComboBox<>(new String[] {
            "All levels", "Cantrips", "Level 1", "Level 2", "Level 3", "Level 4", "Level 5",
            "Level 6", "Level 7", "Level 8", "Level 9"});
    private final JCheckBox availableOnly = new JCheckBox("Available at my level");
    private final JCheckBox selectedOnly = new JCheckBox("My spells only");
    private final JLabel character = new JLabel();
    private final JLabel counts = new JLabel();
    private final JLabel message = new JLabel(" ");
    private final JLabel results = new JLabel();
    private final Runnable changeListener = this::characterChanged;
    private final Consumer<SpellCatalog.Spell> detailOpener;
    private final SpellTableModel model;
    private final JTable table;
    private final SpellRowRenderer spellRenderer;
    private JDialog knownSpellsWindow;
    private final boolean knownSpellsView;

    public SpellBookPanel(SpellSelection selection) {
        this(selection, null);
    }

    SpellBookPanel(SpellSelection selection, Consumer<SpellCatalog.Spell> detailOpener) {
        this(selection, detailOpener, false);
    }

    private SpellBookPanel(SpellSelection selection, Consumer<SpellCatalog.Spell> detailOpener,
                           boolean knownSpellsView) {
        super(new BorderLayout(0, 16));
        this.selection = selection;
        this.knownSpellsView = knownSpellsView;
        model = new SpellTableModel(selection, message::setText, knownSpellsView);
        table = new JTable(model);
        spellRenderer = new SpellRowRenderer(model);
        this.detailOpener = detailOpener == null ? this::openDetails : detailOpener;
        setBackground(Color.WHITE);
        setBorder(new EmptyBorder(16, 16, 16, 16));
        JPanel header = AppStyle.card(new BorderLayout(0, 16));
        JPanel heading = section(new BorderLayout(16, 0));
        JLabel title = AppStyle.label(knownSpellsView ? "Prepared spells" : "Sorcerer spells", 22, true);
        heading.add(title, BorderLayout.WEST);
        if (!knownSpellsView) {
            JButton mySpells = new JButton("My spells", new SpellBookIcon());
            AppStyle.roundButton(mySpells);
            mySpells.setToolTipText("Open your known cantrips and prepared spells");
            mySpells.addActionListener(event -> openKnownSpells());
            heading.add(mySpells, BorderLayout.EAST);
        }
        header.add(heading, BorderLayout.NORTH);
        JPanel info = section(new GridLayout(2, 1, 0, 6));
        info.add(character);
        info.add(counts);
        character.setFont(new Font("SansSerif", Font.PLAIN, 14));
        character.setForeground(AppStyle.MUTED);
        counts.setFont(new Font("SansSerif", Font.PLAIN, 14));
        header.add(info, BorderLayout.CENTER);
        JPanel filters = section(new GridLayout(2, 1, 0, 8));
        JPanel searchRow = section(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel searchLabel = new JLabel("Search");
        searchLabel.setLabelFor(search);
        search.getAccessibleContext().setAccessibleName("Search spells");
        search.setFont(new Font("SansSerif", Font.PLAIN, 15));
        search.setBorder(new AppStyle.PillBorder());
        searchRow.add(searchLabel);
        searchRow.add(search);
        spellLevel.getAccessibleContext().setAccessibleName("Filter by spell level");
        spellLevel.setFont(new Font("SansSerif", Font.PLAIN, 15));
        spellLevel.setBackground(Color.WHITE);
        spellLevel.setBorder(new AppStyle.PillBorder());
        spellLevel.setUI(new javax.swing.plaf.basic.BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton button = new JButton("▾");
                button.setBorder(null);
                button.setContentAreaFilled(false);
                button.setFocusPainted(false);
                button.setPreferredSize(new Dimension(24, 24));
                return button;
            }
        });
        searchRow.add(spellLevel);
        filters.add(searchRow);
        JPanel checks = section(new FlowLayout(FlowLayout.LEFT, 8, 0));
        availableOnly.setOpaque(false);
        selectedOnly.setOpaque(false);
        availableOnly.setFocusPainted(false);
        selectedOnly.setFocusPainted(false);
        checks.add(availableOnly);
        selectedOnly.setSelected(knownSpellsView);
        if (!knownSpellsView) { checks.add(selectedOnly); }
        checks.add(results);
        filters.add(checks);
        header.add(filters, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        table.setRowHeight(64);
        table.setFont(new Font("SansSerif", Font.PLAIN, 15));
        table.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 14));
        table.getTableHeader().setPreferredSize(new Dimension(800, 32));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(AppStyle.SOFT);
        table.setSelectionForeground(AppStyle.INK);
        table.getAccessibleContext().setAccessibleName("Sorcerer spell selection");
        table.getColumnModel().getColumn(0).setMaxWidth(75);
        table.getColumnModel().getColumn(1).setMaxWidth(90);
        table.getColumnModel().getColumn(2).setPreferredWidth(430);
        table.getColumnModel().getColumn(3).setPreferredWidth(195);
        table.getTableHeader().setBackground(Color.WHITE);
        table.getTableHeader().setForeground(AppStyle.MUTED);
        table.getColumnModel().getColumn(2).setCellRenderer(spellRenderer);
        table.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
            public void columnAdded(TableColumnModelEvent event) { }
            public void columnRemoved(TableColumnModelEvent event) { }
            public void columnMoved(TableColumnModelEvent event) { }
            public void columnSelectionChanged(ListSelectionEvent event) { }
            public void columnMarginChanged(javax.swing.event.ChangeEvent event) {
                spellRenderer.updateRowHeights(table);
            }
        });
        table.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent event) {
                spellRenderer.updateRowHeights(table);
            }
        });
        table.addPropertyChangeListener("font", event -> SwingUtilities.invokeLater(() -> spellRenderer.updateRowHeights(table)));
        if (knownSpellsView) { table.removeColumn(table.getColumnModel().getColumn(0)); }
        JScrollPane scroll = new JScrollPane(table);
        AppStyle.scrollPane(scroll);
        JPanel listCard = AppStyle.card(new BorderLayout());
        listCard.add(scroll, BorderLayout.CENTER);
        add(listCard, BorderLayout.CENTER);

        JPanel footer = section(new BorderLayout(0, 10));
        message.setForeground(new Color(155, 55, 30));
        if (knownSpellsView) { message.setForeground(AppStyle.MUTED); }
        footer.add(message, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent event) { refresh(); }
            public void removeUpdate(DocumentEvent event) { refresh(); }
            public void changedUpdate(DocumentEvent event) { refresh(); }
        });
        spellLevel.addActionListener(event -> refresh());
        availableOnly.addActionListener(event -> refresh());
        selectedOnly.addActionListener(event -> refresh());
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event)) { return; }
                int row = table.rowAtPoint(event.getPoint());
                int column = table.columnAtPoint(event.getPoint());
                // The checkbox only changes spell choices; clicking the spell opens its details.
                if (row >= 0 && column >= 0 && table.convertColumnIndexToModel(column) == 2) {
                    showDetailsAt(row);
                }
            }
        });
        table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "spellDetails");
        table.getActionMap().put("spellDetails", new AbstractAction() {
            public void actionPerformed(java.awt.event.ActionEvent event) { showDetailsAt(table.getSelectedRow()); }
        });
        selection.addChangeListener(changeListener);
        refresh();
    }

    public void refresh() {
        boolean rowsChanged = model.filter(search.getText(), spellLevel.getSelectedIndex() - 1,
                availableOnly.isSelected(), selectedOnly.isSelected());
        character.setText("Sorcerer level " + selection.level() + " · " + selection.subclass() + " · " + selection.species()
                + " · Highest spell level: " + selection.maxSpellLevel());
        counts.setText(selection.summary());
        results.setText(model.getRowCount() + " spells");
        // JTable resets heights when the list changes; a checkbox update preserves its existing layout.
        if (rowsChanged) { spellRenderer.updateRowHeights(table); }
        if (knownSpellsView) {
            message.setText(model.getRowCount() == 0
                    ? "No matching spells. Choose spells in the Sorcerer spellbook." : " ");
        }
    }

    private void characterChanged() {
        if (SwingUtilities.isEventDispatchThread()) { refresh(); }
        else { SwingUtilities.invokeLater(this::refresh); }
    }

    public void dispose() { selection.removeChangeListener(changeListener); }

    public SpellBookPanel createKnownSpells() { return new SpellBookPanel(selection, null, true); }
    public static SpellBookPanel knownSpells(SpellSelection selection) { return new SpellBookPanel(selection, null, true); }

    private void openKnownSpells() {
        if (knownSpellsWindow == null || !knownSpellsWindow.isDisplayable()) {
            SpellBookPanel known = createKnownSpells();
            knownSpellsWindow = new JDialog(SwingUtilities.getWindowAncestor(this),
                    "Prepared spells", Dialog.ModalityType.MODELESS);
            knownSpellsWindow.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
            knownSpellsWindow.setContentPane(known);
            knownSpellsWindow.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override public void windowClosed(java.awt.event.WindowEvent event) { known.dispose(); }
            });
            knownSpellsWindow.setSize(1000, 740);
            knownSpellsWindow.setMinimumSize(new Dimension(900, 600));
            knownSpellsWindow.setLocationRelativeTo(this);
        }
        AppStyle.applyAppearance(knownSpellsWindow);
        knownSpellsWindow.setVisible(true);
        knownSpellsWindow.toFront();
    }

    private void showDetailsAt(int row) {
        if (row >= 0 && row < model.getRowCount()) { detailOpener.accept(model.spellAt(table.convertRowIndexToModel(row))); }
    }

    private void openDetails(SpellCatalog.Spell spell) {
        JDialog details = new JDialog(SwingUtilities.getWindowAncestor(this), spell.name(), Dialog.ModalityType.MODELESS);
        details.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        details.setContentPane(new SpellDetailPanel(spell));
        details.setSize(780, 780);
        details.setMinimumSize(new Dimension(660, 620));
        details.setLocationRelativeTo(this);
        AppStyle.applyAppearance(details);
        details.setVisible(true);
    }

}
