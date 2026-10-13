package org.example.ui.spells;

import org.example.domain.character.*;
import org.example.domain.spells.*;
import org.example.ui.common.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.random.RandomGenerator;

/** Cast & Combat workstation. Every resource and selection belongs to the shared character. */
public final class CastingPanel extends JPanel implements LiveView {
    private final PlaySession session;
    private final IntSupplier charisma;
    private SpellCatalog.Spell selected;
    private int castingLevel;
    private final JPanel arsenal=new Arsenal();
    private final JPanel filters=AppStyle.section(new FlowLayout(FlowLayout.LEFT,6,0));
    private final JTextArea preview=AppStyle.text("");
    private final JTextArea log=AppStyle.text("");
    private final JLabel dossierTitle=AppStyle.heading("Spell dossier",25);
    private final JLabel dossierSubtitle=AppStyle.label("Choose a prepared spell",12,false);
    private final JLabel cost=AppStyle.label("No spell selected",13,false);
    private final JPanel diceOptions=AppStyle.section(new GridLayout(1,0,6,0));
    private final JPanel diceActions=AppStyle.section(new FlowLayout(FlowLayout.LEFT,8,0));
    private final JTextArea diceNote=AppStyle.text("");
    private final JPanel diceTray;
    private List<Integer> visibleDice=List.of();
    private final JTextArea result=AppStyle.text("");
    private final JButton castButton=AppStyle.primaryButton("Cast",this::cast);
    private final JButton damageButton=AppStyle.button("Quick roll damage",() -> rollDamage(false));
    private final JButton criticalButton=AppStyle.button("Crit ×2 dice",() -> rollDamage(true));
    private Runnable spellbookAction;
    private final RandomGenerator random=RandomGenerator.getDefault();
    private final Runnable listener=this::refresh;
    private boolean updating;
    private final Set<Integer> selectedLevels=new HashSet<>();
    private List<SpellCatalog.Spell> available=List.of(), visible=List.of();
    private List<Integer> filterLevels;
    private final Map<String,SpellButton> spellCards=new HashMap<>();
    private static final DateTimeFormatter TIME=DateTimeFormatter.ofPattern("HH:mm:ss");

    public CastingPanel(PlaySession session,IntSupplier charisma) {
        super(new BorderLayout(0,12)); this.session=session; this.charisma=charisma;
        spellbookAction=() -> AppStyle.openWindow(this,"Sorcerer spellbook",new SpellBookPanel(session.selection()));
        setOpaque(true); setBackground(AppStyle.BACKGROUND); setBorder(new EmptyBorder(14,14,14,14)); JPanel columns=AppStyle.section(new GridBagLayout());
        Column right=new Column();
        JPanel spellList=arsenalCard(); spellList.setMinimumSize(new Dimension(350,200));
        right.addCard(dossierCard()); diceTray=diceCard(); diceTray.setAlignmentX(LEFT_ALIGNMENT);
        right.addCard(diceTray);right.addCard(logCard());
        GridBagConstraints c=new GridBagConstraints(); c.gridy=0; c.fill=GridBagConstraints.BOTH; c.weighty=1;
        c.gridx=0; c.weightx=.55; c.insets=new Insets(0,0,0,6); columns.add(spellList,c);
        c.gridx=1; c.weightx=.45; c.insets=new Insets(0,6,0,0); columns.add(columnScroll(right),c); add(columns,BorderLayout.CENTER);
        add(castBar(),BorderLayout.SOUTH); castButton.putClientProperty("tactileRole","red");
        session.addListener(listener); session.selection().addChangeListener(listener); installShortcuts(); refresh();
    }
    private JPanel castBar() {
        JPanel card=AppStyle.card(new BorderLayout(12,4)); card.setBorder(new EmptyBorder(9,14,9,14));
        JPanel words=AppStyle.section(new BorderLayout(0,3)); words.add(cost,BorderLayout.NORTH); result.setRows(1); words.add(result,BorderLayout.CENTER);
        card.add(words,BorderLayout.CENTER); castButton.setPreferredSize(new Dimension(160,48)); card.add(castButton,BorderLayout.EAST); return card;
    }
    private JPanel arsenalCard() {
        JPanel card=AppStyle.card(new BorderLayout(0,12));
        card.add(filters,BorderLayout.NORTH);
        JScrollPane scroll=new JScrollPane(arsenal); AppStyle.scrollPane(scroll); scroll.setPreferredSize(new Dimension(400,235)); card.add(scroll,BorderLayout.CENTER); return card;
    }
    private JPanel dossierCard() {
        JPanel card=AppStyle.card(new BorderLayout(0,10)); JPanel heading=AppStyle.section(new GridLayout(0,1,0,4)); heading.add(dossierTitle); heading.add(dossierSubtitle); card.add(heading,BorderLayout.NORTH);
        card.setName("combat-spell-dossier");preview.setRows(11); JScrollPane scroll=new JScrollPane(preview); AppStyle.scrollPane(scroll); scroll.setPreferredSize(new Dimension(360,230)); card.add(scroll,BorderLayout.CENTER); return card;
    }
    private JPanel diceCard() {
        JPanel card=AppStyle.card(new BorderLayout(0,12)); card.add(TactileStyle.heading("Arcane Dice Tray",TactileStyle.YELLOW),BorderLayout.NORTH); JPanel body=AppStyle.section(new GridLayout(0,1,0,8));
        card.setName("spell-dice-tray");diceNote.setRows(3);
        body.add(diceOptions);body.add(diceNote);diceActions.add(damageButton);diceActions.add(criticalButton);body.add(diceActions);
        card.add(body,BorderLayout.CENTER); return card;
    }
    public boolean isDiceTrayVisible(){return diceTray.isVisible();}
    public List<Integer> visibleDiceSides(){return visibleDice;}
    private void updateDiceTray() {
        SpellCatalog.Spell spell=selectedSpell();
        List<Integer> next=SpellDice.forSpell(spell,effectiveLevel(),session.selection().level());
        diceTray.setVisible(!next.isEmpty());
        if(!visibleDice.equals(next)) {
            visibleDice=next;diceOptions.removeAll();
            for(int sides:next) {
                JButton die=AppStyle.button("d"+sides,()->{
                    SpellCatalog.Spell selected=selectedSpell();if(selected==null)return;
                    if(sides==20 && hasAttack(selected)){rollAttack();}
                    else recordEvent(selected.name()+" · d"+sides+": ["+random.nextInt(1,sides+1)+"]");
                });
                die.setToolTipText(sides==20 && spell!=null && hasAttack(spell)?"Roll this spell's attack with the spell attack modifier":"Roll one d"+sides+"; follow the spell text for quantity, modifiers, and timing");
                diceOptions.add(die);
            }
        }
        for(Component child:diceOptions.getComponents()) {
            if(child instanceof JButton die)die.setToolTipText("d20".equals(die.getText()) && spell!=null && hasAttack(spell)
                    ? "Roll this spell's attack with the spell attack modifier; Innate Sorcery grants Advantage"
                    : "Roll one "+die.getText()+"; follow the spell text for quantity, modifiers, and timing");
        }
        boolean automatic=spell!=null && damageProfile().isPresent();
        damageButton.setVisible(automatic);criticalButton.setVisible(automatic && hasAttack(spell));diceActions.setVisible(automatic);
        diceNote.setText(spell==null?"":automatic?"Damage: "+damageProfile().orElseThrow().notation():SpellDice.note(spell));
        AppStyle.applyAppearance(diceTray);diceTray.revalidate();diceTray.getParent().revalidate();
    }
    private JPanel logCard() {
        JPanel card=AppStyle.card(new BorderLayout(0,10)); JPanel title=AppStyle.section(new BorderLayout()); title.add(TactileStyle.heading("Combat Roll Log",TactileStyle.BLUE),BorderLayout.WEST);
        JPanel logActions=AppStyle.section(new FlowLayout(FlowLayout.RIGHT,8,0)); logActions.add(AppStyle.button("Clear",this::clearLog)); title.add(logActions,BorderLayout.EAST); card.add(title,BorderLayout.NORTH); log.setRows(9); log.setFont(new Font(Font.MONOSPACED,Font.PLAIN,12)); clearLog();
        JScrollPane scroll=new JScrollPane(log); AppStyle.scrollPane(scroll); scroll.setPreferredSize(new Dimension(360,190)); card.add(scroll,BorderLayout.CENTER); return card;
    }

    public SpellCatalog.Spell selectedSpell() { return selected; }
    public boolean selectSpell(String name) {
        for (SpellCatalog.Spell spell:available) {
            if (spell.name().equals(name)) { selected=spell; refreshSlots(); return true; }
        }
        return false;
    }
    public void setSpellbookAction(Runnable action) { spellbookAction=Objects.requireNonNull(action); }
    public int visibleSpellCount() { return visible.size(); }
    public String combatLogText() { return log.getText(); }
    public void refresh() {
        if (updating) { return; } updating=true; SpellCatalog.Spell previous=selectedSpell();
        List<SpellCatalog.Spell> next=session.selection().catalog().stream().filter(session.selection()::selected).filter(session.selection()::available).toList();
        if (!available.equals(next)) { available=next; selected=available.contains(previous)?previous:available.isEmpty()?null:available.getFirst(); }
        rebuildFilters(); updating=false; refreshSlots();
    }
    private void rebuildFilters() {
        selectedLevels.removeIf(level -> level>0 && available.stream().noneMatch(s -> s.level()==level));
        List<Integer> levels=available.stream().map(SpellCatalog.Spell::level).filter(l->l>0).distinct().toList();
        if(levels.equals(filterLevels)) {
            for(Component component:filters.getComponents())if(component instanceof JToggleButton button)
                button.setSelected(filterSelected((Integer)button.getClientProperty("spellLevel")));
            return;
        }
        filterLevels=levels;
        filters.removeAll(); addFilter("All",-1); addFilter("Cantrips",0);
        levels.forEach(l -> addFilter("Level "+l,l)); filters.revalidate();
    }
    private boolean filterSelected(int level) { return level<0 ? selectedLevels.isEmpty() : selectedLevels.contains(level); }
    private void addFilter(String title,int level) {
        JToggleButton filter=AppStyle.chip(title,() -> {
            if(level<0)selectedLevels.clear();
            else if(!selectedLevels.remove(level))selectedLevels.add(level);
            rebuildFilters();rebuildArsenal();
        });
        filter.putClientProperty("spellLevel",level);filter.setSelected(filterSelected(level));filters.add(filter);
    }
    private void refreshSlots() {
        if (updating) { return; }
        SpellCatalog.Spell spell=selectedSpell();
        if (spell==null || spell.level()==0) { castingLevel=0; }
        else if (castingLevel<spell.level() || castingLevel>9 || session.maxSlots(castingLevel)==0) { castingLevel=spell.level(); }
        rebuildArsenal(); preview(); AppStyle.applyAppearance(this);
    }
    private void rebuildArsenal() {
        List<SpellCatalog.Spell> next=available.stream().filter(s -> selectedLevels.isEmpty() || selectedLevels.contains(s.level())).toList();
        boolean changed=!visible.equals(next) || arsenal.getComponentCount()==0;
        visible=next;
        if(!changed && !visible.isEmpty()) {
            spellCards.values().forEach(SpellButton::refreshCard);arsenal.revalidate();arsenal.repaint();return;
        }
        arsenal.removeAll();
        for(SpellCatalog.Spell spell:visible) {
            SpellButton card=spellCards.computeIfAbsent(spell.name(),name->new SpellButton(spell));card.refreshCard();arsenal.add(card);
        }
        if (visible.isEmpty()) { JPanel empty=AppStyle.section(new BorderLayout(0,8)); JTextArea text=AppStyle.text(available.isEmpty()?"No available spells. Choose your prepared spells and cantrips to fill the arsenal.":"No spells match this filter."); text.setRows(2); empty.add(text,BorderLayout.CENTER);
            empty.add(AppStyle.button(available.isEmpty()?"Choose spells":"Show all spells",() -> { if (available.isEmpty()) { spellbookAction.run(); } else { selectedLevels.clear(); rebuildFilters(); rebuildArsenal(); } }),BorderLayout.SOUTH); arsenal.add(empty); }
        arsenal.revalidate(); arsenal.repaint();
    }
    private int slotLevel() { return castingLevel; }
    private int effectiveLevel() { return castingLevel; }
    private boolean sorcererSpell(SpellCatalog.Spell spell) { return SpellCatalog.alwaysPrepared(spell,session.selection().level(),session.selection().subclass()) || session.selection().chosenSpells().contains(spell.name()) && spell.sorcererSpell(); }
    public List<Metamagic> compatibleChosen(SpellCatalog.Spell spell) {
        if (session.selection().level()<2) { return List.of(); }
        return Arrays.stream(Metamagic.values()).filter(session.metamagic()::contains).filter(option -> option.applies(spell)).toList();
    }
    private String compatibleNames(SpellCatalog.Spell spell) {
        return String.join(", ",compatibleChosen(spell).stream().map(Metamagic::toString).toList());
    }
    private SorcererRules.Stats stats() { return SorcererRules.calculate(session.selection().level(),charisma.getAsInt(),session.selection().subclass()); }
    private int attackBonus(){return stats().proficiencyBonus()+(selectedSpell()==null?stats().charismaModifier():session.spellcastingModifier(selectedSpell(),charisma.getAsInt()));}
    private int saveDc() { return 8+attackBonus()+(selectedSpell()!=null && session.innateActive() && sorcererSpell(selectedSpell())?1:0); }
    private void preview() {
        updateDiceTray();
        SpellCatalog.Spell spell=selectedSpell();
        if (spell==null) {
            dossierTitle.setText("Spell dossier"); dossierSubtitle.setText("Your chosen spell appears here");
            preview.setText("Open the Spellbook and choose cantrips and prepared spells to get started.");
            cost.setText("No spell selected"); castButton.setEnabled(false);damageButton.setEnabled(false);criticalButton.setEnabled(false);return;
        }
        SpellDetails d=SpellDetails.forSpell(spell);boolean innate=session.innateActive() && sorcererSpell(spell);
        dossierTitle.setText(spell.name());dossierSubtitle.setText(spell.levelName()+" · "+d.school()+" · "+(session.selection().speciesSpell(spell)?"Species magic":"Sorcerer magic"));
        dossierSubtitle.setToolTipText(dossierSubtitle.getText());
        String slotCost=spell.level()==0?"No slot":"Level "+slotLevel()+" slot ("+session.remainingSlots(slotLevel())+" left)";
        cost.setText(slotCost+" + 0 SP · "+session.remainingPoints()+" SP available");
        String enhancements=compatibleChosen(spell).isEmpty()?"No chosen Metamagic matches.":"Compatible chosen Metamagic: "+compatibleNames(spell)+". View these options in the Metamagic.";
        String checks=(hasSave(spell)?"Save DC "+saveDc():"")+(hasSave(spell)&&hasAttack(spell)?" · ":"")+
                (hasAttack(spell)?"Attack "+String.format("%+d",attackBonus())+(innate?" with Advantage":""):"");
        if (checks.isBlank())checks="No target saving throw or caster attack roll.";
        String mechanics="Casting: "+d.castingTime()+"\nRange: "+d.range()+"\nDuration: "+d.duration()+"\nComponents: "+d.components()+"\n"+checks+
                "\nConcentration: "+(d.concentration()?"required":"not required")+(d.ritual()?" · Ritual":"")+"\n\n"+
                damageSummary(spell,effectiveLevel(),session.selection().level())+"\n\n"+enhancements+"\n\nSPELL MECHANICS\n"+
                d.description().replaceAll("<[^>]*>"," ")+"\n\nHIGHER LEVELS\n"+(d.higherLevels().isBlank()?"No additional scaling listed.":d.higherLevels());
        int caret=preview.getCaretPosition();boolean same=Objects.equals(preview.getClientProperty("spellName"),spell.name());
        preview.setText(mechanics);preview.putClientProperty("spellName",spell.name());preview.setCaretPosition(same?Math.min(caret,preview.getDocument().getLength()):0);
        var formula=damageProfile();
        damageButton.setEnabled(formula.isPresent());criticalButton.setEnabled(formula.isPresent() && hasAttack(spell));
        criticalButton.setToolTipText("Double the dice for an attack that scored a critical hit; modifiers are added once.");
        castButton.setEnabled(true);castButton.setToolTipText("Cast "+spell.name()+" · "+slotCost+". Enter when the workspace has focus.");
    }
    private Optional<DamageDice.Formula> damageProfile() {
        SpellCatalog.Spell spell=selectedSpell();return spell==null?Optional.empty():DamageDice.profile(spell,effectiveLevel(),session.selection().level());
    }
    public static String damageSummary(SpellCatalog.Spell spell,int effective,int characterLevel) {
        if (spell.name().equals("True Strike")) {
            int extra=(characterLevel>=5?1:0)+(characterLevel>=11?1:0)+(characterLevel>=17?1:0);
            return "Damage: weapon damage + spellcasting ability modifier"+(extra>0?" + "+extra+"d6 Radiant":"")+
                    ". The spell replaces the weapon's normal ability modifier; choose Radiant or the weapon's normal type for its base damage.";
        }
        return DamageDice.profile(spell,effective,characterLevel).map(f -> "Damage roll: "+f.notation()+". Follow the spell text for additional effects.")
                .orElseGet(() -> SpellMechanics.damageNote(spell));
    }
    private void cast() {
        SpellCatalog.Spell spell=selectedSpell();if (spell==null)return;
        if (SpellDetails.forSpell(spell).concentration() && !session.concentration().isBlank()
                && JOptionPane.showConfirmDialog(this,"Replace concentration on "+session.concentration()+"?","Concentration",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        int selectedLevel=slotLevel();String error=session.cast(spell,selectedLevel,false,Set.of());
        result.setText(error.isEmpty()?"Cast "+spell.name()+". Resources updated.":error);
        if(error.isEmpty())recordEvent("Cast "+spell.name()+" · "+(spell.level()==0?"cantrip":"level "+selectedLevel+" slot"));
        else recordEvent("Cast blocked: "+error);
    }
    private static boolean hasAttack(SpellCatalog.Spell spell) { return SpellMechanics.hasAttack(spell); }
    private static boolean hasSave(SpellCatalog.Spell spell) { return SpellMechanics.forcesSave(spell); }
    private void rollAttack() {
        SpellCatalog.Spell spell=selectedSpell();if (spell==null || !hasAttack(spell))return;
        boolean advantage=session.innateActive() && sorcererSpell(spell);
        int first=random.nextInt(1,21),second=advantage?random.nextInt(1,21):first,die=advantage?Math.max(first,second):first;
        int total=die+attackBonus();String outcome=die==20?" · Critical hit":die==1?" · Automatic miss":"";
        recordEvent(spell.name()+" attack · "+(advantage?"Advantage":"Normal")+" ["+first+(advantage?", "+second:"")+"] "+String.format("%+d",attackBonus())+" = "+total+outcome);
    }
    private void rollDamage(boolean critical) {
        var profile=damageProfile(); if (profile.isEmpty()) { result.setText("Resolve this spell's damage using its full text."); return; } DamageDice.Formula f=profile.get(); DamageDice.Roll rolled=f.roll(random,critical,Math.max(0,session.spellcastingModifier(selectedSpell(),charisma.getAsInt())));
        String formula=critical?f.count()*2+"d"+f.sides()+(f.modifier()==0?"":" + "+f.modifier())+(f.type().isBlank()?"":" "+f.type())+(f.scope().isBlank()?"":" · "+f.scope()):f.notation(); recordEvent(selectedSpell().name()+(critical?" critical damage":" damage")+" · "+formula+" "+rolled.dice()+" = "+rolled.total());
    }
    public static boolean saveSucceeds(int roll,int modifier,int dc) { return roll+modifier>=dc; }
    public void recordEvent(String event) { String entry="["+TIME.format(LocalTime.now())+"] "+event; if (log.getText().startsWith("No actions recorded")) { log.setText(""); } log.append((log.getText().isBlank()?"":"\n\n")+entry); if (log.getDocument().getLength()>30000) { log.setText(log.getText().substring(log.getDocument().getLength()-24000)); } log.setCaretPosition(log.getDocument().getLength()); }
    public void clearLog() { log.setText(""); }
    private void installShortcuts() {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,0),"castSpell"); getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE,0),"castSpell");
        getActionMap().put("castSpell",new AbstractAction() { @Override public void actionPerformed(ActionEvent e) { if (shortcutAllowed()) { cast(); } } });
    }
    private boolean shortcutAllowed() { Component focus=KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner(); return isShowing() && !(focus instanceof JTextComponent || focus instanceof AbstractButton || focus instanceof JComboBox<?> || focus instanceof JSpinner || focus instanceof JSlider); }
    @Override public void dispose() { session.removeListener(listener); session.selection().removeChangeListener(listener); }
    private static JScrollPane columnScroll(Column column) { JScrollPane scroll=new JScrollPane(column); AppStyle.scrollPane(scroll); scroll.setOpaque(false); scroll.getViewport().setOpaque(false); scroll.setMinimumSize(new Dimension(350,200)); return scroll; }
    private static final class Column extends JPanel implements Scrollable {
        Column() { super(new GridBagLayout()); setOpaque(false); }
        void addCard(JPanel panel) { GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=getComponentCount();c.weightx=1;c.weighty=c.gridy==0?1:0;c.fill=GridBagConstraints.BOTH;c.insets=new Insets(c.gridy==0?0:12,0,0,0);add(panel,c); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction) { return 28; }
        @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction) { return visible.height-28; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return getParent()!=null && getParent().getHeight()>=getPreferredSize().height; }
    }
    private static final class Arsenal extends JPanel implements Scrollable {
        Arsenal() { super(new GridLayout(0,1,8,8)); setOpaque(false); }
        private void columns(int width) { ((GridLayout)getLayout()).setColumns(width>=490?2:1); }
        @Override public Dimension getPreferredSize() { columns(getParent() instanceof JViewport viewport?viewport.getWidth():getWidth()); return super.getPreferredSize(); }
        @Override public void doLayout() { columns(getWidth()); super.doLayout(); }
        @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(400,235); }
        @Override public int getScrollableUnitIncrement(Rectangle visible,int orientation,int direction) { return 64; }
        @Override public int getScrollableBlockIncrement(Rectangle visible,int orientation,int direction) { return Math.max(64,visible.height-64); }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
    private final class SpellButton extends JButton {
        private final SpellCatalog.Spell spell;
        SpellButton(SpellCatalog.Spell spell) { super(spell.name()); this.spell=spell; putClientProperty("tactileCustomPaint",true); setPreferredSize(new Dimension(350,compatibleChosen(spell).isEmpty()?58:78)); setFocusPainted(false); setContentAreaFilled(false); setBorder(new EmptyBorder(8,12,8,12)); setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(SpellDetails.forSpell(spell).shortDescription()+(compatibleChosen(spell).isEmpty()?"":" Enhance with: "+compatibleNames(spell)));
            getAccessibleContext().setAccessibleDescription(getToolTipText()); addActionListener(e -> selectSpell(spell.name())); }
        private void refreshCard() {
            setPreferredSize(new Dimension(350,compatibleChosen(spell).isEmpty()?58:78));
            setToolTipText(SpellDetails.forSpell(spell).shortDescription()+(compatibleChosen(spell).isEmpty()?"":" Enhance with: "+compatibleNames(spell)));
            getAccessibleContext().setAccessibleDescription(getToolTipText());
        }
        @Override protected void paintComponent(Graphics graphics) { Graphics2D g=AppStyle.graphics(graphics); boolean selected=spell.equals(selectedSpell()); g.setColor(selected?TactileStyle.YELLOW:AppStyle.SOFT); g.fillRect(0,0,getWidth()-3,getHeight()-3); g.setColor(AppStyle.INK); g.drawRect(0,0,getWidth()-3,getHeight()-3); if (selected) { g.setColor(TactileStyle.BLUE); g.setStroke(new BasicStroke(2)); g.drawRect(2,2,getWidth()-7,getHeight()-7); }
            g.setFont(getFont().deriveFont(Font.BOLD,AppStyle.textSize())); g.setColor(AppStyle.INK); g.drawString(spell.name(),12,22); g.setFont(getFont().deriveFont(Font.PLAIN,AppStyle.textSize()-3f)); g.setColor(selected?TactileStyle.BLUE:AppStyle.MUTED); g.drawString(spell.levelName()+" · "+SpellDetails.forSpell(spell).school()+(SpellDetails.forSpell(spell).concentration()?" · Concentration":""),12,43);
            if (!compatibleChosen(spell).isEmpty()) {
                String enhancement="Enhance: "+String.join(" · ",compatibleChosen(spell).stream().map(m -> m.toString().replace(" Spell","")).toList());
                while (g.getFontMetrics().stringWidth(enhancement)>getWidth()-24 && enhancement.length()>10) { enhancement=enhancement.substring(0,enhancement.length()-2)+"…"; }
                g.setColor(AppStyle.INK); g.drawString(enhancement,12,64);
            }
            if (getWidth()>390) { String range=SpellDetails.forSpell(spell).range(); g.setColor(AppStyle.INK); g.drawString(range,getWidth()-g.getFontMetrics().stringWidth(range)-12,22); } g.dispose(); }
    }
}
