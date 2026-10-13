package org.example.ui.character;

import org.example.domain.character.*;
import org.example.domain.character.CharacterSheet.*;
import org.example.domain.spells.*;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.*;
import org.example.ui.spells.SpellDetailPanel;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.text.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.util.*;
import java.util.List;
import java.util.function.*;

/** Live single-character sheet based on the supplied three-column Bauhaus editor. */
public final class CharacterSheetPanel extends JPanel implements LiveView {
    private final GrimoirePanel workspace;
    private final SorcererPanel editor;
    private final PlaySession session;
    private final CharacterChoicesPanel choices;
    private final JTextField name=new JTextField(20),background=new JTextField(15),alignment=new JTextField(12);
    private final SubclassSelector subclass;
    private final PillSelector<Species> species;
    private final JSpinner level,hitPoints,maxHitPoints;
    private final EnumMap<Ability,JSpinner> scores=new EnumMap<>(Ability.class);
    private final EnumMap<Ability,JLabel> modifiers=new EnumMap<>(Ability.class),saves=new EnumMap<>(Ability.class);
    private final EnumMap<Field,JSpinner> fields=new EnumMap<>(Field.class);
    private final EnumMap<Field,List<JCheckBox>> deathMarkers=new EnumMap<>(Field.class);
    private final EnumMap<Skill,SkillProficiencyRow> skillRows=new EnumMap<>(Skill.class);
    private final JLabel skillLegend=AppStyle.mono("",11);
    private final EnumMap<Skill,JLabel> passives=new EnumMap<>(Skill.class);
    private final JSpinner sorceryPoints;
    private final Map<Integer,JSpinner> spellSlots=new LinkedHashMap<>();
    private final JLabel armor=AppStyle.label("",22,true),initiative=AppStyle.label("",22,true),proficiency=AppStyle.label("",22,true),dc=AppStyle.label("",15,true);
    private final JLabel hpLabel=AppStyle.label("",14,true),hitDice=AppStyle.label("",12,true),status=AppStyle.label("",11,false);
    private final JProgressBar hpBar=new JProgressBar();
    private final JCheckBox inspiration=new JCheckBox("Heroic Inspiration");
    private final JCheckBox automaticHp=new JCheckBox("Calculate maximum HP from class");
    private final JTextArea hpCalculation=FeatComponents.text("",11);
    private final JTextArea languages=new JTextArea(2,16),training=new JTextArea(3,16);
    private final JTextArea grantedTools=FeatComponents.text("",12);
    private final JPanel features=AppStyle.section(new GridBagLayout()),featList=AppStyle.section(new GridBagLayout());
    private final JButton innate,save,spendHitDie,discard,shortRest,longRest;
    private final Runnable listener=this::refresh;
    private final PropertyChangeListener stateListener=event->refresh();
    private boolean updating=true,disposed;
    private String featureSignature="",featSignature="";
    private long restoreRevision;

    public CharacterSheetPanel(GrimoirePanel workspace) {
        super(new BorderLayout());this.workspace=workspace;editor=workspace.characterPanel();session=workspace.playSession();
        setBorder(new EmptyBorder(12,12,12,12));
        JPanel content=new FeatComponents.WidthPanel(new GridBagLayout());
        JPanel heading=FeatComponents.card(new BorderLayout(10,6));
        JPanel title=AppStyle.section(new BorderLayout(0,5));title.add(AppStyle.label("CHARACTER & SHEET",22,true),BorderLayout.NORTH);
        title.add(FeatComponents.tag("SORCERER · PHB 2024",true),BorderLayout.WEST);heading.add(title,BorderLayout.CENTER);
        JPanel restActions=new FeatComponents.WrapPanel(5);
        shortRest=AppStyle.compactButton("Short Rest",()->status.setText(session.shortRest()));restActions.add(shortRest);
        longRest=AppStyle.compactButton("Long Rest",session::longRest);restActions.add(longRest);
        innate=AppStyle.compactButton("Innate Sorcery",()->status.setText(session.toggleInnate()));innate.getAccessibleContext().setAccessibleName("Toggle Innate Sorcery");innate.putClientProperty("tactileRole","blue");restActions.add(innate);
        restActions.add(AppStyle.compactButton("Export sheet",this::printSheet));heading.add(restActions,BorderLayout.SOUTH);FeatComponents.row(content,heading);

        JPanel identity=FeatComponents.card(new GridBagLayout());
        name.setName("character-name");name.getAccessibleContext().setAccessibleName("Character name");name.setText(editor.characterName());
        bindText(name,value->{String clean=value.strip();if(!clean.isEmpty() && clean.length()<=60)editor.setCharacterName(clean);});
        FeatComponents.row(identity,labelled("CHARACTER NAME & EPITHET",name));
        JPanel origin=new FeatComponents.WrapPanel(8);
        subclass=new SubclassSelector(editor::selectSubclass);
        species=new PillSelector<>(List.of(Species.values()),Species.HUMAN,editor::selectSpecies);
        species.getAccessibleContext().setAccessibleName("Select PHB 2024 species and lineage");
        origin.add(labelled("SUBCLASS",subclass));origin.add(labelled("SPECIES & LINEAGE",species));
        level=spinner("Character level",1,1,20,value->editor.setLevel(value));origin.add(labelled("CLASS LEVEL",level));
        origin.add(labelled("BACKGROUND",background));origin.add(labelled("ALIGNMENT",alignment));FeatComponents.row(identity,origin);
        origin.add(AppStyle.compactButton("Choose background",workspace::showBackgrounds));
        bindText(background,value->session.setSheet(session.sheet().withNote(Note.BACKGROUND,value)));
        bindText(alignment,value->session.setSheet(session.sheet().withNote(Note.ALIGNMENT,value)));
        FeatComponents.row(content,identity);
        choices=new CharacterChoicesPanel(session);FeatComponents.row(content,choices);

        JPanel left=column(),middle=column(),right=column();
        JPanel vitals=card("COMBAT VITALS");
        JPanel metrics=new JPanel(new GridLayout(1,3,6,0));metrics.setOpaque(false);
        metrics.add(metric("ARMOR CLASS",armor));metrics.add(metric("INITIATIVE",initiative));metrics.add(metric("PROF BONUS",proficiency));FeatComponents.row(vitals,metrics);
        JPanel basics=new FeatComponents.WrapPanel(7);
        basics.add(labelled("AC OVERRIDE (0 = UNARMORED)",field(Field.AC_OVERRIDE,"Armor class override")));
        basics.add(labelled("SPEED (FEET)",field(Field.SPEED,"Speed")));FeatComponents.row(vitals,basics);
        JPanel hp=AppStyle.section(new BorderLayout(0,5));hp.add(hpLabel,BorderLayout.NORTH);
        hpBar.setStringPainted(true);hpBar.setForeground(AppStyle.ERROR);hpBar.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));hp.add(hpBar,BorderLayout.CENTER);
        JPanel hpInputs=new FeatComponents.WrapPanel(6);
        hitPoints=spinner("Current hit points",0,0,9999,session::setCurrentHitPoints);
        maxHitPoints=spinner("Maximum hit points",0,0,9999,value->session.setHitPoints(Math.min(value,session.hitPoints()),value));
        hpInputs.add(labelled("CURRENT HP",hitPoints));hpInputs.add(labelled("MAX HP",maxHitPoints));hpInputs.add(labelled("TEMP HP",field(Field.TEMP_HP,"Temporary hit points")));
        hp.add(hpInputs,BorderLayout.SOUTH);FeatComponents.row(vitals,FeatComponents.block("HIT POINT POOL",hp));
        automaticHp.getAccessibleContext().setAccessibleName("Automatic maximum HP");AppStyle.roundButton(automaticHp);automaticHp.setFont(new Font("Segoe UI",Font.PLAIN,12));
        automaticHp.addActionListener(event->{if(!updating)session.setAutomaticHp(automaticHp.isSelected());});FeatComponents.row(vitals,automaticHp);FeatComponents.row(vitals,hpCalculation);
        JPanel dice=new FeatComponents.WrapPanel(6);dice.add(hitDice);dice.add(labelled("SPENT",field(Field.SPENT_HIT_DICE,"Spent hit dice")));
        spendHitDie=AppStyle.compactButton("Spend d6 Hit Die",this::spendHitDie);dice.add(spendHitDie);FeatComponents.row(vitals,dice);
        spendHitDie.setToolTipText("During a Short Rest, spend one available Sorcerer Hit Die and heal by d6 + CON modifier");
        JPanel death=new FeatComponents.WrapPanel(6);
        death.add(labelled("DEATH SAVE SUCCESSES",deathMarks(Field.DEATH_SUCCESSES,"Success")));
        death.add(labelled("DEATH SAVE FAILURES",deathMarks(Field.DEATH_FAILURES,"Failure")));FeatComponents.row(vitals,death);
        AppStyle.roundButton(inspiration);inspiration.addActionListener(event->{if(!updating)session.setSheet(session.sheet().withField(Field.INSPIRATION,inspiration.isSelected()?1:0));});FeatComponents.row(vitals,inspiration);
        FeatComponents.row(left,vitals);

        JPanel attributes=card("ABILITY ATTRIBUTES");JPanel matrix=new JPanel(new GridLayout(0,2,7,7));matrix.setOpaque(false);
        for(Ability ability:Ability.values()) {
            JPanel tile=AppStyle.surfacePanel(new BorderLayout(0,5),ability==Ability.CHA?"slotAvailable":"low");
            tile.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(8,8,8,8)));
            JPanel label=AppStyle.section(new BorderLayout());label.add(AppStyle.mono(ability.name(),11),BorderLayout.WEST);
            JLabel saving=AppStyle.mono("",10);saves.put(ability,saving);label.add(saving,BorderLayout.EAST);tile.add(label,BorderLayout.NORTH);
            JPanel value=AppStyle.section(new BorderLayout(8,0));JLabel modifier=AppStyle.label("",23,true);modifiers.put(ability,modifier);value.add(modifier,BorderLayout.WEST);
            JSpinner score=spinner(ability.name()+" score",ability==Ability.CHA?editor.snapshot().charisma():10,1,30,number->{
                if(ability==Ability.CHA)editor.setCharismaScore(number);else session.setSheet(session.sheet().withAbility(ability,number));
            });scores.put(ability,score);value.add(score,BorderLayout.EAST);tile.add(value,BorderLayout.CENTER);matrix.add(tile);
        }
        FeatComponents.row(attributes,matrix);FeatComponents.row(attributes,metric("SORCERER SPELL SAVE DC",dc));FeatComponents.row(left,attributes);

        JPanel senses=card("PASSIVE SENSES");JPanel passiveTiles=new JPanel(new GridLayout(1,3,6,0));passiveTiles.setOpaque(false);
        for(Skill skill:List.of(Skill.PERCEPTION,Skill.INSIGHT,Skill.INVESTIGATION)){JLabel value=AppStyle.label("",20,true);value.setName("passive-"+skill);passives.put(skill,value);passiveTiles.add(metric(skill.label.toUpperCase(Locale.ROOT),value));}
        FeatComponents.row(senses,passiveTiles);FeatComponents.row(middle,senses);
        JPanel skills=card("SKILL PROFICIENCIES");
        skillLegend.setFont(new Font("Segoe UI Symbol",Font.PLAIN,11));
        FeatComponents.row(skills,skillLegend);
        for(Skill skill:Skill.values()) {
            SkillProficiencyRow row=new SkillProficiencyRow(skill,tier->{if(!updating)session.setSheet(session.sheet().withSkill(skill,tier));},()->rollSkill(skill));
            skillRows.put(skill,row);FeatComponents.row(skills,row);
        }
        FeatComponents.row(middle,skills);

        JPanel gear=card("TRAINING & GEAR");
        training.setLineWrap(true);training.setWrapStyleWord(true);languages.setLineWrap(true);languages.setWrapStyleWord(true);
        training.getAccessibleContext().setAccessibleName("Training and equipment notes");languages.getAccessibleContext().setAccessibleName("Languages and tools");
        bindText(training,value->session.setSheet(session.sheet().withNote(Note.TRAINING,value)));
        bindText(languages,value->session.setSheet(session.sheet().withNote(Note.LANGUAGES,value)));
        JScrollPane trainingInput=FeatComponents.scroll(training),languageInput=FeatComponents.scroll(languages);
        trainingInput.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));languageInput.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));
        FeatComponents.row(gear,labelled("WEAPON / ARMOR TRAINING & EQUIPMENT",trainingInput));
        grantedTools.getAccessibleContext().setAccessibleName("Granted tool proficiencies");FeatComponents.row(gear,grantedTools);
        FeatComponents.row(gear,labelled("LANGUAGES & TOOLS",languageInput));FeatComponents.row(right,gear);
        JPanel resources=card("SPELLCASTING RESOURCES");
        sorceryPoints=spinner("Remaining Sorcery Points",session.remainingPoints(),0,20,session::setRemainingPoints);
        FeatComponents.row(resources,labelled("SORCERY POINTS REMAINING",sorceryPoints));
        JPanel slots=new JPanel(new GridLayout(0,3,6,6));slots.setOpaque(false);
        for(int slot=1;slot<=9;slot++){
            int tier=slot;JSpinner remaining=spinner("Remaining level "+tier+" slots",session.remainingSlots(tier),0,4,value->session.setRemainingSlots(tier,value));
            spellSlots.put(tier,remaining);slots.add(labelled("LEVEL "+tier,remaining));
        }
        FeatComponents.row(resources,slots);FeatComponents.row(right,resources);
        JPanel traits=card("FEATURES & TRAITS");FeatComponents.row(traits,features);
        JPanel links=new FeatComponents.WrapPanel(5);
        links.add(AppStyle.compactButton("Metamagic",()->workspace.showPage(GrimoirePanel.METAMAGIC)));
        links.add(AppStyle.compactButton("Prepared spells",workspace::showPreparedSpells));
        links.add(AppStyle.compactButton("Choose feats",()->workspace.showPage(GrimoirePanel.FEATS)));FeatComponents.row(traits,links);FeatComponents.row(right,traits);
        for(JPanel column:List.of(left,middle,right)){
            GridBagConstraints glue=new GridBagConstraints();glue.gridx=0;glue.gridy=column.getComponentCount();glue.weighty=1;glue.fill=GridBagConstraints.VERTICAL;
            column.add(Box.createVerticalGlue(),glue);
        }
        JPanel columns=new JPanel(null) {
            private boolean stacked(){return getWidth()>0 && getWidth()<1050+Math.max(0,AppStyle.textSize()-14)*50;}
            @Override public Dimension getPreferredSize(){
                int height=0;for(Component child:getComponents())height=stacked()?height+child.getPreferredSize().height:Math.max(height,child.getPreferredSize().height);
                return new Dimension(getWidth()>0?getWidth():1100,height+(stacked()?24:0));
            }
            @Override public void doLayout(){
                int width=stacked()?getWidth():(getWidth()-24)/3,offset=0;
                for(Component child:getComponents()){
                    int height=stacked()?child.getPreferredSize().height:getHeight();
                    child.setBounds(stacked()?0:offset,stacked()?offset:0,width,height);offset+=(stacked()?height:width)+12;
                }
            }
        };columns.setOpaque(false);columns.add(left);columns.add(middle);columns.add(right);FeatComponents.row(content,columns);
        JPanel selectedFeats=card("SELECTED FEATS");FeatComponents.row(selectedFeats,featList);FeatComponents.row(content,selectedFeats);
        add(FeatComponents.scroll(content),BorderLayout.CENTER);
        JPanel footer=FeatComponents.card(new BorderLayout(8,0));footer.add(status,BorderLayout.CENTER);
        JPanel actions=AppStyle.section(new FlowLayout(FlowLayout.RIGHT,6,0));
        discard=AppStyle.compactButton("Discard edits",editor::restoreSaved);actions.add(discard);
        save=AppStyle.primaryButton("Commit to grimoire",this::commit);save.putClientProperty("tactileRole","red");actions.add(save);footer.add(actions,BorderLayout.EAST);add(footer,BorderLayout.SOUTH);
        session.addListener(listener);session.selection().addChangeListener(listener);editor.addPropertyChangeListener("characterState",stateListener);
        updating=false;refresh();
    }

    private static JPanel column(){return AppStyle.section(new GridBagLayout());}
    private static JPanel card(String title){JPanel card=FeatComponents.card(new GridBagLayout());FeatComponents.row(card,AppStyle.label(title,15,true));return card;}
    private static JPanel labelled(String title,JComponent component){JPanel panel=AppStyle.section(new BorderLayout(0,5));panel.add(AppStyle.mono(title,10),BorderLayout.NORTH);panel.add(component,BorderLayout.CENTER);return panel;}
    private static JPanel metric(String title,JLabel value){JPanel panel=AppStyle.surfacePanel(new BorderLayout(0,5),"low");panel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE),new EmptyBorder(7,7,7,7)));panel.add(AppStyle.mono(title,9),BorderLayout.NORTH);panel.add(value,BorderLayout.CENTER);return panel;}
    private JSpinner spinner(String accessible,int initial,int min,int max,IntConsumer setter){JSpinner spinner=new JSpinner(new SpinnerNumberModel(initial,min,max,1));spinner.setBorder(BorderFactory.createLineBorder(AppStyle.LINE));spinner.setPreferredSize(new Dimension(76,32));spinner.getAccessibleContext().setAccessibleName(accessible);spinner.addChangeListener(e->{if(!updating)setter.accept((Integer)spinner.getValue());});return spinner;}
    private JSpinner field(Field field,String title){JSpinner spinner=spinner(title,field.initial,field.minimum,field.maximum,value->session.setSheet(session.sheet().withField(field,value)));fields.put(field,spinner);return spinner;}
    private void bindText(JTextComponent component,Consumer<String> setter){
        int limit=component==name?60:2000;
        ((AbstractDocument)component.getDocument()).setDocumentFilter(new DocumentFilter(){
            @Override public void insertString(FilterBypass b,int offset,String text,AttributeSet attrs)throws BadLocationException{if(b.getDocument().getLength()+text.length()<=limit)super.insertString(b,offset,text,attrs);}
            @Override public void replace(FilterBypass b,int offset,int length,String text,AttributeSet attrs)throws BadLocationException{if(b.getDocument().getLength()-length+(text==null?0:text.length())<=limit)super.replace(b,offset,length,text,attrs);}
        });
        component.getDocument().addDocumentListener(new DocumentListener(){public void insertUpdate(DocumentEvent e){update();}public void removeUpdate(DocumentEvent e){update();}public void changedUpdate(DocumentEvent e){update();}private void update(){if(!updating)setter.accept(component.getText());}});
    }
    private static String signed(int number){return String.format("%+d",number);}
    private JPanel deathMarks(Field field,String title){
        JPanel panel=AppStyle.section(new FlowLayout(FlowLayout.LEFT,3,0));List<JCheckBox> markers=new ArrayList<>();
        for(int index=0;index<3;index++){
            int mark=index;JCheckBox box=new JCheckBox();AppStyle.roundButton(box);
            box.getAccessibleContext().setAccessibleName("Death save "+title.toLowerCase(Locale.ROOT)+" "+(index+1));
            box.addActionListener(event->{if(!updating)session.setSheet(session.sheet().withField(field,box.isSelected()?mark+1:mark));});
            markers.add(box);panel.add(box);
        }
        deathMarkers.put(field,markers);return panel;
    }
    private static void text(JTextComponent component,String value){if(!component.getText().equals(value))component.setText(value);}

    public void refresh(){
        if(updating || disposed)return;updating=true;
        try {
            CharacterState state=editor.snapshot();CharacterSheet sheet=session.sheet();var stats=SorcererRules.calculate(state.level(),state.charisma(),state.subclass());
            boolean restored=restoreRevision!=session.restoreRevision();restoreRevision=session.restoreRevision();
            if(restored || !name.hasFocus())text(name,state.name());text(background,sheet.note(Note.BACKGROUND));text(alignment,sheet.note(Note.ALIGNMENT));text(training,sheet.note(Note.TRAINING));text(languages,sheet.note(Note.LANGUAGES));
            background.setEditable(BackgroundCatalog.selected(sheet).isEmpty());background.setToolTipText(BackgroundCatalog.selected(sheet).isPresent()?"Selected PHB background; change it in Feats → Backgrounds":"Free-form background note; use Choose background to select a PHB background");
            subclass.setSubclass(state.subclass());species.setSelectedOption(state.species());level.setValue(state.level());
            ((SpinnerNumberModel)hitPoints.getModel()).setMaximum(session.maxHitPoints());hitPoints.setValue(session.hitPoints());maxHitPoints.setValue(session.maxHitPoints());fields.forEach((field,spinner)->spinner.setValue(sheet.field(field)));
            automaticHp.setSelected(session.automaticHp());maxHitPoints.setEnabled(!session.automaticHp());maxHitPoints.setToolTipText(session.automaticHp()?"Calculated from level, Constitution and HP bonuses":"Manual maximum for rolled HP or a table override");
            hpCalculation.setText((session.automaticHp()?"Calculated: ":"Fixed-value reference: ")+session.hpCalculation().explanation()+" = "+session.hpCalculation().maximum()+" HP");
            deathMarkers.forEach((field,markers)->{for(int index=0;index<markers.size();index++)markers.get(index).setSelected(index<sheet.field(field));});
            inspiration.setSelected(sheet.field(Field.INSPIRATION)==1);
            hpLabel.setText("HIT POINTS "+session.hitPoints()+" / "+session.maxHitPoints());hpBar.setMaximum(Math.max(1,session.maxHitPoints()));hpBar.setValue(session.hitPoints());hpBar.setString(session.maxHitPoints()==0?"Set maximum HP":session.hitPoints()+" / "+session.maxHitPoints());
            armor.setText(Integer.toString(sheet.armorClass(state.charisma())));armor.setToolTipText("Unarmored: 10 + DEX modifier unless AC override is set. Mage Armor: "+(13+CharacterSheet.modifier(sheet.score(Ability.DEX,state.charisma()))));
            initiative.setText(signed(CharacterSheet.modifier(sheet.score(Ability.DEX,state.charisma()))));proficiency.setText(signed(stats.proficiencyBonus()));dc.setText(Integer.toString(stats.spellSaveDc()));
            for(Ability ability:Ability.values()){
                int score=sheet.score(ability,state.charisma()),modifier=CharacterSheet.modifier(score);scores.get(ability).setValue(score);modifiers.get(ability).setText(signed(modifier));
                boolean trained=ability==Ability.CON || ability==Ability.CHA;saves.get(ability).setText("SAVE "+signed(modifier+(trained?stats.proficiencyBonus():0))+(trained?" P":""));
                saves.get(ability).setToolTipText(trained?"Sorcerer saving throw proficiency":"Ability modifier; apply extra feat proficiency on your sheet");
            }
            skillLegend.setText("■ PROF ("+signed(stats.proficiencyBonus())+")   ★ EXP ("+signed(stats.proficiencyBonus()*2)+")");
            skillLegend.setToolTipText("Click a square: untrained → proficient → expertise → untrained. Bonuses calculate from your ability scores and level.");
            Set<Skill> granted=session.grantedSkills();
            skillRows.values().forEach(row->row.refresh(sheet,state.charisma(),stats.proficiencyBonus(),granted,session.expertiseSkills()));
            passives.forEach((skill,value)->value.setText(Integer.toString(10+session.skillBonus(skill,state.charisma()))));
            choices.refresh();
            List<String> tools=session.grantedTools();
            grantedTools.setText(tools.isEmpty()?"":"Granted tool proficiencies: "+String.join(", ",tools));grantedTools.setVisible(!tools.isEmpty());
            int remaining=Math.max(0,state.level()-sheet.field(Field.SPENT_HIT_DICE));hitDice.setText("HIT DICE "+remaining+" / "+state.level()+" (d6)");spendHitDie.setEnabled(remaining>0 && session.hitPoints()>0 && session.hitPoints()<session.maxHitPoints());
            boolean canRest=session.maxHitPoints()==0 || session.hitPoints()>0;shortRest.setEnabled(canRest);longRest.setEnabled(canRest);
            shortRest.setToolTipText(canRest?"Record a completed Short Rest":"A rest must start with at least 1 HP");longRest.setToolTipText(canRest?"Record a completed Long Rest":"A rest must start with at least 1 HP");
            ((SpinnerNumberModel)sorceryPoints.getModel()).setMaximum(session.maxPoints());sorceryPoints.setValue(session.remainingPoints());sorceryPoints.setEnabled(session.maxPoints()>0);
            spellSlots.forEach((slot,spinner)->{((SpinnerNumberModel)spinner.getModel()).setMaximum(session.maxSlots(slot));spinner.setValue(session.remainingSlots(slot));spinner.setEnabled(session.maxSlots(slot)>0);});
            innate.setText(session.innateActive()?"Innate active":"Innate Sorcery "+Math.max(0,2-state.play().innateUses())+" / 2");
            innate.putClientProperty("tactileRole",session.innateActive()?"red":"blue");save.setEnabled(workspace.canSaveNow() && !name.getText().isBlank());save.setToolTipText(workspace.isSaveLocked()?"Unlock the saved character before committing experiments":"Save this sheet and character to the grimoire");discard.setEnabled(workspace.canRestoreSaved());
            status.setText(editor.saveStatusText());refreshFeatures(state);AppStyle.applyAppearance(this);
        }finally{updating=false;}
    }
    private void refreshFeatures(CharacterState state){
        String configured=session.sheet().values().entrySet().stream().filter(entry->entry.getKey().startsWith("choice.")).sorted(Map.Entry.comparingByKey()).toList().toString();
        String signature=state.level()+":"+state.subclass()+":"+state.species()+":"+session.metamagic()+":"+configured;
        if(!signature.equals(featureSignature)){
            featureSignature=signature;features.removeAll();
            String speciesChoices=CharacterChoices.speciesSummary(session.sheet(),state.species());
            FeatComponents.row(features,FeatComponents.block("SUBCLASS & LINEAGE",FeatComponents.text(state.subclass()+"\n"+state.species()+(speciesChoices.isBlank()?"":"\n"+speciesChoices)+(state.level()<3?"\nSubclass features unlock at level 3.":""),12)));
            for(Metamagic option:session.metamagic().stream().sorted(Comparator.comparing(Metamagic::toString)).toList()){
                JLabel meta=FeatComponents.tag(option+" · "+option.cost()+" SP",false);meta.setToolTipText(option.description());FeatComponents.row(features,meta);
            }
            List<SpellCatalog.Spell> grants=session.selection().catalog().stream().filter(session.selection()::automatic).filter(session.selection()::available).toList();
            if(!grants.isEmpty()){
                JPanel buttons=new FeatComponents.WrapPanel(4);for(SpellCatalog.Spell spell:grants)buttons.add(AppStyle.compactButton(spell.name(),()->AppStyle.openWindow(this,spell.name(),new SpellDetailPanel(spell))));
                FeatComponents.row(features,FeatComponents.block("AUTOMATIC SPELL GRANTS",buttons));
            }
            features.revalidate();features.repaint();
        }
        String feats=session.feats()+":"+configured;if(!feats.equals(featSignature)){
            featSignature=feats;featList.removeAll();
            session.feats().entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry->{
                JPanel row=AppStyle.section(new BorderLayout(8,4));JButton title=AppStyle.textButton(entry.getKey()+" · "+FeatCatalog.named(entry.getKey()).category(),()->workspace.inspectFeat(entry.getKey()));
                String config=session.featConfiguration(entry.getKey());
                row.add(title,BorderLayout.NORTH);row.add(FeatComponents.text((entry.getValue().isBlank()?FeatCatalog.named(entry.getKey()).summary():entry.getValue())+(config.isBlank()?"":"\n"+config),12),BorderLayout.CENTER);FeatComponents.row(featList,row);
            });if(session.feats().isEmpty())FeatComponents.row(featList,AppStyle.label("No feats selected",12,false));featList.revalidate();featList.repaint();
        }
    }
    private void spendHitDie(){
        CharacterState state=editor.snapshot();CharacterSheet sheet=session.sheet();int spent=sheet.field(Field.SPENT_HIT_DICE);
        if(spent>=state.level() || session.hitPoints()==0)return;
        int die=java.util.concurrent.ThreadLocalRandom.current().nextInt(1,7),healing=sheet.hitDieHealing(die);
        session.setSheet(sheet.withField(Field.SPENT_HIT_DICE,spent+1));session.setCurrentHitPoints(Math.min(session.maxHitPoints(),session.hitPoints()+healing));
        workspace.combatPanel().recordEvent("Short Rest Hit Die: d6 ["+die+"] + CON = "+healing+" HP (limited by maximum HP)");
    }
    private void rollSkill(Skill skill){CharacterState state=editor.snapshot();int bonus=session.skillBonus(skill,state.charisma());int die=java.util.concurrent.ThreadLocalRandom.current().nextInt(1,21);workspace.combatPanel().recordEvent(skill.label+" check: d20 ["+die+"] "+signed(bonus)+" = "+(die+bonus));status.setText(skill.label+" roll recorded in combat log");}
    private void commit(){if(name.getText().strip().isEmpty()){status.setText("Enter a character name before saving.");return;}workspace.saveNow();}
    private void printSheet(){
        CharacterState state=editor.snapshot();CharacterSheet sheet=session.sheet();var stats=SorcererRules.calculate(state.level(),state.charisma(),state.subclass());
        JTextArea document=new JTextArea(state.name()+" // SORCERER "+state.level()+"\n"+state.subclass()+" // "+state.species()+"\n\n");
        document.setLineWrap(true);document.setWrapStyleWord(true);
        document.append("Background: "+sheet.note(Note.BACKGROUND)+"; Alignment: "+sheet.note(Note.ALIGNMENT)+"\n");
        for(Ability ability:Ability.values())document.append(ability+" "+sheet.score(ability,state.charisma())+" ("+signed(CharacterSheet.modifier(sheet.score(ability,state.charisma())))+")\n");
        for(Skill skill:Skill.values())document.append(skill.label+" "+signed(session.skillBonus(skill,state.charisma()))+"\n");
        document.append("\nHP "+session.hitPoints()+" / "+session.maxHitPoints()+"; Temp HP "+sheet.field(Field.TEMP_HP)+"\n"+sheet.note(Note.TRAINING)+"\n"+sheet.note(Note.LANGUAGES)+"\n\nFeats\n");session.feats().forEach((feat,notes)->document.append(feat+": "+notes+"\n"));
        document.append("\nConfirmed creation choices\n");
        session.requiredChoices().stream().filter(choice->choice.complete(sheet)).forEach(choice->document.append(choice.title()+": "+String.join(", ",sheet.choice(choice.id()))+"\n"));
        document.append("\nSorcery Points "+session.remainingPoints()+" / "+session.maxPoints()+"\n");
        for(int tier=1;tier<=9;tier++)if(session.maxSlots(tier)>0)document.append("Level "+tier+" slots: "+session.remainingSlots(tier)+" / "+session.maxSlots(tier)+"\n");
        document.append("\nKnown / prepared spells\n");session.selection().catalog().stream().filter(session.selection()::selected).filter(session.selection()::available).forEach(spell->document.append(spell.levelName()+": "+spell.name()+"\n"));
        try{document.print();}catch(java.awt.print.PrinterException error){status.setText("Could not print sheet: "+error.getMessage());}
    }
    public void dispose(){disposed=true;session.removeListener(listener);session.selection().removeChangeListener(listener);editor.removePropertyChangeListener("characterState",stateListener);}
}
