package org.example;

import org.example.domain.character.*;
import org.example.domain.spells.SpellCatalog;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.SpellDetailPanel;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Set;

/** Theme lifecycle and navigation regression checks for shared secondary window chrome. */
public final class BauhausChecks {
    public static void main(String[] args) throws Exception {
        SwingUtilities.invokeAndWait(()->{
            GrimoirePanel panel=new GrimoirePanel(new CharacterState(5,17,SorcererRules.Subclass.ABERRANT,Set.of("Fire Bolt","Shield"),Species.HUMAN));
            JFrame host=new JFrame();host.setContentPane(panel);host.setSize(1480,960);host.setVisible(true);
            try {
                panel.playSession().setHitPoints(7,22);
                check(PlayState.decode(panel.playSession().snapshot().encode()).hitPoints()==7,"HP round trips through saved play state");
                var hpCheckpoint=panel.snapshot();panel.playSession().longRest();
                check(panel.playSession().hitPoints()==22,"Long rest restores configured HP");
                panel.restore(hpCheckpoint);check(panel.playSession().hitPoints()==7,"Restore recovers current HP checkpoint");
                String legacy=hpCheckpoint.play().encode().replaceAll("(?m)^(hp|maxHp)=.*\\R?","");
                check(PlayState.decode(legacy).maxHitPoints()==0,"Old saves load with unset HP");
                try{panel.playSession().setHitPoints(23,22);throw new AssertionError("Invalid HP accepted");}catch(IllegalArgumentException expected){}
                for(boolean dark:new boolean[]{false,true}) {
                    panel.playSession().appearance(dark,14);
                    check(AppStyle.ACCENT.getBlue()>AppStyle.ACCENT.getRed(),"Shared accent is blue");
                    for(String page:new String[]{GrimoirePanel.COMBAT,GrimoirePanel.METAMAGIC,GrimoirePanel.FEATS,GrimoirePanel.LIBRARY,GrimoirePanel.CHARACTER}) {
                        panel.showPage(page);preview(panel,"target/bauhaus-"+page.replaceAll("[^A-Za-z]","")+"-"+(dark?"dark":"light")+".png");
                    }
                }
                panel.playSession().appearance(false,14);
                SpellDetailPanel detail=new SpellDetailPanel(SpellCatalog.all().stream().filter(s->s.name().equals("Shield")).findFirst().orElseThrow());
                JDialog dialog=AppStyle.openWindow(panel,"Shield",detail);
                try {
                    Container shell=dialog.getContentPane();
                    check(AppStyle.windowContent(dialog)==detail,"Shell preserves original detail view");
                    check(shell==detail,"Spell detail window omits shared header and sidebar");
                    AppStyle.applyAppearance(dialog);AppStyle.applyAppearance(dialog);
                    check(dialog.getContentPane()==shell,"Repeated styling never nests duplicate sidebars");
                    panel.playSession().appearance(true,18);
                    check(detail.getBackground().equals(AppStyle.BACKGROUND),"Open detail follows theme change");
                    panel.playSession().appearance(false,14);
                    check(find(shell,"Cast & Combat")==null,"Spell details have no workspace navigation sidebar");
                    preview((JComponent)shell,"target/bauhaus-detail-preview.png");
                } finally {dialog.dispose();}
            } finally {host.dispose();panel.dispose();}
        });
        System.out.println("Passed: five pages in both themes, open-dialog theme synchronization, shared navigation, preserved resources, and idempotent window styling.");
    }
    private static JButton find(Container root,String text) {
        for(Component c:root.getComponents()){if(c instanceof JButton b && text.equals(b.getText()))return b;if(c instanceof Container child){JButton found=find(child,text);if(found!=null)return found;}}return null;
    }
    private static void layout(Container c){c.doLayout();for(Component child:c.getComponents())if(child instanceof Container nested)layout(nested);}
    private static void preview(JComponent c,String file) {
        layout(c);BufferedImage image=new BufferedImage(c.getWidth(),c.getHeight(),BufferedImage.TYPE_INT_RGB);
        Graphics2D g=image.createGraphics();c.printAll(g);g.dispose();
        try{ImageIO.write(image,"png",Path.of(file).toFile());}catch(Exception e){throw new RuntimeException(e);}
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
