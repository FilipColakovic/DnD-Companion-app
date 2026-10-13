package org.example.ui.common;

import org.example.domain.character.Metamagic;
import javax.swing.Icon;
import java.awt.*;
import java.awt.geom.*;
import java.util.StringTokenizer;

/** Scalable glyphs from the supplied icons/metamagic-icons.svg artwork, without its labels or cost tiles. */
public final class MetamagicIcon implements Icon {
    private static final Color INK=new Color(0x1a1a1a),CREAM=new Color(0xeae2d5),BLUE=new Color(0x2563eb),RED=new Color(0xdc2626),GOLD=new Color(0xf59e0b);
    private final Metamagic option;
    private final int size;
    public MetamagicIcon(Metamagic option,int size){this.option=option;this.size=size;}
    @Override public int getIconWidth(){return size;}
    @Override public int getIconHeight(){return size;}
    @Override public void paintIcon(Component component,Graphics graphics,int x,int y){
        Graphics2D g=AppStyle.graphics(graphics);g.translate(x,y);g.scale(size/80d,size/80d);g.translate(40,40);
        // A pale backing keeps the black outlines readable in either theme.
        g.setColor(CREAM);g.fillRoundRect(-39,-39,78,78,8,8);
        switch(option){
            case CAREFUL -> {
                shape(g,path("M 0 -34 L 28 -18 L 28 12 C 28 26 0 36 0 36 C 0 36 -28 26 -28 12 L -28 -18 Z"),CREAM,3);
                shape(g,path("M 0 -24 L 18 -12 L 18 10 C 18 20 0 28 0 28 C 0 28 -18 20 -18 10 L -18 -12 Z"),BLUE,2);
                circle(g,0,6,6,Color.WHITE,2.5f);
            }
            case DISTANT -> {
                dashed(g,new Ellipse2D.Double(-32,-32,64,64),INK,2,4,3);
                circle(g,0,0,22,null,BLUE,2);circle(g,0,0,12,CREAM,2);
                line(g,-24,24,26,-26,INK,4);shape(g,polygon(28,-36,34,-18,18,-24),RED,2);circle(g,-16,16,4,GOLD,2);
            }
            case EMPOWERED -> {
                shape(g,polygon(0,-32,26,-16,26,16,0,32,-26,16,-26,-16),CREAM,3);
                shape(g,polygon(0,-32,26,-16,0,0,-26,-16),new Color(0xfbbf24),2);
                shape(g,polygon(0,0,26,-16,26,16,0,32),RED,2);
                shape(g,polygon(0,0,-26,-16,-26,16,0,32),new Color(0xb45309),2);
                shape(g,polygon(0,-18,5,-8,15,-8,7,-2,10,8,0,2,-10,8,-7,-2,-15,-8,-5,-8),Color.WHITE,1.5f);
            }
            case EXTENDED -> {
                shape(g,polygon(-24,-28,24,-28,0,0),BLUE,3);shape(g,polygon(-24,28,24,28,0,0),CREAM,3);
                line(g,-28,-28,28,-28,INK,3.5f);line(g,-28,28,28,28,INK,3.5f);line(g,0,-4,0,18,GOLD,3);
                g.setColor(GOLD);g.fill(new Ellipse2D.Double(-3,19,6,6));
                g.setColor(INK);g.fillRoundRect(14,-8,18,16,2,2);g.setColor(Color.WHITE);g.setFont(new Font("Segoe UI",Font.BOLD,9));g.drawString("2×",16,4);
            }
            case HEIGHTENED -> {
                shape(g,polygon(0,-34,32,0,0,34,-32,0),CREAM,3);
                shape(g,path("M -26 0 C -14 -16 14 -16 26 0 C 14 16 -14 16 -26 0 Z"),Color.WHITE,2.5f);
                circle(g,0,0,10,RED,2);g.setColor(INK);g.fillOval(-4,-4,8,8);
                shape(g,path("M -8 -24 L 0 -16 L 8 -24"),null,3);shape(g,path("M -8 24 L 0 16 L 8 24"),null,3);
            }
            case QUICKENED -> {
                shape(g,polygon(4,-34,-20,2,-2,2,-8,34,22,-2,2,-2),new Color(0xfbbf24),3);
                line(g,-24,-20,-10,-20,RED,3);line(g,-28,-8,-18,-8,RED,3);line(g,12,16,26,16,RED,3);line(g,16,26,28,26,RED,3);
            }
            case SEEKING -> {
                circle(g,0,0,28,null,2.5f);circle(g,0,0,16,null,BLUE,2);
                line(g,-34,0,34,0,INK,2.5f);line(g,0,-34,0,34,INK,2.5f);
                dashed(g,path("M -22 24 Q 0 0 18 -18"),RED,3,3,3);circle(g,0,0,6,RED,2);
            }
            case SUBTLE -> {
                circle(g,0,0,28,CREAM,3);line(g,-24,-24,24,24,RED,4);
                shape(g,path("M -16 4 C -8 -6 8 -6 16 4"),null,3);circle(g,-8,-8,3,BLUE,1.5f);circle(g,8,-8,3,BLUE,1.5f);
                g.setColor(INK);g.fillRoundRect(-10,14,20,8,2,2);
            }
            case TRANSMUTED -> {
                shape(g,path("M 0 -28 C 8 -16 12 -6 12 2 C 12 9 6 14 0 14 C -6 14 -12 9 -12 2 C -12 -6 -8 -16 0 -28 Z"),RED,2.5f);
                shape(g,polygon(18,-4,28,12,12,18),BLUE,2);circle(g,-16,12,8,GOLD,2);
                // Endpoint arc from the supplied SVG: radius 26, clockwise across the top.
                double centerY=-6+Math.sqrt(26*26-23*23),start=Math.toDegrees(Math.atan2(centerY+6,-23));
                dashed(g,new Arc2D.Double(1-26,centerY-26,52,52,start,-(2*start-180),Arc2D.OPEN),INK,2.5f,3,3);
                shape(g,polygon(26,-12,28,0,18,-4),INK,0);
            }
            case TWINNED -> {
                circle(g,0,22,7,INK,2);g.setColor(Color.WHITE);g.fillOval(-3,19,6,6);
                line(g,0,16,-20,-16,INK,3.5f);line(g,0,16,20,-16,INK,3.5f);
                circle(g,-20,-20,10,BLUE,2.5f);circle(g,20,-20,10,BLUE,2.5f);
                g.setColor(Color.WHITE);g.fillOval(-24,-24,8,8);g.fillOval(16,-24,8,8);
                dashed(g,new Line2D.Double(-10,-20,10,-20),GOLD,2,2,2);
            }
        }
        g.dispose();
    }
    private static void shape(Graphics2D g,Shape shape,Color fill,float width){if(fill!=null){g.setColor(fill);g.fill(shape);}if(width>0){g.setColor(INK);g.setStroke(new BasicStroke(width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));g.draw(shape);}}
    private static void circle(Graphics2D g,double x,double y,double radius,Color fill,float width){circle(g,x,y,radius,fill,INK,width);}
    private static void circle(Graphics2D g,double x,double y,double radius,Color fill,Color stroke,float width){Shape shape=new Ellipse2D.Double(x-radius,y-radius,2*radius,2*radius);if(fill!=null){g.setColor(fill);g.fill(shape);}g.setColor(stroke);g.setStroke(new BasicStroke(width));g.draw(shape);}
    private static void line(Graphics2D g,double x1,double y1,double x2,double y2,Color color,float width){g.setColor(color);g.setStroke(new BasicStroke(width,BasicStroke.CAP_SQUARE,BasicStroke.JOIN_ROUND));g.draw(new Line2D.Double(x1,y1,x2,y2));}
    private static void dashed(Graphics2D g,Shape shape,Color color,float width,float on,float off){g.setColor(color);g.setStroke(new BasicStroke(width,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,10,new float[]{on,off},0));g.draw(shape);}
    private static Path2D polygon(double... points){Path2D path=new Path2D.Double();path.moveTo(points[0],points[1]);for(int i=2;i<points.length;i+=2)path.lineTo(points[i],points[i+1]);path.closePath();return path;}
    /** Only the absolute path commands used by these fixed artwork glyphs. */
    private static Path2D path(String data){Path2D path=new Path2D.Double();StringTokenizer tokens=new StringTokenizer(data);while(tokens.hasMoreTokens()){switch(tokens.nextToken()){
        case "M" -> path.moveTo(number(tokens),number(tokens));case "L" -> path.lineTo(number(tokens),number(tokens));
        case "C" -> path.curveTo(number(tokens),number(tokens),number(tokens),number(tokens),number(tokens),number(tokens));
        case "Q" -> path.quadTo(number(tokens),number(tokens),number(tokens),number(tokens));case "Z" -> path.closePath();
        default -> throw new IllegalArgumentException("Unsupported icon path command");
    }}return path;}
    private static double number(StringTokenizer tokens){return Double.parseDouble(tokens.nextToken());}
}
