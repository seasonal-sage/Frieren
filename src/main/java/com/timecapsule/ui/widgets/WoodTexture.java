package com.timecapsule.ui.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

/**
 * Generates a subtle warm plank-wood tile in code (no image assets, so it
 * always ships inside the jar). Tile it with {@code BackgroundRepeat.REPEAT}.
 */
public final class WoodTexture {

    private static final int TILE = 256;
    private static final int PLANK = 64;

    private static Image cached;

    private WoodTexture() {
    }

    public static Image plankTile() {
        if (cached == null) {
            cached = makeTile();
        }
        return cached;
    }

    private static Image makeTile() {
        WritableImage img = new WritableImage(TILE, TILE);
        PixelWriter out = img.getPixelWriter();
        // Per-plank tone + grain phase: tight range keeps it subtle.
        double[] tone = {0.94, 1.0, 0.90, 1.04};
        double[] phase = {0.0, 2.1, 4.4, 1.2};
        for (int y = 0; y < TILE; y++) {
            int plank = y / PLANK;
            int inPlank = y % PLANK;
            for (int x = 0; x < TILE; x++) {
                double grain = 0.5 + 0.5 * Math.sin(
                        x * 0.10 + phase[plank] + Math.sin((x + y * 3.0) * 0.02) * 4.0);
                double fine = 0.5 + 0.5 * Math.sin(x * 0.9 + y * 1.7 + plank * 5.0);
                double v = tone[plank] * (0.93 + grain * 0.09 + fine * 0.03);
                if (inPlank < 2) {
                    v *= 0.55; // plank seam shadow
                } else if (inPlank < 4) {
                    v *= 1.10; // bevel highlight under the seam
                }
                out.setArgb(x, y, (255 << 24) | (clamp(122 * v) << 16) | (clamp(90 * v) << 8) | clamp(60 * v));
            }
        }
        return img;
    }

    private static int clamp(double v) {
        return Math.max(0, Math.min(255, (int) Math.round(v)));
    }
}
