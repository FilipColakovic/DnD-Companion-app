package org.example.ui.character;

import org.example.domain.character.*;
import org.example.domain.character.CharacterSheet.*;
import org.example.domain.spells.*;
import org.example.ui.common.AppStyle;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

/** Creation choices, source-aware grants, exact confirmation, draft stability and legacy persistence. */
public final class CharacterChoicesChecks {
    public static void main(String[] args)throws Exception{
        Map<Species,Integer> expected=Map.ofEntries(Map.entry(Species.AASIMAR,1),Map.entry(Species.DRAGONBORN,1),Map.entry(Species.DWARF,0),
            Map.entry(Species.DROW,2),Map.entry(Species.HIGH_ELF,2),Map.entry(Species.WOOD_ELF,2),Map.entry(Species.FOREST_GNOME,1),Map.entry(Species.ROCK_GNOME,1),
            Map.entry(Species.GOLIATH,1),Map.entry(Species.HALFLING,0),Map.entry(Species.HUMAN,3),Map.entry(Species.ORC,0),
            Map.entry(Species.ABYSSAL_TIEFLING,2),Map.entry(Species.CHTHONIC_TIEFLING,2),Map.entry(Species.INFERNAL_TIEFLING,2));
        expected.forEach((species,count)->check(CharacterChoices.speciesChoices(species).size()==count,"PHB creation choices for "+species));
        check(CharacterChoices.SORCERER_SKILLS.options().equals(List.of("Arcana","Deception","Insight","Intimidation","Persuasion","Religion")),"Six audited Sorcerer options");
        CharacterSheet data=CharacterSheet.defaults().withChoice("sorcerer.skills",List.of("Arcana","Deception"))
            .withChoice("human.skill",List.of("Perception")).withChoice("elf.skill",List.of("Survival")).withChoice("human.feat",List.of("Skilled"))
            .withChoice("feat.Skilled",List.of("Insight","Athletics","Tool: Thieves' Tools")).withSkill(Skill.PERSUASION,2);
        Set<Skill> human=CharacterChoices.skills(data,Species.HUMAN,CharacterChoices.feats(data,Species.HUMAN,Map.of()).keySet());
        check(human.containsAll(Set.of(Skill.ARCANA,Skill.DECEPTION,Skill.PERCEPTION,Skill.INSIGHT,Skill.ATHLETICS))&&!human.contains(Skill.SURVIVAL),"Active Human and Skilled grants apply");
        Set<Skill> elf=CharacterChoices.skills(data,Species.HIGH_ELF,Set.of());
        check(elf.contains(Skill.SURVIVAL)&&!elf.contains(Skill.PERCEPTION)&&!elf.contains(Skill.ATHLETICS),"Changing species removes only inactive source grants");
        check(data.tier(Skill.PERSUASION,elf)==2 && data.tier(Skill.ARCANA,human)==1,"Manual expertise and class grants coexist");
        check(data.skillBonus(Skill.ARCANA,17,2,human)==2,"Granted proficiency enters computed bonuses");
        try{data.withChoice("sorcerer.skills",List.of("Arcana"));throw new AssertionError("Accepted incomplete selection");}catch(IllegalArgumentException expectedError){}
        try{data.withChoice("sorcerer.skills",List.of("Arcana","Arcana"));throw new AssertionError("Accepted duplicate selection");}catch(IllegalArgumentException expectedError){}
        PlayState encoded=new PlayState(List.of(0,0,0,0,0,0,0,0,0),0,false,Set.of(),"",Map.of(),0,false,false,14,0,0,Map.of(),data);
        check(PlayState.decode(encoded.encode()).equals(encoded),"Creation choices round-trip in the existing save format");
        check(PlayState.decode("points=0\n").sheet().choice("sorcerer.skills").isEmpty(),"Legacy saves retain pending reminders");
        SwingUtilities.invokeAndWait(()->{
            AppStyle.appearance(false,14);SpellSelection selection=new SpellSelection();selection.setCharacter(3,SorcererRules.Subclass.WILD_MAGIC,Species.HUMAN);
            PlaySession session=new PlaySession(selection);CharacterChoicesPanel panel=new CharacterChoicesPanel(session);
            session.addListener(panel::refresh);selection.addChangeListener(panel::refresh);panel.refresh();
            JFrame frame=new JFrame();frame.setContentPane(panel);frame.setSize(1100,850);frame.setVisible(true);
            try{
                JButton confirm=accessible(panel,JButton.class,"Confirm sorcerer.skills");check(!confirm.isEnabled(),"Select disabled until exactly two skills");
                JCheckBox arcana=accessible(panel,JCheckBox.class,"sorcerer.skills option Arcana"),deception=accessible(panel,JCheckBox.class,"sorcerer.skills option Deception");
                arcana.doClick(0);session.setRemainingPoints(1);check(arcana.isSelected(),"Resource changes preserve incomplete checkbox drafts");
                deception.doClick(0);check(confirm.isEnabled()&&!accessible(panel,JCheckBox.class,"sorcerer.skills option Insight").isEnabled(),"Reached count blocks an extra option but permits unchecking");
                confirm.doClick(0);check(named(panel,"choice-reminder-sorcerer.skills")==null,"Confirmed reminder disappears");
                check(CharacterChoices.skills(session.sheet(),Species.HUMAN,session.feats().keySet()).contains(Skill.DECEPTION),"Confirmed skill is granted");
                session.setSheet(session.sheet().withChoice("human.feat",List.of("Skilled")));check(named(panel,"choice-reminder-feat.Skilled")!=null,"Origin feat creates its configuration reminder");
                selection.setCharacter(3,SorcererRules.Subclass.WILD_MAGIC,Species.HIGH_ELF);
                check(named(panel,"choice-reminder-human.skill")==null&&named(panel,"choice-reminder-elf.skill")!=null,"Species switch shows only relevant reminders");
                check(!session.feats().containsKey("Skilled"),"Human Origin feat is dormant after switching species");
                session.setSheet(session.sheet().withChoice("species.HIGH_ELF.casting",List.of("Intelligence")).withAbility(Ability.INT,18));
                var detect=SpellCatalog.all().stream().filter(spell->spell.name().equals("Detect Magic")).findFirst().orElseThrow();
                check(session.spellcastingAbility(detect)==Ability.INT && session.spellcastingModifier(detect,17)==4,"Species spells use selected lineage ability");
                session.setSheet(session.sheet().withChoice("elf.skill",List.of("Perception")));
                check(CharacterChoices.required(session.sheet(),Species.HIGH_ELF,session.feats().keySet()).stream().allMatch(choice->choice.complete(session.sheet())),"All High Elf reminders complete");
                selection.setCharacter(3,SorcererRules.Subclass.WILD_MAGIC,Species.HUMAN);
                session.setSheet(session.sheet().withChoice("human.feat",List.of("Magic Initiate")).withChoice("feat.Magic Initiate.list",List.of("Cleric")));
                check(accessible(panel,JCheckBox.class,"feat.Magic Initiate.cantrips option Word of Radiance")!=null,"PHB cantrip choices include non-Free-Rules entries");
                session.setSheet(session.sheet().withChoice("feat.Magic Initiate.cantrips",List.of("Light","Guidance")).withChoice("feat.Magic Initiate.spell",List.of("Bless")));
                session.setSheet(session.sheet().withChoice("feat.Magic Initiate.list",List.of("Wizard")));
                check(named(panel,"choice-reminder-feat.Magic Initiate.cantrips")!=null,"A spell-list change invalidates incompatible saved choices");
                session.restore(PlayState.defaults());check(named(panel,"choice-reminder-sorcerer.skills")!=null,"Checkpoint restoration restores pending choices");
                for(Species species:Species.values()){selection.setCharacter(3,SorcererRules.Subclass.WILD_MAGIC,species);panel.refresh();}
                selection.setCharacter(3,SorcererRules.Subclass.WILD_MAGIC,Species.HUMAN);
                for(boolean dark:new boolean[]{false,true})for(int size:new int[]{14,18}){
                    AppStyle.appearance(dark,size);AppStyle.applyAppearance(panel);layout(frame);layout(frame);
                    BufferedImage image=new BufferedImage(panel.getWidth(),panel.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();panel.printAll(g);g.dispose();
                    try{ImageIO.write(image,"png",Path.of("target/creation-choices-"+(dark?"dark":"light")+"-"+size+".png").toFile());}catch(Exception error){throw new RuntimeException(error);}
                }
            }finally{frame.dispose();}
        });
        SwingUtilities.invokeAndWait(()->{
            org.example.ui.GrimoirePanel workspace=new org.example.ui.GrimoirePanel(new CharacterState(3,17,SorcererRules.Subclass.WILD_MAGIC,Set.of(),Species.HUMAN));
            try{
                workspace.showPage(org.example.ui.GrimoirePanel.CHARACTER);
                PlaySession session=workspace.playSession();
                session.setSheet(session.sheet().withChoice("sorcerer.skills",List.of("Arcana","Deception"))
                    .withChoice("human.skill",List.of("Perception")).withChoice("elf.skill",List.of("Survival")));
                check(((JLabel)named(workspace,"skill-bonus-DECEPTION")).getText().equals("+5"),"Class proficiency updates the real skill row");
                check(((JLabel)named(workspace,"passive-PERCEPTION")).getText().equals("12"),"Human choice updates the real passive sense");
                accessible(workspace,JButton.class,"Proficiency for Deception").doClick(0);
                check(((JLabel)named(workspace,"skill-bonus-DECEPTION")).getText().equals("+7"),"Granted skill can be promoted to expertise");
                accessible(workspace,JButton.class,"Proficiency for Deception").doClick(0);
                check(((JLabel)named(workspace,"skill-bonus-DECEPTION")).getText().equals("+5"),"Expertise toggles back to its granted proficiency");
                workspace.characterPanel().selectSpecies(Species.HIGH_ELF);
                check(((JLabel)named(workspace,"skill-bonus-PERCEPTION")).getText().equals("+0")&&((JLabel)named(workspace,"skill-bonus-SURVIVAL")).getText().equals("+2"),"Actual rows deactivate Human and activate Elf skill grants");
            }finally{workspace.dispose();}
        });
        System.out.println("Passed: all PHB species choices, source-aware skills and feats, confirmation, drafts, lineage abilities, Origin feat choices, themes and persistence.");
    }
    private static void layout(Container container){container.doLayout();for(Component child:container.getComponents())if(child instanceof Container nested)layout(nested);}
    private static Component named(Container root,String name){for(Component child:root.getComponents()){if(name.equals(child.getName()))return child;if(child instanceof Container nested){Component found=named(nested,name);if(found!=null)return found;}}return null;}
    private static <T extends Component>T accessible(Container root,Class<T> type,String name){for(Component child:root.getComponents()){if(type.isInstance(child)&&name.equals(child.getAccessibleContext().getAccessibleName()))return type.cast(child);if(child instanceof Container nested){T found=accessible(nested,type,name);if(found!=null)return found;}}return null;}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
