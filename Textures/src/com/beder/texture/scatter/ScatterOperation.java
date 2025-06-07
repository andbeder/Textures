package com.beder.texture.scatter;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.Random;

import com.beder.texture.ImagePair;
import com.beder.texture.Operation;
import com.beder.texture.Parameters;
import com.beder.texture.Redrawable;

public class ScatterOperation extends Operation {

    public ScatterOperation(Redrawable redraw) {
        super(redraw);
        addParameter("Quantity", CONTROL_TYPE.INT, 10);
        addParameter("Size", CONTROL_TYPE.INT, 64);
        addParameter("StdDev", CONTROL_TYPE.DOUBLE, 10.0);
        addParameter("Seed", CONTROL_TYPE.SEED, new Random().nextLong());
    }

    @Override
    public ImagePair executeOperation(ImagePair input, Parameters par) {
        // 1. Read parameters
        int quantity  = (int) par.get("Quantity", 10);
        int meanSize  = (int) par.get("Size",     64);
        double stdDev =      par.get("StdDev",   10.0);
        long seed     =   (long) par.get("Seed",    System.currentTimeMillis());
        Random rnd    = new Random(seed);

        // 2. Fetch sprites
        SpriteRepository repo = SpriteRepository.getInstance();
        if (repo.getCount() == 0) {
            // No sprites configured → no-op
            return input;
        }

        int res = getRedraw().getRes();
        BufferedImage canvas = new BufferedImage(res, res, BufferedImage.TYPE_INT_ARGB);
        input.left = canvas;

        for (int i = 0; i < quantity; i++) {
            // 3. Weighted random sprite selection
            int idx = repo.getRandomIndex(rnd);
            BufferedImage sprite = repo.getSprite(idx);

            // 4. Sample size (Gaussian) and rotation angle
            int size = Math.max(1, (int)(rnd.nextGaussian() * stdDev + meanSize));
            double angle = rnd.nextDouble() * Math.PI * 2;

            // 5. Build AffineTransform: scale → rotate around center
            double scaleX = (double) size / sprite.getWidth();
            double scaleY = (double) size / sprite.getHeight();
            AffineTransform tx = new AffineTransform();
            tx.translate(size / 2.0, size / 2.0);
            tx.rotate(angle);
            tx.scale(scaleX, scaleY);
            tx.translate(-sprite.getWidth() / 2.0, -sprite.getHeight() / 2.0);

            // 6. Render transformed sprite into a temp image
            BufferedImage transformed = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = transformed.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.drawImage(sprite, tx, null);
            g2.dispose();

            // 7. Choose a random placement
            int x0 = rnd.nextInt(res);
            int y0 = rnd.nextInt(res);

            // 8. Paste with toroidal wrap: pixel‐by‐pixel
            for (int y = 0; y < transformed.getHeight(); y++) {
                for (int x = 0; x < transformed.getWidth(); x++) {
                    int argb = transformed.getRGB(x, y);
                    int alpha = (argb >>> 24) & 0xFF;
                    if (alpha == 0) continue; // skip fully transparent
                    int dx = (x0 + x) % res;
                    if (dx < 0) dx += res;
                    int dy = (y0 + y) % res;
                    if (dy < 0) dy += res;
                    canvas.setRGB(dx, dy, argb);
                }
            }
        }

        return input;
    }

    @Override
    public String getTitle() {
        return "Scatter";
    }

    @Override
    public String getDescription() {
        return "Scatter: randomly distributes sprites across the image buffer";
    }
}