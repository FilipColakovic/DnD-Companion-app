package org.example.ui;

import org.example.domain.character.CharacterState;
import org.example.domain.character.PlaySession;
import org.example.ui.character.MetamagicPanel;
import org.example.ui.character.FeatsPanel;
import org.example.ui.character.SorcererPanel;
import org.example.ui.character.CharacterSheetPanel;
import org.example.ui.common.AppStyle;
import org.example.ui.common.ArcaneIcon;
import org.example.ui.common.LiveView;
import org.example.ui.spells.CastingPanel;
import org.example.ui.spells.SpellLibraryPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Desktop shell: every page uses the same character, selection, resource model and checkpoint. */
public final class GrimoirePanel extends JPanel implements LiveView {
    public static final String COMBAT = "Cast & Combat",
            METAMAGIC = "Metamagic", FEATS = "Feats", LIBRARY = "Spell Library", CHARACTER = "Character";
    private final SorcererPanel character;
    private final CastingPanel combat;
    private final SpellLibraryPanel library;
    private final MetamagicPanel metamagic;
    private final FeatsPanel feats;
    private final CharacterSheetPanel sheetEditor;
    private Consumer<CharacterState> saveAction;
    private boolean restoreConfigured;
    private final CardLayout pageLayout = new CardLayout();
    private final JPanel pages = AppStyle.section(pageLayout);
    private final JScrollPane sidebarScroll;
    private final Map<String,JButton> navigation = new LinkedHashMap<>();
    private final JLabel pendingDecisions=AppStyle.label("!",17,true);

    private final org.example.ui.common.CharacterSummary summary;
    private final JCheckBox lock = new JCheckBox("Lock saved character");
    private final JCheckBox dark = new JCheckBox("Dark theme");
    private final JComboBox<Integer> size = new JComboBox<>(new Integer[]{12,14,16,18});
    private final JButton restore;
    private final Runnable listener = this::refresh;
    private boolean updating, disposed;
    private String currentPage = COMBAT;

    public GrimoirePanel(CharacterState state) {
        super(new BorderLayout());
        character = new SorcererPanel(state); character.setEmbedded(true);
        combat = new CastingPanel(playSession(), () -> snapshot().charisma());
        combat.setSpellbookAction(() -> showPage(LIBRARY));
        library = new SpellLibraryPanel(playSession(),this::snapshot);
        metamagic = new MetamagicPanel(playSession());
        feats = new FeatsPanel(playSession(),this::snapshot);
        feats.setCharacterAction(()->showPage(CHARACTER));
        JPanel sidebar = new Sidebar();
        sidebar.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,2,2,AppStyle.LINE),new EmptyBorder(18,16,18,16)));
        JPanel sideTop = AppStyle.section(new BorderLayout(0,16));
        summary=new org.example.ui.common.CharacterSummary(this::snapshot,playSession());
        sideTop.add(summary,BorderLayout.NORTH);
        JPanel nav = AppStyle.section(new GridLayout(0,1,0,7));
        addNavigation(nav,COMBAT,ArcaneIcon.Kind.BOLT,"Cast prepared spells, roll dice and track resources");
        addNavigation(nav,METAMAGIC,ArcaneIcon.Kind.SPARK,"Choose 2024 Metamagic options and inspect compatible spells");
        addNavigation(nav,FEATS,ArcaneIcon.Kind.SHIELD,"Browse PHB 2024 feats, record choices and preview milestones");
        addNavigation(nav,LIBRARY,ArcaneIcon.Kind.LIBRARY,"Search the Sorcerer spell list and choose spells");
        addNavigation(nav,CHARACTER,ArcaneIcon.Kind.PERSON,"Edit name, level, Charisma, subclass and species");
        pendingDecisions.setName("pending-character-decisions");pendingDecisions.getAccessibleContext().setAccessibleName("Pending character decisions");AppStyle.setColorRole(pendingDecisions,"error");
        pendingDecisions.setBorder(new EmptyBorder(0,8,0,0));pendingDecisions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        pendingDecisions.addMouseListener(new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent event){if(SwingUtilities.isLeftMouseButton(event))showPage(CHARACTER);}});
        JButton characterNavigation=navigation.get(CHARACTER);characterNavigation.setLayout(new BorderLayout());characterNavigation.add(pendingDecisions,BorderLayout.EAST);
        nav.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(2,0,0,0,AppStyle.LINE),new EmptyBorder(14,0,0,0)));
        sideTop.add(nav,BorderLayout.CENTER); sidebar.add(sideTop,BorderLayout.NORTH);

        JPanel bottom = AppStyle.section(new BorderLayout(0,18));
        JPanel preferences = AppStyle.section(new GridBagLayout());
        AppStyle.roundButton(lock); lock.setName("save-lock");
        lock.setFont(new Font("SansSerif",Font.PLAIN,13));
        lock.setToolTipText("Save the current character and protect it while experimenting");
        lock.addActionListener(e -> { if (!updating) { character.requestSaveLock(lock.isSelected()); refresh(); } }); preferenceRow(preferences,lock);
        restore = AppStyle.button("Restore saved", () -> character.restoreSaved());
        restore.setName("restore-saved"); restore.setToolTipText("Return inputs, spell choices and resources to the protected checkpoint"); preferenceRow(preferences,restore);
        AppStyle.roundButton(dark); dark.setName("dark-mode"); dark.setFont(new Font("SansSerif",Font.PLAIN,13));
        dark.setToolTipText("Use charcoal or cream Bauhaus surfaces in every open window");
        dark.addActionListener(e -> { if (!updating) { playSession().appearance(dark.isSelected(),(Integer)size.getSelectedItem()); } }); preferenceRow(preferences,dark);
        JPanel fontRow = AppStyle.section(new BorderLayout(10,0)); fontRow.add(AppStyle.label("Text size",12,false),BorderLayout.CENTER);
        size.setName("text-size"); size.setToolTipText("Set the base text size across all windows"); fontRow.add(size,BorderLayout.EAST);
        size.setPreferredSize(new Dimension(88,36));
        size.addActionListener(e -> { if (!updating) { playSession().appearance(dark.isSelected(),(Integer)size.getSelectedItem()); } }); preferenceRow(preferences,fontRow);
        bottom.add(preferences,BorderLayout.CENTER); sidebar.add(bottom,BorderLayout.SOUTH);
        sidebarScroll = new JScrollPane(sidebar); AppStyle.scrollPane(sidebarScroll);
        sidebarScroll.setPreferredSize(new Dimension(248,0)); sidebarScroll.setMinimumSize(new Dimension(248,0));
        add(sidebarScroll,BorderLayout.WEST);

        JPanel workspace = AppStyle.section(new BorderLayout());
        sheetEditor=new CharacterSheetPanel(this);
        pages.add(combat,COMBAT); pages.add(metamagic,METAMAGIC); pages.add(feats,FEATS); pages.add(library,LIBRARY); pages.add(sheetEditor,CHARACTER);
        workspace.add(pages,BorderLayout.CENTER); add(workspace,BorderLayout.CENTER);
        character.addPropertyChangeListener("saveStatus", e -> refresh());
        character.addPropertyChangeListener("characterState", e -> refresh());
        playSession().addListener(listener); playSession().selection().addChangeListener(listener);
        showPage(COMBAT); refresh();
    }

    private void addNavigation(JPanel panel,String page,ArcaneIcon.Kind icon,String tooltip) {
        JButton button = AppStyle.navigationButton(page,new ArcaneIcon(icon,20),() -> showPage(page)); button.setName("nav-"+page);
        button.setFont(new Font("SansSerif",Font.PLAIN,14)); button.setToolTipText(tooltip);
        navigation.put(page,button); panel.add(button);
    }
    public void showPage(String page) {
        if (!navigation.containsKey(page)) { throw new IllegalArgumentException("Unknown page: "+page); }
        currentPage = page; pageLayout.show(pages,page);
        navigation.forEach((title,button) -> button.setSelected(title.equals(page)));
        AppStyle.applyAppearance(this); revalidate(); repaint();
    }
    public void refresh() {
        if (updating || disposed) { return; }
        updating = true;
        try {
            CharacterState state = snapshot();
            int remaining=playSession().pendingDecisions();pendingDecisions.setVisible(remaining>0);
            pendingDecisions.setToolTipText(remaining+" pending character decision"+(remaining==1?"":"s"));
            pendingDecisions.getAccessibleContext().setAccessibleDescription(pendingDecisions.getToolTipText());
            summary.refresh();
            lock.setSelected(character.isSaveLocked()); restore.setEnabled(character.isSaveLocked());
            dark.setSelected(state.play().dark()); size.setSelectedItem(state.play().textSize());
            sheetEditor.refresh();
            AppStyle.applyAppearance(this);
            int sidebarWidth = 280 + Math.max(0,state.play().textSize()-14)*11;
            sidebarScroll.setPreferredSize(new Dimension(sidebarWidth,0));
            revalidate();
        } finally { updating = false; }
    }
    public void showPreparedSpells(){library.showPreparedOnly();showPage(LIBRARY);}
    public void showBackgrounds(){showPage(FEATS);feats.showBackgrounds();}
    public String currentPage() { return currentPage; }
    public void inspectFeat(String name){showPage(FEATS);feats.inspect(name);}
    public boolean canSaveNow(){return saveAction!=null && !isSaveLocked();}
    public boolean canRestoreSaved(){return restoreConfigured;}
    public void saveNow(){if(canSaveNow())saveAction.accept(snapshot());}
    public void setSaveAction(Consumer<CharacterState> action){saveAction=java.util.Objects.requireNonNull(action);sheetEditor.refresh();}
    public SorcererPanel characterPanel() { return character; }
    public CastingPanel combatPanel() { return combat; }
    public PlaySession playSession() { return character.playSession(); }
    public CharacterState snapshot() { return character.snapshot(); }
    public void restore(CharacterState state) { character.restore(state); refresh(); combat.recordEvent("Saved character restored."); }
    public boolean isSaveLocked() { return character.isSaveLocked(); }
    public void setSaveLocked(boolean locked) { character.setSaveLocked(locked); refresh(); }
    public void markSaved() { character.markSaved(); refresh(); }
    public void setSaveActions(Consumer<Boolean> onLock,Runnable onRestore) {
        restoreConfigured=true;
        character.setSaveActions(locked -> { onLock.accept(locked); refresh(); }, () -> { onRestore.run(); refresh(); });
        sheetEditor.refresh();
    }
    @Override public void dispose() {
        if (disposed) { return; } disposed = true;
        playSession().removeListener(listener); playSession().selection().removeChangeListener(listener);
        combat.dispose(); library.dispose(); metamagic.dispose(); feats.dispose(); sheetEditor.dispose();
    }
    private static void preferenceRow(JPanel panel,JComponent component) {
        GridBagConstraints c = new GridBagConstraints(); c.gridx=0; c.gridy=panel.getComponentCount();
        c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL; c.insets=new Insets(0,0,10,0);
        panel.add(component,c);
    }
    private static final class Sidebar extends JPanel implements Scrollable {
        Sidebar() { super(new BorderLayout(0,24)); putClientProperty("surfaceRole","low"); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return getParent()!=null && getParent().getHeight()>=getPreferredSize().height; }
        @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction) { return 24; }
        @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction) { return Math.max(24,visible.height-24); }
    }
}
