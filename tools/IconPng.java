import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;

/** Store icon: teal field and the four Kwadra tiles. */
public final class IconPng {
    public static void main(String[] args) throws Exception {
        int size = 512;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(0x005740));
        g.fillRect(0, 0, size, size);
        float pad = size * 0.28f;
        float gap = size * 0.045f;
        float tile = (size - pad * 2f - gap) / 2f;
        float radius = tile * 0.22f;
        int[] colors = {0x6FB59D, 0x63B4BA, 0x9AAD76, 0x7CA9D5};
        for (int i = 0; i < 4; i++) {
            float x = pad + (i % 2) * (tile + gap);
            float y = pad + (i / 2) * (tile + gap);
            g.setColor(new Color(colors[i]));
            g.fill(new RoundRectangle2D.Float(x, y, tile, tile, radius, radius));
        }
        g.dispose();
        ImageIO.write(image, "png", Path.of(args[0]).toFile());
    }
}
