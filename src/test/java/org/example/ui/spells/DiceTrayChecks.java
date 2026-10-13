package org.example.ui.spells;

import org.example.domain.character.*;
import org.example.domain.spells.*;
import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Set;

public final class DiceTrayChecks {
    public static void main(String[] args)throws Exception {
        SwingUtilities.invokeAndWait(()->{
            SpellSelection selection=new SpellSelection();selection.setCharacter(20,SorcererRules.Subclass.ABERRANT);
            selection.restoreChoices(Set.of("Fire Bolt","Fireball","Scorching Ray","Hold Person","Shield","Web","Ice Knife","Ice Storm","Mirror Image","Blink","False Life","Ray of Enfeeblement","Blade Ward"));
            PlaySession session=new PlaySession(selection);CastingPanel panel=new CastingPanel(session,()->17);
            try {
                for(String name:List.of("Hold Person","Shield","Web","Blade Ward")) {
                    check(panel.selectSpell(name),"Selected "+name);check(!panel.isDiceTrayVisible(),"No caster dice for "+name);
                }
                check(SpellDice.forSpell(spell("Ray of Enfeeblement"),2,20).isEmpty(),"Enemy damage penalties are not caster rolls");
                verify(panel,"Fire Bolt",List.of(10,20));var before=session.snapshot();
                button(panel,"d20").doClick(0);check(panel.combatLogText().contains("attack") && panel.combatLogText().contains("+9"),"d20 uses spell attack modifier");
                button(panel,"d10").doClick(0);check(session.snapshot().equals(before),"Rolling does not spend spell resources");
                verify(panel,"Fireball",List.of(6));
                check(!button(panel,"Crit ×2 dice").isVisible(),"Save damage has no critical attack control");
                button(panel,"Quick roll damage").doClick(0);check(panel.combatLogText().contains("8d6"),"Quick damage rolls the selected spell formula");
                verify(panel,"Scorching Ray",List.of(6,20));check(button(panel,"Crit ×2 dice").isVisible(),"Attack damage supports critical dice");
                verify(panel,"Ice Knife",List.of(6,10,20));verify(panel,"Ice Storm",List.of(6,10));
                check(!button(panel,"Quick roll damage").isVisible(),"Mixed damage requires its separate spell components");
                verify(panel,"Blink",List.of(6));verify(panel,"Mirror Image",List.of(6));verify(panel,"False Life",List.of(4));
                check(SpellDice.forSpell(spell("True Strike"),0,1).equals(List.of(20)),"Unknown weapon die is not guessed");
                check(SpellDice.forSpell(spell("True Strike"),0,5).equals(List.of(6,20)),"True Strike shows its unlocked bonus damage die");
                check(SpellDice.forSpell(spell("Teleport"),7,20).equals(List.of(10,100)),"Teleport has table and conditional mishap dice");
                for(var spell:SpellCatalog.all())check(SpellDice.forSpell(spell,spell.level(),20).stream().allMatch(Set.of(4,6,8,10,12,20,100)::contains),"Valid dice for "+spell.name());
                selection.restoreChoices(Set.of());selection.setCharacter(1,SorcererRules.Subclass.WILD_MAGIC);
                check(!panel.isDiceTrayVisible(),"Empty spell selection hides the tray");
            }finally{panel.dispose();}
        });
        System.out.println("Passed: live dice visibility, save-only effects, attack modifiers, save damage, mixed profiles, effect dice, cantrip scaling, empty selection, and unchanged resources.");
    }
    private static SpellCatalog.Spell spell(String name){return SpellCatalog.all().stream().filter(s->s.name().equals(name)).findFirst().orElseThrow();}
    private static void verify(CastingPanel panel,String name,List<Integer> dice){check(panel.selectSpell(name),"Selected "+name);check(panel.isDiceTrayVisible() && panel.visibleDiceSides().equals(dice),"Correct dice for "+name+": "+panel.visibleDiceSides());}
    private static JButton find(Container root,String text){for(Component c:root.getComponents()){if(c instanceof JButton b && text.equals(b.getText()))return b;if(c instanceof Container child){JButton found=find(child,text);if(found!=null)return found;}}return null;}
    private static JButton button(Container root,String text){JButton result=find(root,text);if(result==null)throw new AssertionError("Missing "+text);return result;}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
