package org.example.ui.spells;

import org.example.domain.character.SorcererRules;
import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellSelection;
import org.example.ui.character.SorcererPanel;

import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JViewport;
import javax.swing.SwingUtilities;

public final class SpellBookChecks {
    public static void main(String[] args) throws Exception {
        int[] counts = {20, 22, 30, 21, 13, 13, 12, 8, 6, 5};
        for (int level = 0; level <= 9; level++) {
            int spellLevel = level;
            check(
                    SpellCatalog.all().stream()
                                    .filter(
                                            spell ->
                                                    spell.sorcererSpell()
                                                            && spell.level() == spellLevel)
                                    .count()
                            == counts[level],
                    "Complete PHB class list at spell level " + level);
        }
        check(
                SpellCatalog.all().stream().map(SpellCatalog.Spell::name).distinct().count()
                        == SpellCatalog.all().size(),
                "No duplicate spell names");
        for (var spell : SpellCatalog.all()) {
            var details = SpellDetails.forSpell(spell);
            check(
                    !details.description().isBlank()
                            && !details.school().isBlank()
                            && !details.castingTime().isBlank()
                            && !details.range().isBlank()
                            && !details.components().isBlank()
                            && !details.duration().isBlank(),
                    "Full details for " + spell.name());
            check(
                    !details.shortDescription().isBlank()
                            && details.shortDescription().length() <= 135,
                    "Short summary for " + spell.name());
        }
        check(
                SpellDetails.forSpell(spell("Chill Touch")).range().equalsIgnoreCase("touch"),
                "2024 Chill Touch range");
        check(
                SpellDetails.forSpell(spell("Sleep")).concentration(),
                "2024 Sleep requires concentration");
        check(
                SpellDetails.forSpell(spell("Witch Bolt")).description().contains("Bonus Action"),
                "2024 Witch Bolt uses Bonus Action");
        check(
                SpellDetails.forSpell(spell("Summon Dragon"))
                        .description()
                        .contains("Draconic Spirit"),
                "Summon includes spirit stats");
        for (var subclass : SorcererRules.Subclass.values()) {
            for (int level = 1; level <= 20; level++) {
                SpellSelection selection = new SpellSelection();
                selection.setCharacter(level, subclass);
                var stats = SorcererRules.calculate(level, 17, subclass);
                check(
                        selection.automaticCount()
                                == stats.subclassPreparedSpells()
                                        + (level >= 3 && subclass == SorcererRules.Subclass.ABERRANT
                                                ? 1
                                                : 0),
                        "Subclass grants match calculator");
                check(
                        SpellCatalog.forSubclass(subclass).stream()
                                .filter(selection::automatic)
                                .allMatch(selection::selected),
                        "Subclass grants auto-selected");
            }
        }
        SpellSelection selection = new SpellSelection();
        check(
                !selection.choose(spell("Fireball"), true).isEmpty(),
                "Level 1 can't choose Fireball");
        check(selection.choose(spell("Shield"), true).isEmpty(), "Choose Shield");
        check(selection.choose(spell("Magic Missile"), true).isEmpty(), "Choose Magic Missile");
        check(!selection.choose(spell("Sleep"), true).isEmpty(), "Enforce prepared spell limit");
        check(selection.choose(spell("Shield"), false).isEmpty(), "Remove selection");
        check(selection.choose(spell("Sleep"), true).isEmpty(), "Freed slot can be used");
        selection.setCharacter(5, SorcererRules.Subclass.ABERRANT);
        check(selection.choose(spell("Fireball"), true).isEmpty(), "Level 5 chooses Fireball");
        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC);
        check(
                selection.selected(spell("Fireball")) && !selection.available(spell("Fireball")),
                "Level decrease preserves dormant choice");
        selection.setCharacter(5, SorcererRules.Subclass.ABERRANT);
        check(
                selection.selected(spell("Fireball")) && selection.available(spell("Fireball")),
                "Level increase restores choice");
        check(
                !selection.choose(spell("Mind Sliver"), false).isEmpty(),
                "Cannot remove subclass cantrip");
        selection.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC);
        for (String name : new String[] {"Fire Bolt", "Light", "Mage Hand", "True Strike"}) {
            check(selection.choose(spell(name), true).isEmpty(), "Choose cantrip");
        }
        check(!selection.choose(spell("Friends"), true).isEmpty(), "Enforce cantrip limit");
        SwingUtilities.invokeAndWait(
                () -> {
                    // Drive the actual main panel's slider and subclass controls, with no manual
                    // refresh.
                    SorcererPanel main = new SorcererPanel();
                    SpellBookPanel linked = main.createSpellBook();
                    JTable linkedTable = find(linked, JTable.class);
                    check(
                            !linkedTable.isCellEditable(row(linkedTable, "Fireball"), 0),
                            "Fireball starts unavailable");
                    find(main, JSlider.class).setValue(5);
                    check(
                            linkedTable.isCellEditable(row(linkedTable, "Fireball"), 0),
                            "Main slider updates existing spell window");
                    main.selectSubclass(SorcererRules.Subclass.DRACONIC);
                    check(
                            Boolean.TRUE.equals(
                                    linkedTable.getValueAt(row(linkedTable, "Command"), 0)),
                            "Main subclass auto-prepares Command");
                    find(main, JSlider.class).setValue(1);
                    check(
                            !linkedTable.isCellEditable(row(linkedTable, "Fireball"), 0),
                            "Level decrease updates spell window");
                    check(
                            !Boolean.TRUE.equals(
                                    linkedTable.getValueAt(row(linkedTable, "Command"), 0)),
                            "Level decrease locks subclass spells");
                    linked.dispose();

                    SpellSelection character = new SpellSelection();
                    java.util.List<SpellCatalog.Spell> opened = new java.util.ArrayList<>();
                    SpellBookPanel panel = new SpellBookPanel(character, opened::add);
                    JTable table = find(panel, JTable.class);
                    check(
                            table.getRowCount() > 150,
                            "Subclass list adds spells to full class list");
                    JTextField search = find(panel, JTextField.class);
                    search.setText("fireball");
                    check(
                            table.getRowCount() == 2,
                            "Case-insensitive search includes Delayed Blast Fireball");
                    check(!table.isCellEditable(0, 0), "Unavailable spells cannot be chosen");
                    character.setCharacter(5, SorcererRules.Subclass.ABERRANT);
                    int row = row(table, "Fireball");
                    check(
                            table.isCellEditable(row, 0),
                            "Character changes notify the spell panel directly");
                    table.getModel().setValueAt(true, row, 0);
                    check(character.selected(spell("Fireball")), "Table checkbox updates choices");
                    SpellBookPanel reopened = new SpellBookPanel(character);
                    check(
                            Boolean.TRUE.equals(
                                    find(reopened, JTable.class)
                                            .getValueAt(
                                                    row(find(reopened, JTable.class), "Fireball"),
                                                    0)),
                            "Reopening keeps selection");
                    search.setText("no-such-spell");
                    check(table.getRowCount() == 0, "Empty search results handled");
                    search.setText("");
                    JComboBox<?> levels = find(panel, JComboBox.class);
                    levels.setSelectedIndex(4); // Level 3.
                    for (int i = 0; i < table.getRowCount(); i++) {
                        check(table.getValueAt(i, 1).equals("Level 3"), "Spell-level filter");
                    }
                    levels.setSelectedIndex(0);
                    character.setCharacter(20, SorcererRules.Subclass.ABERRANT);
                    panel.refresh();
                    panel.setSize(960, 700);
                    layout(panel);
                    checkDescriptionLayout(table);
                    SpellBookPanel known = panel.createKnownSpells();
                    JTable knownTable = find(known, JTable.class);
                    check(
                            knownTable.getColumnCount() == 3,
                            "Known spells window has no Choose column");
                    check(
                            knownTable.getRowCount() == character.automaticCount() + 1,
                            "Known spells contains the manual Fireball and automatic subclass"
                                + " grants");
                    for (int i = 0; i < knownTable.getRowCount(); i++) {
                        check(
                                Boolean.TRUE.equals(knownTable.getModel().getValueAt(i, 0)),
                                "Only selected spells in known list");
                        check(!knownTable.isCellEditable(i, 0), "Known spells list is read-only");
                    }
                    java.util.List<javax.swing.event.TableModelEvent> changes =
                            new java.util.ArrayList<>();
                    table.getModel().addTableModelListener(changes::add);
                    int chosenRow = row(table, "Fireball");
                    table.setRowSelectionInterval(chosenRow, chosenRow);
                    table.scrollRectToVisible(table.getCellRect(chosenRow, 0, true));
                    JViewport viewport = (JViewport) table.getParent();
                    Point scrollPosition = viewport.getViewPosition();
                    int[] heights =
                            java.util.stream.IntStream.range(0, table.getRowCount())
                                    .map(table::getRowHeight)
                                    .toArray();
                    check(table.editCellAt(chosenRow, 0), "Start actual checkbox editor");
                    ((JCheckBox) table.getEditorComponent()).doClick(0);
                    check(
                            !character.selected(spell("Fireball")),
                            "Actual checkbox editor commits deselection");
                    check(
                            changes.size() == 1
                                    && changes.get(0).getFirstRow() == chosenRow
                                    && changes.get(0).getLastRow() == chosenRow
                                    && changes.get(0).getColumn() == 0,
                            "Checkbox updates one cell without reloading the table");
                    check(
                            table.getSelectedRow() == chosenRow
                                    && viewport.getViewPosition().equals(scrollPosition),
                            "Checkbox preserves row selection and scroll position");
                    for (int i = 0; i < table.getRowCount(); i++) {
                        check(
                                table.getRowHeight(i) == heights[i],
                                "Checkbox preserves measured row heights");
                    }
                    check(
                            knownTable.getRowCount() == character.automaticCount(),
                            "Known list updates immediately on deselection");
                    table.getModel().setValueAt(true, chosenRow, 0);
                    check(
                            knownTable.getRowCount() == character.automaticCount() + 1,
                            "Known list updates immediately on selection");
                    character.setCharacter(1, SorcererRules.Subclass.WILD_MAGIC);
                    check(
                            knownTable.getRowCount() == 1,
                            "Known list updates when subclass grants disappear");
                    check(
                            knownTable.getModel().getValueAt(0, 2).equals("Fireball")
                                    && knownTable
                                            .getModel()
                                            .getValueAt(0, 3)
                                            .equals("Needs higher Sorcerer level"),
                            "Dormant choice is retained with its availability shown");
                    character.setCharacter(20, SorcererRules.Subclass.ABERRANT);
                    known.setSize(960, 700);
                    layout(known);
                    BufferedImage knownImage =
                            new BufferedImage(960, 700, BufferedImage.TYPE_INT_RGB);
                    Graphics2D knownGraphics = knownImage.createGraphics();
                    known.printAll(knownGraphics);
                    knownGraphics.dispose();
                    try {
                        ImageIO.write(
                                knownImage, "png", new File("target/known-spells-preview.png"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                    known.dispose();
                    if (!GraphicsEnvironment.isHeadless()) {
                        JFrame host = new JFrame();
                        try {
                            host.setContentPane(panel);
                            host.pack();
                            JButton mySpells = findButton(panel, "My spells");
                            mySpells.doClick(0);
                            Window[] windows = host.getOwnedWindows();
                            check(
                                    windows.length == 1 && windows[0].isVisible(),
                                    "My spells button opens a separate window");
                            check(
                                    org.example.ui.common.AppStyle.windowContent(
                                                    (JDialog) windows[0])
                                            instanceof SpellBookPanel,
                                    "My spells dialog uses the matching spellbook style");
                            mySpells.doClick(0);
                            check(
                                    host.getOwnedWindows().length == 1,
                                    "My spells button reuses its existing window");
                        } finally {
                            host.dispose();
                        }
                    }
                    int fireballRow = row(table, "Fireball");
                    Rectangle spellCell = table.getCellRect(fireballRow, 2, true);
                    table.dispatchEvent(
                            new java.awt.event.MouseEvent(
                                    table,
                                    java.awt.event.MouseEvent.MOUSE_CLICKED,
                                    System.currentTimeMillis(),
                                    0,
                                    spellCell.x + 10,
                                    spellCell.y + 10,
                                    1,
                                    false,
                                    java.awt.event.MouseEvent.BUTTON1));
                    check(
                            opened.size() == 1 && opened.get(0).name().equals("Fireball"),
                            "Single-click opens the correct spell details");
                    Rectangle checkboxCell = table.getCellRect(fireballRow, 0, true);
                    table.dispatchEvent(
                            new java.awt.event.MouseEvent(
                                    table,
                                    java.awt.event.MouseEvent.MOUSE_CLICKED,
                                    System.currentTimeMillis(),
                                    0,
                                    checkboxCell.x + 10,
                                    checkboxCell.y + 10,
                                    1,
                                    false,
                                    java.awt.event.MouseEvent.BUTTON1));
                    check(opened.size() == 1, "Checkbox does not open details");
                    levels.setSelectedIndex(4);
                    panel.setSize(960, 700);
                    layout(panel);
                    ((JViewport) table.getParent()).setViewPosition(new Point(0, 0));
                    BufferedImage image = new BufferedImage(960, 700, BufferedImage.TYPE_INT_RGB);
                    Graphics2D g = image.createGraphics();
                    panel.printAll(g);
                    g.dispose();
                    try {
                        ImageIO.write(image, "png", new File("target/spellbook-preview.png"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                    SpellDetailPanel detailPanel = new SpellDetailPanel(spell("Fireball"));
                    detailPanel.setSize(744, 740);
                    layout(detailPanel);
                    BufferedImage detailImage =
                            new BufferedImage(744, 740, BufferedImage.TYPE_INT_RGB);
                    Graphics2D detailGraphics = detailImage.createGraphics();
                    detailPanel.printAll(detailGraphics);
                    detailGraphics.dispose();
                    try {
                        ImageIO.write(
                                detailImage, "png", new File("target/spell-detail-preview.png"));
                    } catch (java.io.IOException e) {
                        throw new RuntimeException(e);
                    }
                    panel.dispose();
                    reopened.dispose();
                });
        System.out.println(
                "Passed: catalog and full details, selection limits, search, filters, single-click"
                    + " details, and live main-window synchronization.");
    }

    private static SpellCatalog.Spell spell(String name) {
        return SpellCatalog.all().stream()
                .filter(spell -> spell.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    private static JButton findButton(Container root, String text) {
        for (Component component : root.getComponents()) {
            if (component instanceof JButton button && button.getText().equals(text)) {
                return button;
            }
            if (component instanceof Container nested) {
                JButton result = findButton(nested, text);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void checkDescriptionLayout(JTable table) {
        var column = table.getColumnModel().getColumn(2);
        int originalWidth = column.getWidth();
        SpellRowRenderer renderer = (SpellRowRenderer) column.getCellRenderer();
        column.setWidth(240);
        checkRowsFit(table, renderer);
        int narrowHeight = table.getRowHeight(row(table, "Clairvoyance"));
        column.setWidth(500);
        checkRowsFit(table, renderer);
        check(
                table.getRowHeight(row(table, "Clairvoyance")) < narrowHeight,
                "Wrapped rows shrink when the spell column widens");
        column.setWidth(240);
        JTextArea text = find(renderer, JTextArea.class);
        Font originalFont = text.getFont();
        text.setFont(originalFont.deriveFont(18f));
        renderer.updateRowHeights(table);
        checkRowsFit(table, renderer);
        text.setFont(originalFont);
        column.setWidth(originalWidth);
        renderer.updateRowHeights(table);
    }

    private static void checkRowsFit(JTable table, SpellRowRenderer renderer) {
        for (int row = 0; row < table.getRowCount(); row++) {
            Component cell =
                    renderer.getTableCellRendererComponent(
                            table, table.getValueAt(row, 2), false, false, row, 2);
            cell.setSize(table.getColumnModel().getColumn(2).getWidth(), table.getRowHeight(row));
            layout((Container) cell);
            JTextArea text = find((Container) cell, JTextArea.class);
            JLabel title = find((Container) cell, JLabel.class);
            check(
                    title.getHeight() > 0 && title.getY() + title.getHeight() <= text.getY(),
                    "Spell name remains visible above the summary");
            check(
                    text.getHeight() >= text.getPreferredSize().height,
                    "Complete description visible for " + table.getValueAt(row, 2));
        }
    }

    private static int row(JTable table, String name) {
        for (int i = 0; i < table.getRowCount(); i++) {
            if (name.equals(table.getValueAt(i, 2))) {
                return i;
            }
        }
        throw new AssertionError("Missing " + name);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static <T extends Component> T find(Container root, Class<T> type) {
        for (Component child : root.getComponents()) {
            if (type.isInstance(child)) {
                return type.cast(child);
            }
            if (child instanceof Container nested) {
                T result = find(nested, type);
                if (result != null) {
                    return result;
                }
            }
        }
        return null;
    }

    private static void layout(Container root) {
        root.doLayout();
        for (Component child : root.getComponents()) {
            if (child instanceof Container nested) {
                layout(nested);
            }
        }
    }
}
