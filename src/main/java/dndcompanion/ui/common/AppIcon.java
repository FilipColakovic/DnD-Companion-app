package dndcompanion.ui.common;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Icon;

/** Supplied Stitch artwork, bundled independently of the launch directory. */
public final class AppIcon {
    private AppIcon() {}

    private static final List<Image> IMAGES = load();

    public static void install(Window window) {
        if (!IMAGES.isEmpty() && !window.getIconImages().equals(IMAGES))
            window.setIconImages(IMAGES);
    }

    public static Icon caption() {
        return new Icon() {
            public int getIconWidth() {
                return 40;
            }

            public int getIconHeight() {
                return 40;
            }

            public void paintIcon(Component component, Graphics graphics, int x, int y) {
                if (IMAGES.isEmpty()) return;
                Graphics2D g = (Graphics2D) graphics.create();
                g.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g.setRenderingHint(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.drawImage(IMAGES.getLast(), x, y, 40, 40, component);
                g.dispose();
            }
        };
    }

    private static List<Image> load() {
        try (var resource = AppIcon.class.getResourceAsStream("/icons/app-icon.png")) {
            if (resource == null) return List.of();
            BufferedImage source = ImageIO.read(resource);
            if (source == null) return List.of();
            List<Image> sizes = new ArrayList<>();
            for (int size : new int[] {16, 24, 32, 48, 64, 128, 256}) {
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setRenderingHint(
                        RenderingHints.KEY_INTERPOLATION,
                        RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                graphics.setRenderingHint(
                        RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                graphics.drawImage(source, 0, 0, size, size, null);
                graphics.dispose();
                sizes.add(image);
            }
            sizes.add(source);
            return List.copyOf(sizes);
        } catch (IOException error) {
            return List.of();
        }
    }
}
