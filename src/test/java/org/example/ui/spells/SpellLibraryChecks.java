package org.example.ui.spells;

import org.example.domain.character.*;
import org.example.domain.spells.*;
import org.example.ui.GrimoirePanel;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

public final class SpellLibraryChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var workspace=new GrimoirePanel(new CharacterState(3,17,SorcererRules.Subclass.ABERRANT,Set.of("Fire Bolt","Shield"),Species.HUMAN));
            var frame=new JFrame();frame.setContentPane(workspace);frame.setSize(1480,960);frame.setVisible(true);
            try {
                workspace.showPage(GrimoirePanel.LIBRARY);var library=find(workspace,SpellLibraryPanel.class,"Spell Library");
                check(find(library,JButton.class,"Choose Mind Sliver").getText().equals("Always prepared"),"Prepared page includes automatic grants");
                JTextField search=find(library,JTextField.class,"Search library spells");
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().size(),"Catalog includes class and subclass entries");
                search.setText("Fire Bolt");check(library.visibleSpellCount()==1,"Search filters spell cards");
                JPanel fireBoltCard=find(library,JPanel.class,"Spell card Fire Bolt");
                library.inspect("Shield");click(fireBoltCard);check(library.inspectedSpell().equals("Fire Bolt"),"Clicking card background updates inspector");
                library.inspect("Shield");click(find(fireBoltCard,JLabel.class,null));check(library.inspectedSpell().equals("Fire Bolt"),"Clicking level/badge labels updates inspector");
                library.inspect("Shield");click(find(fireBoltCard,JTextArea.class,null));check(library.inspectedSpell().equals("Fire Bolt"),"Clicking casting metadata updates inspector");
                find(library,JButton.class,"Choose Fire Bolt").doClick(0);
                check(!workspace.playSession().selection().chosenSpells().contains("Fire Bolt"),"Unlearn updates shared model");
                check(library.inspectedSpell().equals("Fire Bolt"),"Unlearn also inspects its spell");
                check(find(library,JButton.class,"Choose Fire Bolt").getText().equals("Learn"),"Unselected cantrips use Learn");
                find(library,JButton.class,"Choose Fire Bolt").doClick(0);
                check(workspace.playSession().selection().chosenSpells().contains("Fire Bolt"),"Learn updates shared model");
                check(find(library,JButton.class,"Choose Fire Bolt").getText().equals("Unlearn"),"Selected cantrips use Unlearn");
                library.inspect("Shield");click(find(fireBoltCard,JLabel.class,null));check(library.inspectedSpell().equals("Fire Bolt"),"Card inspection still works after selection refreshes");
                library.inspect("Shield");AbstractButton details=findText(library,"Full details");
                workspace.playSession().setRemainingPoints(2);
                check(findText(library,"Full details")==details,"Unrelated resources preserve inspector controls");
                search.setText("Fireball");library.inspect("Fireball");
                check(!find(library,JButton.class,"Choose Fireball").isEnabled(),"Locked spells remain inspectable");
                workspace.characterPanel().setLevel(5);check(find(library,JButton.class,"Choose Fireball").isEnabled(),"Live level changes unlock spell cards");
                reset(library);find(library,JToggleButton.class,"Library tier 2").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->s.level()==2).count(),"Tier tab filters correctly");
                find(library,JToggleButton.class,"Library tier 0").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->s.level()==0||s.level()==2).count(),"Multiple tiers combine");
                find(library,JToggleButton.class,"Library tier 2").doClick(0);
                find(library,JToggleButton.class,"Library tier 0").doClick(0);
                check(find(library,JToggleButton.class,"Library tier -1").isSelected()&&library.visibleSpellCount()==workspace.playSession().selection().catalog().size(),"Deselecting every tier restores All");
                reset(library);find(library,JToggleButton.class,"Library school Evocation").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->SpellDetails.forSpell(s).school().equals("Evocation")).count(),"School filters correctly");
                find(library,JToggleButton.class,"Library school Illusion").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->Set.of("Evocation","Illusion").contains(SpellDetails.forSpell(s).school())).count(),"Multiple schools combine");
                find(library,JToggleButton.class,"Library tier 0").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->s.level()==0&&Set.of("Evocation","Illusion").contains(SpellDetails.forSpell(s).school())).count(),"Tier and school groups intersect");
                find(library,JToggleButton.class,"Library school Evocation").doClick(0);
                find(library,JToggleButton.class,"Library school Illusion").doClick(0);
                check(find(library,JToggleButton.class,"Library school All").isSelected(),"Deselecting every school restores All");
                reset(library);findText(library,"Concentration").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->SpellDetails.forSpell(s).concentration()).count(),"Concentration filter uses real spell rules");
                reset(library);findText(library,"Psionic").doClick(0);
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(s->SpellCatalog.unlockLevel(s,SorcererRules.Subclass.ABERRANT)>0).count(),"Psionic filter identifies Aberrant grants");
                reset(library);check(findText(library,"Next >")==null && findText(library,"< Prev")==null,"Pagination controls are removed");
                check(find(library,JButton.class,"Choose Wish")!=null,"Last-tier spells are present in the continuous list");
                check(find(library,JToggleButton.class,"Library tier 2").getFont().getSize()>=14,"Filter buttons have readable text");
                search.setText("Shield");library.inspect("Shield");int slots=workspace.playSession().remainingSlots(1);
                check(find(library,JButton.class,"Send Shield to combat")==null && findText(library,"Prepared spells")==null && findText(library,"Reset")==null,"Removed footer and Reset actions are absent");
                check(find(library,JComboBox.class,null)==null,"Sort dropdown is removed");
                check(find(library,JButton.class,"Cast Shield")==null,"Library cards have no Cast action");
                check(find(library,JButton.class,"Choose Shield").getText().equals("Unlearn"),"Manual leveled spells use Unlearn");
                find(library,JButton.class,"Choose Shield").doClick(0);
                check(find(library,JButton.class,"Choose Shield").getText().equals("Learn"),"Unselected leveled spells use Learn");
                find(library,JButton.class,"Choose Shield").doClick(0);
                check(workspace.playSession().remainingSlots(1)==slots,"Learning and unlearning do not spend spell slots");
                workspace.showPage(GrimoirePanel.LIBRARY);findText(library,"Full details").doClick(0);
                check(frame.getOwnedWindows().length>0,"Full details opens a real window");for(Window owned:frame.getOwnedWindows())owned.dispose();
                reset(library);library.inspect("Shield");
                JButton automatic=find(library,JButton.class,"Choose Mind Sliver");
                check(automatic.getText().equals("Always prepared")&&!automatic.isEnabled(),"Automatic grants are Always prepared and cannot be unlearned");

                workspace.showPreparedSpells();check(findText(library,"Prepared only").isSelected(),"Review link enables Prepared only in Spell Library");
                var choices=new java.util.HashSet<>(workspace.playSession().selection().chosenSpells());choices.add("Fireball");workspace.playSession().selection().restoreChoices(choices);workspace.characterPanel().setLevel(3);
                check(find(library,JButton.class,"Choose Fireball")!=null&&find(library,JButton.class,"Choose Fireball").isEnabled(),"Dormant prepared choices remain visible and removable after lowering level");
                check(library.visibleSpellCount()==workspace.playSession().selection().catalog().stream().filter(workspace.playSession().selection()::selected).count(),"Prepared-only library includes chosen spells and automatic grants");
                check(find(library,JButton.class,"Choose Fireball")!=null,"Dormant spells remain reviewable in Prepared only");
                find(library,JButton.class,"Choose Fireball").doClick(0);check(find(library,JButton.class,"Choose Fireball")==null,"Unlearn updates the prepared-only list live");
                reset(library);check(find(library,JButton.class,"Choose Wish")!=null,"Unchecking Prepared only restores the full catalog");
                for(boolean dark:new boolean[]{false,true})for(int size:new int[]{12,14,16,18}) {
                    workspace.playSession().appearance(dark,size);frame.setSize(1480,960);frame.validate();layout(workspace);preview(workspace,"target/spell-library-"+(dark?"dark":"light")+"-"+size+".png");
                }
                frame.setSize(960,1050);workspace.playSession().appearance(false,18);frame.validate();layout(workspace);frame.validate();layout(workspace);preview(workspace,"target/spell-library-narrow-18.png");
            }finally{frame.dispose();workspace.dispose();}
        });
        System.out.println("Passed: spell cards, counts, tier/school/rule filters, continuous scrolling, locked inspection, live level/selection updates, stable resource refreshes, Learn/Unlearn/Always prepared states and Full details actions and theme previews.");
    }
    private static void reset(Container library){
        find(library,JTextField.class,"Search library spells").setText("");
        find(library,JToggleButton.class,"Library tier -1").doClick(0);
        find(library,JToggleButton.class,"Library school All").doClick(0);
        for(String text:new String[]{"Concentration","Psionic","Ritual","Prepared only"}){AbstractButton button=findText(library,text);if(button.isSelected())button.doClick(0);}
    }
    private static void click(Component component){component.dispatchEvent(new java.awt.event.MouseEvent(component,java.awt.event.MouseEvent.MOUSE_CLICKED,System.currentTimeMillis(),0,2,2,1,false,java.awt.event.MouseEvent.BUTTON1));}
    private static AbstractButton findText(Container root,String text){for(Component child:root.getComponents()){if(child instanceof AbstractButton b&&text.equals(b.getText()))return b;if(child instanceof Container nested){var found=findText(nested,text);if(found!=null)return found;}}return null;}
    private static <T extends Component>T find(Container root,Class<T> type,String name){for(Component child:root.getComponents()){if(type.isInstance(child)&&(name==null||name.equals(child.getAccessibleContext().getAccessibleName())))return type.cast(child);if(child instanceof Container nested){T found=find(nested,type,name);if(found!=null)return found;}}return null;}
    private static void layout(Container root){root.doLayout();for(Component child:root.getComponents())if(child instanceof Container nested)layout(nested);}
    private static void preview(JComponent view,String file){layout(view);BufferedImage image=new BufferedImage(view.getWidth(),view.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();view.printAll(g);g.dispose();try{ImageIO.write(image,"png",Path.of(file).toFile());}catch(Exception error){throw new RuntimeException(error);}}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
