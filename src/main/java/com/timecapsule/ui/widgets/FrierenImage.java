package com.timecapsule.ui.widgets;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.InputStream;

/**
 * Loads Frieren's portrait and keys out the near-black backdrop at runtime,
 * so the source file can be dropped in as-is. Dark costume details (boots,
 * staff) survive via a feathered threshold. Returns {@code null} when the
 * file is absent — FairyGuide then runs bubble-only.
 */
public final class FrierenImage {

    private static final String PATH = "/com/timecapsule/img/frieren.png";

    private FrierenImage() {
    }

    public static Image load() {
        try (InputStream in = FrierenImage.class.getResourceAsStream(PATH)) {
            if (in == null) {
                System.out.println("[Frieren] frieren.png not found under resources/img — bubble-only mode. "
                        + "Drop the file in as src/main/resources/com/timecapsule/img/frieren.png and rerun.");
                return null;
            }
            Image src = new Image(in);
            int w = (int) src.getWidth();
            int h = (int) src.getHeight();
            WritableImage out = new WritableImage(w, h);
            PixelReader reader = src.getPixelReader();
            PixelWriter writer = out.getPixelWriter();
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int argb = reader.getArgb(x, y);
                    int r = (argb >> 16) & 0xFF;
                    int g = (argb >> 8) & 0xFF;
                    int b = argb & 0xFF;
                    double lum = (r + g + b) / 3.0;
                    int a;
                    if (lum < 24) {
                        a = 0;
                    } else if (lum < 60) {
                        a = (int) (255 * (lum - 24) / 36);
                    } else {
                        a = 255;
                    }
                    writer.setArgb(x, y, (a << 24) | (r << 16) | (g << 8) | b);
                }
            }
            return out;
        } catch (Exception e) {
            System.out.println("[Frieren] could not load portrait: " + e.getMessage());
            return null;
        }
    }
}
