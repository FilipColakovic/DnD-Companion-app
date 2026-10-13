package org.example;

import org.example.persistence.CharacterStore;
import org.example.ui.GrimoirePanel;
import org.example.ui.common.AppStyle;
import org.example.ui.spells.SpellDetailPanel;
import org.example.domain.spells.SpellCatalog;
import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.*;

/** Real caption interactions preserve the main save handler and original content pane. */
public final class WindowChromeChecks {
    public static void main(String[] args) throws Exception {
        Path directory=Files.createTempDirectory(Path.of("target"),"caption-check-");
        Path database=directory.resolve("character.db");CharacterStore store=new CharacterStore(database);
        try {
            SwingUtilities.invokeAndWait(()->{
                JFrame frame=null;
                try {
                    frame=Main.createWindow(store);frame.setVisible(true);frame.validate();
                    check(frame.isUndecorated(),"Native title bar is removed");
                    check(frame.getIconImages().size()==8,"Supplied artwork is available at native window-icon sizes");
                    check(frame.getIconImages().getFirst().getWidth(null)==16,"Small taskbar/window icon is bundled");
                    check(frame.getContentPane() instanceof GrimoirePanel,"Original content-pane API is preserved");
                    GrimoirePanel workspace=(GrimoirePanel)frame.getContentPane();
                    workspace.characterPanel().setCharismaScore(19);
                    JButton maximize=find(frame.getRootPane(),"Maximize window");
                    maximize.doClick(0);check((frame.getExtendedState()&Frame.MAXIMIZED_BOTH)!=0,"Maximize uses frame state");
                    find(frame.getRootPane(),"Restore window").doClick(0);check(frame.getExtendedState()==Frame.NORMAL,"Restore leaves maximized state");
                    Rectangle initial=new Rectangle(80,80,1200,800);frame.setBounds(initial);frame.validate();
                    JPanel caption=findCaption(frame.getRootPane());
                    Point at=caption.getLocationOnScreen();
                    caption.dispatchEvent(mouse(caption,MouseEvent.MOUSE_PRESSED,100,15,at.x+100,at.y+15));
                    caption.dispatchEvent(mouse(caption,MouseEvent.MOUSE_DRAGGED,130,35,at.x+130,at.y+35));
                    caption.dispatchEvent(mouse(caption,MouseEvent.MOUSE_RELEASED,130,35,at.x+130,at.y+35));
                    check(frame.getX()==initial.x+30&&frame.getY()==initial.y+20,"Dragging the caption moves the window");
                    JRootPane root=frame.getRootPane();Point rootAt=root.getLocationOnScreen();int width=frame.getWidth();
                    root.dispatchEvent(mouse(root,MouseEvent.MOUSE_MOVED,width-3,100,rootAt.x+width-3,rootAt.y+100));
                    root.dispatchEvent(mouse(root,MouseEvent.MOUSE_PRESSED,width-3,100,rootAt.x+width-3,rootAt.y+100));
                    root.dispatchEvent(mouse(root,MouseEvent.MOUSE_DRAGGED,width+37,100,rootAt.x+width+37,rootAt.y+100));
                    root.dispatchEvent(mouse(root,MouseEvent.MOUSE_RELEASED,width+37,100,rootAt.x+width+37,rootAt.y+100));
                    check(frame.getWidth()==width+40,"Dragging a resize edge changes width");frame.validate();
                    BufferedImage image=new BufferedImage(root.getWidth(),root.getHeight(),BufferedImage.TYPE_INT_RGB);Graphics2D graphics=image.createGraphics();root.printAll(graphics);graphics.dispose();ImageIO.write(image,"png",Path.of("target/window-caption-preview.png").toFile());
                    var spell=SpellCatalog.all().stream().filter(s->s.name().equals("Shield")).findFirst().orElseThrow();
                    JDialog detail=AppStyle.openWindow(workspace,"Shield",new SpellDetailPanel(spell));
                    check(detail.isUndecorated()&&detail.getContentPane() instanceof SpellDetailPanel,"Details use custom controls without a sidebar");
                    check(detail.getIconImages().equals(frame.getIconImages()),"Detail windows use the same supplied app artwork");
                    find(detail.getRootPane(),"Close window").doClick(0);check(!detail.isDisplayable(),"Detail close button disposes its window");
                    find(root,"Minimize window").doClick(0);check((frame.getExtendedState()&Frame.ICONIFIED)!=0,"Minimize iconifies the main window");frame.setExtendedState(Frame.NORMAL);
                    var expected=workspace.snapshot();find(root,"Close window").doClick(0);
                    check(!frame.isDisplayable()&&store.load().equals(expected),"Caption close saves character through the production handler");
                }catch(Exception error){throw new RuntimeException(error);}finally{if(frame!=null)frame.dispose();}
            });
        }finally{Files.deleteIfExists(database);Files.delete(directory);}
        System.out.println("Passed: custom caption, maximize/restore, dragging, resizing, sidebar-free dialogs and save-on-close.");
    }
    private static MouseEvent mouse(Component component,int id,int x,int y,int screenX,int screenY){return new MouseEvent(component,id,System.currentTimeMillis(),id==MouseEvent.MOUSE_DRAGGED?MouseEvent.BUTTON1_DOWN_MASK:0,x,y,screenX,screenY,1,false,id==MouseEvent.MOUSE_DRAGGED||id==MouseEvent.MOUSE_MOVED?MouseEvent.NOBUTTON:MouseEvent.BUTTON1);}
    private static JButton find(Container root,String name){for(Component c:root.getComponents()){if(c instanceof JButton button&&name.equals(button.getAccessibleContext().getAccessibleName()))return button;if(c instanceof Container child){JButton found=find(child,name);if(found!=null)return found;}}return null;}
    private static JPanel findCaption(Container root){for(Component c:root.getComponents()){if(c instanceof JPanel panel&&"window-caption".equals(c.getName()))return panel;if(c instanceof Container child){JPanel found=findCaption(child);if(found!=null)return found;}}return null;}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
}
