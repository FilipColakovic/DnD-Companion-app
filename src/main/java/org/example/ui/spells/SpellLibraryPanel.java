package org.example.ui.spells;

import org.example.domain.character.*;
import org.example.domain.spells.*;
import org.example.ui.common.AppStyle;
import org.example.ui.common.LiveView;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.List;
import java.util.*;
import java.util.function.Supplier;

/** Compact spell cards and a live codex inspector sharing the character's existing selection rules. */
public final class SpellLibraryPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final SpellSelection selection;
    private final Supplier<CharacterState> character;
    private final JTextArea selectionSummary=AppStyle.text("");
    private final JTextField search=new JTextField(20);
    private final JCheckBox available=new JCheckBox("Available"),chosen=new JCheckBox("Prepared only"),
            concentration=new JCheckBox("Concentration"),ritual=new JCheckBox("Ritual"),psionic=new JCheckBox("Psionic");
    private final Map<Integer,JToggleButton> tiers=new LinkedHashMap<>();
    private final Map<String,JToggleButton> schools=new LinkedHashMap<>();
    private final Map<String,SpellCard> cards=new HashMap<>();
    private final SpellGrid list=new SpellGrid();
    private final Column inspector=new Column();
    private final JScrollPane listScroll=scroll(list),inspectorScroll=scroll(inspector);
    private final JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,listScroll,inspectorScroll);
    private final JLabel cataloged=AppStyle.mono("",11),shown=AppStyle.mono("",11),message=AppStyle.label(" ",12,false);
    private final Runnable listener=this::changed;
    private List<SpellCatalog.Spell> visible=List.of(),listedSpells=List.of();
    private int layoutWidth=-1;
    private final Set<Integer> selectedTiers=new HashSet<>();
    private final Set<String> selectedSchools=new HashSet<>();
    private SpellCatalog.Spell focused;
    private boolean updating,disposed;
    private String inspectorStamp="";

    public SpellLibraryPanel(PlaySession session,Supplier<CharacterState> character) {
        super(new BorderLayout(0,12));this.session=session;selection=session.selection();this.character=character;
        setName("spell-library-panel");getAccessibleContext().setAccessibleName("Spell Library");
        putClientProperty("surfaceRole","background");setBorder(new EmptyBorder(14,14,14,14));
        JPanel filters=AppStyle.card(new GridBagLayout());filters.putClientProperty("surfaceRole","low");filters.setBorder(new EmptyBorder(12,12,16,16));
        row(filters,selectionSummary);
        JPanel controls=new Wrap();controls.add(AppStyle.mono("SEARCH",10));search.getAccessibleContext().setAccessibleName("Search library spells");controls.add(search);
        controls.add(filterButton("Clear",()->search.setText("")));
        for(JCheckBox check:List.of(available,chosen,concentration,ritual,psionic)) {AppStyle.roundButton(check);check.addActionListener(e->filterChanged());controls.add(check);}
        row(filters,controls);
        JPanel tierRow=new Wrap();tierRow.add(AppStyle.mono("TIER",10));
        for(int level=-1;level<=9;level++) {int value=level;JToggleButton button=toggle("",()->{if(value<0)selectedTiers.clear();else if(!selectedTiers.remove(value))selectedTiers.add(value);filterChanged();});button.getAccessibleContext().setAccessibleName("Library tier "+level);tiers.put(level,button);tierRow.add(button);}
        tiers.get(-1).setSelected(true);row(filters,tierRow);
        JPanel schoolRow=new Wrap();schoolRow.add(AppStyle.mono("SCHOOL",10));
        for(String name:List.of("All","Abjuration","Conjuration","Divination","Enchantment","Evocation","Illusion","Necromancy","Transmutation")) {
            JToggleButton button=toggle(name.toUpperCase(Locale.ROOT),()->{if(name.equals("All"))selectedSchools.clear();else if(!selectedSchools.remove(name))selectedSchools.add(name);filterChanged();});button.getAccessibleContext().setAccessibleName("Library school "+name);schools.put(name,button);schoolRow.add(button);
        }
        schools.get("All").setSelected(true);row(filters,schoolRow);
        add(filters,BorderLayout.NORTH);
        JPanel left=AppStyle.section(new BorderLayout(0,8));left.add(cataloged,BorderLayout.NORTH);left.add(listScroll,BorderLayout.CENTER);
        left.add(shown,BorderLayout.SOUTH);
        split.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI(){
            @Override public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider(){return new javax.swing.plaf.basic.BasicSplitPaneDivider(this){@Override public void paint(Graphics g){g.setColor(AppStyle.BACKGROUND);g.fillRect(0,0,getWidth(),getHeight());}};}
        });split.setLeftComponent(left);split.setResizeWeight(.6);split.setBorder(null);split.setDividerSize(10);add(split,BorderLayout.CENTER);add(message,BorderLayout.SOUTH);
        search.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){filterChanged();}public void removeUpdate(DocumentEvent e){filterChanged();}public void changedUpdate(DocumentEvent e){filterChanged();}});
        session.addListener(listener);selection.addChangeListener(listener);refresh();
    }
    @Override public void doLayout(){
        int orientation=getWidth()<850?JSplitPane.VERTICAL_SPLIT:JSplitPane.HORIZONTAL_SPLIT;
        if(split.getOrientation()!=orientation){split.setOrientation(orientation);layoutWidth=-1;}
        super.doLayout();
        if(layoutWidth!=split.getWidth()){layoutWidth=split.getWidth();split.setDividerLocation(.6);}
        split.doLayout();
    }
    public void showPreparedOnly(){
        updating=true;
        try {search.setText("");selectedTiers.clear();selectedSchools.clear();for(JCheckBox check:List.of(available,concentration,ritual,psionic))check.setSelected(false);chosen.setSelected(true);}
        finally {updating=false;}
        filterChanged();
    }
    private void filterChanged(){if(updating)return;refresh();listScroll.getViewport().setViewPosition(new Point());}
    private void changed(){if(SwingUtilities.isEventDispatchThread())refresh();else SwingUtilities.invokeLater(this::refresh);}
    private boolean matches(SpellCatalog.Spell spell) {
        SpellDetails d=SpellDetails.forSpell(spell);
        return spell.name().toLowerCase(Locale.ROOT).contains(search.getText().strip().toLowerCase(Locale.ROOT))
                && (!available.isSelected()||selection.available(spell)) && (!chosen.isSelected()||selection.selected(spell))
                && (!concentration.isSelected()||d.concentration()) && (!ritual.isSelected()||d.ritual())
                && (!psionic.isSelected()||selection.subclass()==SorcererRules.Subclass.ABERRANT && SpellCatalog.unlockLevel(spell,selection.subclass())>0);
    }
    public void refresh(){
        if(updating||disposed)return;updating=true;
        try {
            var candidates=selection.catalog().stream().filter(this::matches).toList();
            tiers.forEach((level,button)->{long count=candidates.stream().filter(s->level<0||s.level()==level).count();button.setText((level<0?"ALL":level==0?"CANTRIP":level==1?"1ST":level==2?"2ND":level==3?"3RD":level+"TH")+" ("+count+")");});
            tiers.forEach((level,button)->button.setSelected(level<0?selectedTiers.isEmpty():selectedTiers.contains(level)));
            schools.forEach((name,button)->button.setSelected(name.equals("All")?selectedSchools.isEmpty():selectedSchools.contains(name)));
            available.setText("Lv "+selection.level()+" available");
            psionic.setToolTipText("Aberrant Sorcery's subclass spell grants, including Mind Sliver");
            Comparator<SpellCatalog.Spell> order=Comparator.comparingInt(SpellCatalog.Spell::level).thenComparing(SpellCatalog.Spell::name);
            visible=candidates.stream().filter(s->selectedTiers.isEmpty()||selectedTiers.contains(s.level())).filter(s->selectedSchools.isEmpty()||selectedSchools.contains(SpellDetails.forSpell(s).school())).sorted(order).toList();
            List<SpellCatalog.Spell> current=visible;
            if(!current.equals(listedSpells)) {
                listedSpells=List.copyOf(current);list.removeAll();
                for(var spell:current)list.add(cards.computeIfAbsent(spell.name(),name->new SpellCard(spell)));
                if(current.isEmpty())list.add(AppStyle.text("No spells match these filters."));
                list.revalidate();
            }
            for(var spell:current)cards.get(spell.name()).update();
            if(focused==null||!selection.catalog().contains(focused)||chosen.isSelected()&&!selection.selected(focused))focused=visible.isEmpty()?null:visible.getFirst();
            selectionSummary.setText(selection.summary());
            cataloged.setText("CATALOGED: "+visible.size()+" ENTRIES // "+character.get().name()+" LV "+selection.level());
            shown.setText(visible.size()+" MATCHING SPELLS");refreshInspector();AppStyle.applyAppearance(this);revalidate();repaint();
        }finally{updating=false;}
    }
    public int visibleSpellCount(){return visible.size();}
    public String inspectedSpell(){return focused==null?"":focused.name();}
    public void inspect(String name){focused=selection.catalog().stream().filter(s->s.name().equals(name)).findFirst().orElseThrow();inspectorScroll.getViewport().setViewPosition(new Point());refreshInspector();cards.values().forEach(SpellCard::update);AppStyle.applyAppearance(this);}
    private void choose(SpellCatalog.Spell spell){String error=selection.choose(spell,!selection.selected(spell));message.setText(error.isEmpty()?spell.name()+" selection updated.":error);inspect(spell.name());}
    private JPanel selectionActions(SpellCatalog.Spell spell){
        JPanel actions=AppStyle.section(new FlowLayout(FlowLayout.RIGHT,5,0));boolean selected=selection.selected(spell),automatic=selection.automatic(spell),active=selection.available(spell);
        JButton select=AppStyle.compactButton(automatic?"Always prepared":selected?"Unlearn":"Learn",()->choose(spell));
        select.setToolTipText(automatic?"Automatically prepared by your subclass or species":selected?"Remove this manual spell choice":active?"Add this spell to your character":"This spell requires a higher level");
        select.getAccessibleContext().setAccessibleName("Choose "+spell.name());select.setEnabled(!automatic&&(selected||active));if(!selected&&active)select.putClientProperty("tactileRole","yellow");actions.add(select);return actions;
    }
    private final class SpellCard extends JPanel {
        String stamp="";
        final java.awt.event.MouseAdapter inspectClick=new java.awt.event.MouseAdapter(){@Override public void mouseClicked(java.awt.event.MouseEvent event){if(SwingUtilities.isLeftMouseButton(event))inspect(spell.name());}};
        final SpellCatalog.Spell spell;final JPanel actions=AppStyle.section(new BorderLayout()),badges=new Wrap();
        SpellCard(SpellCatalog.Spell spell){super(new BorderLayout());this.spell=spell;getAccessibleContext().setAccessibleName("Spell card "+spell.name());setOpaque(false);JPanel card=AppStyle.card(new GridBagLayout());card.putClientProperty("surfaceRole","surface");card.setBorder(new EmptyBorder(10,10,14,14));add(card);
            JPanel heading=AppStyle.section(new BorderLayout(8,0));
            JPanel tile=AppStyle.card(new BorderLayout());tile.putClientProperty("surfaceRole",spell.level()>0?"slotAvailable":"low");tile.putClientProperty("shadowOffset",2);tile.setBorder(new EmptyBorder(3,3,5,5));tile.setPreferredSize(new Dimension(44,44));
            JLabel level=AppStyle.mono(""+spell.level(),20);level.setHorizontalAlignment(SwingConstants.CENTER);AppStyle.setColorRole(level,spell.level()>0?"badgeText":"ink");tile.add(level);
            JPanel tileHolder=AppStyle.section(new FlowLayout(FlowLayout.LEFT,0,0));tileHolder.add(tile);heading.add(tileHolder,BorderLayout.WEST);
            JButton name=new SpellTitle(spell.name().toUpperCase(Locale.ROOT),()->inspect(spell.name()));name.getAccessibleContext().setAccessibleName("Inspect "+spell.name());heading.add(name,BorderLayout.CENTER);heading.add(actions,BorderLayout.EAST);row(card,heading);
            row(card,badges);
            var d=SpellDetails.forSpell(spell);String time=d.castingTime().split(":",2)[0];
            JTextArea metadata=wrapped((time+"  //  "+d.range()+"  //  "+d.components().replaceAll("\\s*\\(.*\\)","")).toUpperCase(Locale.ROOT));metadata.setFont(new Font("Consolas",Font.BOLD,12));AppStyle.setColorRole(metadata,"accent");metadata.setToolTipText("Casting time: "+d.castingTime()+" | Range: "+d.range()+" | Components: "+d.components());
            JPanel strip=AppStyle.surfacePanel(new BorderLayout(),"low");strip.setBorder(new EmptyBorder(4,6,4,6));strip.add(metadata);row(card,strip);
            bindInspection(this);
        }
        private void bindInspection(Component component){
            if(component instanceof JComponent child && !(component instanceof AbstractButton button && button.isEnabled()) && !Boolean.TRUE.equals(child.getClientProperty("spellCardInspection"))){
                child.putClientProperty("spellCardInspection",true);child.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));child.addMouseListener(inspectClick);
            }
            if(component instanceof Container container)for(Component child:container.getComponents())bindInspection(child);
        }
        void update(){String next=selection.selected(spell)+":"+selection.automatic(spell)+":"+selection.available(spell)+":"+spell.name().equals(inspectedSpell());if(next.equals(stamp))return;stamp=next;actions.removeAll();actions.add(selectionActions(spell));badges.removeAll();var d=SpellDetails.forSpell(spell);
            if(selection.selected(spell))badges.add(coloredTag(selection.automatic(spell)?"GRANTED":spell.level()==0?"KNOWN":"PREPARED","badgeDark"));
            if(selection.subclass()==SorcererRules.Subclass.ABERRANT && SpellCatalog.unlockLevel(spell,selection.subclass())>0)badges.add(coloredTag("PSIONIC ORIGIN","badgeBlue"));
            JLabel schoolTag=cardTag(d.school(),false);if(d.school().equals("Evocation"))AppStyle.setColorRole(schoolTag,"error");badges.add(schoolTag);
            if(d.concentration())badges.add(coloredTag("CONCENTRATION","badgeRed"));
            if(d.castingTime().startsWith("Reaction"))badges.add(coloredTag("REACTION","badgeRed"));
            if(d.castingTime().startsWith("Bonus Action"))badges.add(coloredTag("BONUS ACTION","badgeRed"));
            if(spell.level()==0)badges.add(cardTag("CANTRIP",true));
            if(d.ritual())badges.add(coloredTag("RITUAL","badgeBlue"));if(!selection.available(spell))badges.add(cardTag("LOCKED",false));
            setToolTipText(spell.name().equals(inspectedSpell())?"Selected in codex inspector": "Click anywhere on this card to inspect");bindInspection(this);revalidate();repaint();}
    }
    private void refreshInspector(){
        String next=(focused==null?"":focused.name())+":"+selection.level()+":"+character.get().charisma()+":"+session.metamagic()+":"+selection.species()+":"+session.sheet().values()+":"+(focused!=null&&selection.selected(focused))+":"+(focused!=null&&selection.automatic(focused));
        if(next.equals(inspectorStamp))return;inspectorStamp=next;
        Point position=inspectorScroll.getViewport().getViewPosition();inspector.removeAll();
        JPanel codex=AppStyle.card(new GridBagLayout());codex.putClientProperty("surfaceRole","lowest");row(codex,tag("SPELL CODEX INSPECTOR // PHB 2024",true));
        if(focused==null){row(codex,wrapped("Select a spell to inspect its rules."));row(inspector,codex);glue(inspector);return;}
        var spell=focused;var d=SpellDetails.forSpell(spell);row(codex,AppStyle.label(spell.name().toUpperCase(Locale.ROOT),23,true));row(codex,tag(spell.levelName()+" // "+d.school(),true));
        JPanel stats=AppStyle.section(new GridLayout(0,2,8,8));JPanel castingSpeed=block("CASTING SPEED",wrapped(d.castingTime().contains(":")?d.castingTime().substring(0,d.castingTime().indexOf(":")):d.castingTime()));castingSpeed.setToolTipText(d.castingTime());stats.add(castingSpeed);stats.add(block("RANGE / AREA",wrapped(d.range())));stats.add(block("COMPONENTS",wrapped(d.components())));stats.add(block("DURATION",wrapped(d.duration())));
        var state=character.get();var calculated=SorcererRules.calculate(selection.level(),state.charisma(),selection.subclass());
        int attack=calculated.proficiencyBonus()+session.spellcastingModifier(spell,state.charisma());
        stats.add(block("SPELLCASTING",wrapped("Save DC "+(8+attack)+" // Attack "+String.format("%+d",attack)+" // "+session.spellcastingAbility(spell))));stats.add(block("SPELL FLAGS",wrapped((d.concentration()?"Concentration":"No concentration")+(d.ritual()?" // Ritual":""))));row(codex,stats);
        row(codex,block("DAMAGE / EFFECT",wrapped(CastingPanel.damageSummary(spell,spell.level(),selection.level()))));
        row(codex,block("OPERATIONAL SPELL RULES",wrapped(d.description())));
        if(!d.higherLevels().isBlank())row(codex,block("HIGHER-LEVEL SCALING",wrapped(d.higherLevels())));
        JPanel compatible=new Wrap();for(var option:session.metamagic().stream().sorted(Comparator.comparing(Metamagic::toString)).toList())if(option.applies(spell)){JLabel chip=tag(option+" // "+option.cost()+" SP",false);chip.setToolTipText(option.eligibilityNote(spell));compatible.add(chip);}
        if(compatible.getComponentCount()==0)compatible.add(wrapped("No compatible chosen Metamagic."));row(codex,block("COMPATIBLE CHOSEN METAMAGIC",compatible));
        row(codex,AppStyle.compactButton("Full details",()->AppStyle.openWindow(this,spell.name(),new SpellDetailPanel(spell))));row(inspector,codex);glue(inspector);inspector.revalidate();
        SwingUtilities.invokeLater(()->{if(!disposed)inspectorScroll.getViewport().setViewPosition(position);});
    }
    @Override public void dispose(){disposed=true;session.removeListener(listener);selection.removeChangeListener(listener);}
    private static JLabel cardTag(String value,boolean yellow){JLabel label=tag(value,yellow);label.setFont(new Font("Consolas",Font.BOLD,11));label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(4,6,4,6)));return label;}
    private static final class SpellTitle extends JButton {
        SpellTitle(String name,Runnable action){super("<html><u>"+name.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</u></html>");setUI(new javax.swing.plaf.basic.BasicButtonUI());putClientProperty("tactileCustomPaint",true);setFont(new Font("Segoe UI",Font.BOLD,16));AppStyle.setColorRole(this,"ink");setBorder(new EmptyBorder(0,0,0,0));setBorderPainted(false);setContentAreaFilled(false);setFocusPainted(false);setHorizontalAlignment(SwingConstants.LEFT);setVerticalAlignment(SwingConstants.TOP);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setToolTipText(name);addActionListener(e->action.run());}
        @Override public Dimension getPreferredSize(){int width=getWidth()>0?getWidth():180;Object renderer=getClientProperty(javax.swing.plaf.basic.BasicHTML.propertyKey);if(renderer instanceof javax.swing.text.View view){view.setSize(width,Float.MAX_VALUE);return new Dimension(width,(int)Math.ceil(view.getPreferredSpan(javax.swing.text.View.Y_AXIS)));}return super.getPreferredSize();}
        @Override public Dimension getMinimumSize(){return new Dimension(0,getPreferredSize().height);}
    }
    private static JLabel coloredTag(String value,String surface){JLabel label=cardTag(value,false);label.putClientProperty("surfaceRole",surface);AppStyle.setColorRole(label,"badgeLightText");return label;}
    private static JLabel tag(String value,boolean yellow){JLabel label=AppStyle.mono(value.toUpperCase(Locale.ROOT),10);label.setOpaque(true);label.putClientProperty("surfaceRole",yellow?"slotAvailable":"high");AppStyle.setColorRole(label,yellow?"badgeText":"ink");label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(2,4,2,4)));return label;}
    private static JPanel block(String title,JComponent content){JPanel panel=AppStyle.surfacePanel(new BorderLayout(0,6),"high");panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(8,8,8,8)));panel.add(AppStyle.mono(title,10),BorderLayout.NORTH);panel.add(content);return panel;}
    private static JTextArea wrapped(String value){JTextArea area=new JTextArea(value){@Override public Dimension getPreferredSize(){int width=getWidth()>0?getWidth():300;getUI().getRootView(this).setSize(Math.max(1,width),Float.MAX_VALUE);return new Dimension(width,(int)Math.ceil(getUI().getRootView(this).getPreferredSpan(javax.swing.text.View.Y_AXIS)));}};area.setLineWrap(true);area.setWrapStyleWord(true);area.setEditable(false);area.setOpaque(false);area.setFont(new Font("Segoe UI",Font.PLAIN,13));return area;}
    private static JButton filterButton(String text,Runnable action){JButton button=AppStyle.compactButton(text,action);button.setFont(new Font("Segoe UI",Font.BOLD,14));return button;}
    private static JToggleButton toggle(String text,Runnable action){JToggleButton button=new JToggleButton(text);AppStyle.roundButton(button);button.setFont(new Font("Segoe UI",Font.BOLD,14));button.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(7,9,7,9)));button.addActionListener(e->action.run());return button;}
    private static void row(JPanel panel,JComponent child){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=panel.getComponentCount();c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.NORTH;c.insets=new Insets(0,0,5,0);panel.add(child,c);}
    private static void glue(JPanel panel){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=panel.getComponentCount();c.weighty=1;panel.add(Box.createVerticalGlue(),c);}
    private static JScrollPane scroll(JComponent view){JScrollPane scroll=new JScrollPane(view);AppStyle.scrollPane(scroll);scroll.getViewport().putClientProperty("surfaceRole","background");return scroll;}
    private static final class SpellGrid extends JPanel implements Scrollable {
        SpellGrid(){super(new CardRows());setName("spell-card-grid");putClientProperty("surfaceRole","background");}
        private int columns(){int width=getParent() instanceof JViewport viewport?viewport.getWidth():getWidth();return width>=580+Math.max(0,AppStyle.textSize()-14)*30?2:1;}
        @Override public void doLayout(){((GridLayout)getLayout()).setColumns(columns());super.doLayout();}
        @Override public Dimension getPreferredSize(){((GridLayout)getLayout()).setColumns(columns());return super.getPreferredSize();}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(650,600);}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return false;}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 48;}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(48,r.height-48);}
    }
    /** Equal widths with heights measured per pair, so one long spell does not stretch every card. */
    private static final class CardRows extends GridLayout {
        CardRows(){super(0,2,8,8);}
        private int[] heights(Container parent,int width){
            Component[] children=parent.getComponents();int columns=getColumns();int[] heights=new int[(children.length+columns-1)/columns];
            for(int index=0;index<children.length;index++){
                Component child=children[index];child.setSize(width,Math.max(1,child.getPreferredSize().height));layoutChildren(child);
                heights[index/columns]=Math.max(heights[index/columns],child.getPreferredSize().height);
            }
            return heights;
        }
        private static void layoutChildren(Component child){if(child instanceof Container container){container.doLayout();for(Component nested:container.getComponents())layoutChildren(nested);}}
        @Override public Dimension preferredLayoutSize(Container parent){int width=parent.getWidth()>0?parent.getWidth():650;int columnWidth=Math.max(1,(width-(getColumns()-1)*getHgap())/getColumns());int[] heights=heights(parent,columnWidth);return new Dimension(width,Arrays.stream(heights).sum()+Math.max(0,heights.length-1)*getVgap());}
        @Override public void layoutContainer(Container parent){int columns=getColumns();int width=Math.max(1,(parent.getWidth()-(columns-1)*getHgap())/columns);int[] heights=heights(parent,width);int y=0;Component[] children=parent.getComponents();for(int row=0;row<heights.length;row++){for(int column=0;column<columns;column++){int index=row*columns+column;if(index<children.length)children[index].setBounds(column*(width+getHgap()),y,width,heights[row]);}y+=heights[row]+getVgap();}}
    }
    private static final class Column extends JPanel implements Scrollable {Column(){super(new GridBagLayout());putClientProperty("surfaceRole","background");}public Dimension getPreferredScrollableViewportSize(){return new Dimension(450,600);}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getPreferredSize().height<getParent().getHeight();}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 24;}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(24,r.height-24);}}
    private static final class Wrap extends JPanel {Wrap(){super(new FlowLayout(FlowLayout.LEFT,5,3));setOpaque(false);putClientProperty("surfaceRole","background");}@Override public Dimension getPreferredSize(){int width=getWidth()>0?getWidth():600;Container library=SwingUtilities.getAncestorOfClass(SpellLibraryPanel.class,this);if(library!=null&&library.getWidth()>0)width=Math.max(100,Math.min(width,library.getWidth()-100));int x=0,height=0,line=0;for(Component child:getComponents()){Dimension size=child.getPreferredSize();if(x>0&&x+size.width>width-10){height+=line+6;x=0;line=0;}x+=size.width+5;line=Math.max(line,size.height);}return new Dimension(width,height+line+6);}@Override public Dimension getMinimumSize(){return new Dimension(0,getPreferredSize().height);}}
}
