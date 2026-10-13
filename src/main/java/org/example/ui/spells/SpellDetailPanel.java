package org.example.ui.spells;

import org.example.domain.spells.SpellCatalog;
import org.example.domain.spells.SpellDetails;
import org.example.domain.spells.SpellMechanics;
import org.example.ui.common.AppStyle;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Locale;

/** Standalone, complete spell entry with specifications and locally bundled rules text. */
public final class SpellDetailPanel extends JPanel {
    public SpellDetailPanel(SpellCatalog.Spell spell) {
        super(new BorderLayout());
        putClientProperty("windowShell",false);
        putClientProperty("surfaceRole","background");
        SpellDetails details=SpellDetails.forSpell(spell);
        Column page=new Column();page.setBorder(new EmptyBorder(20,20,20,20));
        JPanel heading=AppStyle.section(new BorderLayout(12,10));
        heading.add(AppStyle.mono((spell.levelName()+" · "+details.school()+" SCHOOL // RULES 2024").toUpperCase(Locale.ROOT),12),BorderLayout.NORTH);
        JTextArea name=wrappingText(spell.name().toUpperCase(Locale.ROOT),38,true);name.setName("spell-detail-title");heading.add(name,BorderLayout.CENTER);
        JPanel badges=AppStyle.section(new FlowLayout(FlowLayout.LEFT,8,0));
        badges.add(badge(spell.levelName(),"slotAvailable"));badges.add(badge(details.school(),"badgeBlue"));
        if(details.concentration())badges.add(badge("CONCENTRATION","badgeRed"));if(details.ritual())badges.add(badge("RITUAL","badgeDark"));heading.add(badges,BorderLayout.SOUTH);
        heading.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,2,0,AppStyle.LINE),new EmptyBorder(0,0,16,0)));row(page,heading,0);

        JPanel specifications=card("SPELL SPECIFICATIONS","badgeBlue");
        JPanel stats=new JPanel(new GridLayout(0,4,12,16)) {
            @Override public void doLayout(){((GridLayout)getLayout()).setColumns(getWidth()<700?2:4);super.doLayout();}
            @Override public Dimension getPreferredSize(){((GridLayout)getLayout()).setColumns(getWidth()>0&&getWidth()<700?2:4);return super.getPreferredSize();}
        };
        stats.setOpaque(false);
        stats.add(stat("CASTING TIME",details.castingTime(),"ink"));stats.add(stat("RANGE",details.range(),"ink"));
        stats.add(stat("COMPONENTS",details.components(),"ink"));stats.add(stat("DURATION",details.duration(),details.concentration()?"error":"ink"));
        stats.add(stat("CONCENTRATION",details.concentration()?"Yes — required":"No",details.concentration()?"error":"ink"));stats.add(stat("RITUAL",details.ritual()?"Yes":"No","ink"));
        stats.add(stat("SCHOOL",details.school(),"accent"));
        String rolls=SpellMechanics.hasAttack(spell)?SpellMechanics.forcesSave(spell)?"Attack / saving throw":"Spell attack":SpellMechanics.forcesSave(spell)?"Saving throw":"See spell effects";
        stats.add(stat("ATTACK / SAVE",rolls,"accent"));specifications.add(stats,BorderLayout.CENTER);row(page,specifications,0);

        JPanel rules=card("SPELL RULES // 2024","badgeRed");
        String description=formatted(details.description());
        JEditorPane text=editor(description);text.setName("spell-detail-rules");
        JPanel entry=AppStyle.surfacePanel(new BorderLayout(),"low");entry.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,4,0,0,AppStyle.LINE),new EmptyBorder(14,16,14,16)));entry.add(text);rules.add(entry,BorderLayout.CENTER);row(page,rules,1);
        if(!details.higherLevels().isBlank()){
            JPanel scaling=card(spell.level()==0?"CANTRIP UPGRADE":"HIGHER-LEVEL SCALING","badgeRed");
            JEditorPane higher=editor(formatted(details.higherLevels()));higher.setName("spell-detail-scaling");scaling.add(higher,BorderLayout.CENTER);row(page,scaling,0);
        }
        JLabel source=AppStyle.mono(details.source().replace("Â·","·"),11);AppStyle.setColorRole(source,"muted");source.setToolTipText("Source attribution; see SPELL_DATA_LICENSE.md");row(page,source,0);
        JScrollPane scroll=new JScrollPane(page);scroll.setName("spell-detail-scroll");AppStyle.scrollPane(scroll);scroll.getViewport().putClientProperty("surfaceRole","background");add(scroll);
    }
    private static JPanel card(String title,String color){
        JPanel card=AppStyle.card(new BorderLayout(0,16));card.putClientProperty("surfaceRole","lowest");card.setBorder(new EmptyBorder(20,20,24,24));
        JPanel titleRow=AppStyle.section(new BorderLayout(10,0));JLabel marker=badge(" ",color);titleRow.add(marker,BorderLayout.WEST);titleRow.add(AppStyle.mono(title,13),BorderLayout.CENTER);
        titleRow.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(0,0,1,0,AppStyle.LINE),new EmptyBorder(0,0,12,0)));card.add(titleRow,BorderLayout.NORTH);return card;
    }
    private static JLabel badge(String value,String role){JLabel label=AppStyle.mono(value.toUpperCase(Locale.ROOT),13);label.setOpaque(true);label.putClientProperty("surfaceRole",role);AppStyle.setColorRole(label,role.equals("slotAvailable")?"badgeText":"badgeLightText");label.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(AppStyle.LINE,2),new EmptyBorder(5,10,5,10)));return label;}
    private static JPanel stat(String title,String value,String color){JPanel panel=AppStyle.section(new BorderLayout(0,8));JLabel caption=AppStyle.mono(title,12);AppStyle.setColorRole(caption,"muted");panel.add(caption,BorderLayout.NORTH);JTextArea text=wrappingText(value,16,true);AppStyle.setColorRole(text,color);panel.add(text,BorderLayout.CENTER);return panel;}
    private static JTextArea wrappingText(String value,int size,boolean bold){
        JTextArea text=new JTextArea(value){@Override public Dimension getPreferredSize(){int width=getWidth()>0?getWidth():250;getUI().getRootView(this).setSize(Math.max(1,width),Float.MAX_VALUE);return new Dimension(width,(int)Math.ceil(getUI().getRootView(this).getPreferredSpan(javax.swing.text.View.Y_AXIS)));}};
        text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setOpaque(false);text.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size));return text;
    }
    private static JEditorPane editor(String html){JEditorPane text=new JEditorPane("text/html","<html><body>"+html+"</body></html>"){
        @Override public Dimension getPreferredSize(){int width=getWidth()>0?getWidth():650;Insets i=getInsets();getUI().getRootView(this).setSize(Math.max(1,width-i.left-i.right),Float.MAX_VALUE);return new Dimension(width,(int)Math.ceil(getUI().getRootView(this).getPreferredSpan(javax.swing.text.View.Y_AXIS))+i.top+i.bottom);}
    };text.setEditable(false);text.setOpaque(false);text.setFont(new Font("Segoe UI",Font.PLAIN,17));text.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES,true);text.setBorder(null);text.setCaretPosition(0);return text;}
    private static void row(JPanel panel,JComponent child,double weight){GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=panel.getComponentCount();c.weightx=1;c.weighty=weight;c.fill=GridBagConstraints.BOTH;c.anchor=GridBagConstraints.NORTH;c.insets=new Insets(0,0,18,0);panel.add(child,c);}
    private static final class Column extends JPanel implements Scrollable{
        Column(){super(new GridBagLayout());putClientProperty("surfaceRole","background");}
        public Dimension getPreferredScrollableViewportSize(){return new Dimension(950,800);}public boolean getScrollableTracksViewportWidth(){return true;}public boolean getScrollableTracksViewportHeight(){return getParent()!=null&&getParent().getHeight()>=getPreferredSize().height;}public int getScrollableUnitIncrement(Rectangle r,int o,int d){return 28;}public int getScrollableBlockIncrement(Rectangle r,int o,int d){return Math.max(28,r.height-28);}
    }
    private static String escape(String value){return value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;");}
    private static String formatted(String value){String html=value.contains("<table")?value:escape(value);html=html.replaceAll("(?m)^###? (.*)$","<h3>$1</h3>").replaceAll("\\*\\*(.*?)\\*\\*","<b>$1</b>").replaceAll("\\*(.*?)\\*","<i>$1</i>").replaceAll("_(.*?)_","<i>$1</i>");return "<p>"+html.replace("\n\n","</p><p>").replace("\n","<br>")+"</p>";}
}
