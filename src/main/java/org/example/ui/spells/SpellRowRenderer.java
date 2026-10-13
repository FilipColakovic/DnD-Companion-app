package org.example.ui.spells;

import org.example.domain.spells.SpellDetails;
import org.example.ui.common.AppStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;
import java.awt.*;

/** Plain Swing text wraps consistently with the user's font scaling and column width. */
final class SpellRowRenderer extends JPanel implements TableCellRenderer {
    private static final int PADDING = 10;
    private static final int GAP = 5;
    private final JLabel title = AppStyle.label("", 15, true);
    private final JTextArea description = new JTextArea();
    private final SpellTableModel model;

    SpellRowRenderer(SpellTableModel model) {
        super(new BorderLayout(0, GAP));
        this.model = model;
        setBorder(new EmptyBorder(PADDING, PADDING, PADDING, PADDING));
        description.setFont(new Font("SansSerif", Font.PLAIN, 13));
        description.setForeground(AppStyle.MUTED);
        description.setEditable(false);
        description.setLineWrap(true);
        description.setWrapStyleWord(true);
        description.setOpaque(false);
        description.setBorder(null);
        description.setMargin(new Insets(0, 0, 0, 0));
        add(title, BorderLayout.NORTH);
        add(description, BorderLayout.CENTER);
    }

    @Override public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean focused, int row, int column) {
        var spell = model.spellAt(table.convertRowIndexToModel(row));
        title.setText(spell.name());
        description.setText(SpellDetails.forSpell(spell).shortDescription());
        setBackground(selected ? AppStyle.SOFT : AppStyle.SURFACE);
        setToolTipText(description.getText());
        // A renderer is reused without a normal component hierarchy/layout pass.
        setSize(table.getColumnModel().getColumn(column).getWidth(), table.getRowHeight(row));
        doLayout();
        return this;
    }

    int requiredHeight(int width) {
        description.setSize(Math.max(1, width - 2 * PADDING), Short.MAX_VALUE);
        return Math.max(64, 2 * PADDING + title.getPreferredSize().height + GAP
                + description.getPreferredSize().height);
    }

    void updateRowHeights(JTable table) {
        int column = table.convertColumnIndexToView(2);
        int width = table.getColumnModel().getColumn(column).getWidth();
        for (int row = 0; row < table.getRowCount(); row++) {
            getTableCellRendererComponent(table, table.getValueAt(row, column), false, false, row, column);
            int height = requiredHeight(width);
            if (table.getRowHeight(row) != height) { table.setRowHeight(row, height); }
        }
    }
}
