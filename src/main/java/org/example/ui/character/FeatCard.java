package org.example.ui.character;

import org.example.domain.character.*;
import org.example.ui.common.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.Locale;

/** Compact library card; its full rules and editable choices live in the inspector. */
final class FeatCard extends JPanel {
    private final FeatCatalog.Feat feat;
    private final JLabel status=FeatComponents.tag("",false);
    private final JButton choose;
    private final JPanel card=FeatComponents.card(new BorderLayout(0,7));

    FeatCard(FeatCatalog.Feat feat,Runnable inspect,Runnable select) {
        super(new BorderLayout());this.feat=feat;setOpaque(false);
        getAccessibleContext().setAccessibleName("Feat card "+feat.name());
        add(card,BorderLayout.CENTER);
        JPanel heading=AppStyle.section(new BorderLayout(8,0));
        JPanel tile=AppStyle.card(new BorderLayout());
        tile.putClientProperty("surfaceRole",feat.category()==FeatCatalog.Category.ORIGIN?"slotAvailable":"low");
        tile.putClientProperty("shadowOffset",2);tile.setBorder(new EmptyBorder(3,3,5,5));
        tile.setPreferredSize(new Dimension(44,44));
        JLabel level=AppStyle.mono(""+feat.minimumLevel(),20);level.setHorizontalAlignment(SwingConstants.CENTER);
        AppStyle.setColorRole(level,feat.category()==FeatCatalog.Category.ORIGIN?"badgeText":"ink");
        level.setToolTipText("Minimum level "+feat.minimumLevel());tile.add(level);
        JPanel tileHolder=AppStyle.section(new FlowLayout(FlowLayout.LEFT,0,0));tileHolder.add(tile);
        heading.add(tileHolder,BorderLayout.WEST);
        JButton title=FeatComponents.title(feat.name().toUpperCase(Locale.ROOT),inspect);
        title.getAccessibleContext().setAccessibleName("Inspect "+feat.name());heading.add(title,BorderLayout.CENTER);
        choose=AppStyle.compactButton("Equip",()->{select.run();inspect.run();});
        choose.getAccessibleContext().setAccessibleName("Select or remove "+feat.name());
        JPanel actions=AppStyle.section(new FlowLayout(FlowLayout.RIGHT,0,0));actions.add(choose);
        heading.add(actions,BorderLayout.EAST);card.add(heading,BorderLayout.NORTH);
        JPanel content=AppStyle.section(new BorderLayout(0,7));
        JPanel tags=new FeatComponents.WrapPanel(4);
        tags.add(FeatComponents.tag(feat.category().toString().toUpperCase(Locale.ROOT),feat.category()==FeatCatalog.Category.ORIGIN));
        tags.add(status);
        if(!feat.abilityIncrease().isEmpty())tags.add(FeatComponents.colorTag("ABILITY INCREASE","badgeBlue","badgeLightText"));
        content.add(tags,BorderLayout.NORTH);
        JTextArea requirements=FeatComponents.text("LEVEL "+feat.minimumLevel()+"+  //  "+(feat.prerequisite().isEmpty()?"NO OTHER PREREQUISITES":feat.prerequisite().toUpperCase(Locale.ROOT)),12);
        requirements.setFont(new Font("Consolas",Font.BOLD,12));AppStyle.setColorRole(requirements,"accent");
        JPanel strip=AppStyle.surfacePanel(new BorderLayout(),"low");strip.setBorder(new EmptyBorder(4,6,4,6));strip.add(requirements);
        content.add(strip,BorderLayout.CENTER);card.add(content,BorderLayout.CENTER);
        bindInspection(this,inspect);
    }

    private static void bindInspection(Component component,Runnable inspect) {
        if(!(component instanceof AbstractButton)) {
            component.addMouseListener(new MouseAdapter(){@Override public void mouseClicked(MouseEvent event){if(SwingUtilities.isLeftMouseButton(event))inspect.run();}});
            component.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        if(component instanceof Container container)for(Component child:container.getComponents())bindInspection(child,inspect);
    }

    void refresh(CharacterState state) {
        boolean selected=state.equippedFeats().containsKey(feat.name());
        card.putClientProperty("surfaceRole","surface");
        String required=feat.requirements(state.level(),state.charisma());
        status.setText(selected?"EQUIPPED":!required.isEmpty()?"LOCKED":feat.needsConfirmation(state.charisma())?"CONFIRM PREREQUISITES":"AVAILABLE");
        status.setToolTipText(FeatListModel.status(feat,state));
        status.putClientProperty("surfaceRole",selected?"badgeDark":!required.isEmpty()?"badgeRed":"high");
        AppStyle.setColorRole(status,selected||!required.isEmpty()?"badgeLightText":"ink");
        boolean fromBackground=BackgroundCatalog.selected(state.play().sheet()).map(background->background.feat().equals(feat.name())).orElse(false)&&!state.play().feats().containsKey(feat.name());
        choose.setText(selected?(fromBackground?"View":"Remove"):!required.isEmpty()?"Inspect":feat.needsConfirmation(state.charisma())?"Review":"Equip");
        choose.putClientProperty("tactileRole",selected?null:"yellow");
    }
}
