package com.timecapsule.ui.widgets;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.nio.file.Path;

/**
 * A photo in a white polaroid frame with washi-tape corners. Missing or
 * unreadable files simply show an empty frame (callers note the loss).
 */
public class TapedPhoto extends StackPane {

    public TapedPhoto(Path file) {
        ImageView view = new ImageView();
        try {
            Image image = new Image(file.toUri().toString(), 220, 0, true, true, false);
            if (!image.isError()) {
                view.setImage(image);
            }
        } catch (Exception ignored) {
            // Empty frame below.
        }
        view.setFitWidth(220);
        view.setPreserveRatio(true);

        VBox frame = new VBox(view);
        frame.getStyleClass().add("polaroid");
        frame.setAlignment(Pos.CENTER);

        Region left = tape(-28);
        Region right = tape(28);
        StackPane.setAlignment(left, Pos.TOP_LEFT);
        StackPane.setAlignment(right, Pos.TOP_RIGHT);
        StackPane.setMargin(left, new javafx.geometry.Insets(2, 0, 0, 14));
        StackPane.setMargin(right, new javafx.geometry.Insets(2, 14, 0, 0));

        getChildren().addAll(frame, left, right);
        setCursor(Cursor.HAND);
        Tooltip.install(this, new Tooltip("Click to behold"));
    }

    private static Region tape(double angle) {
        Region tape = new Region();
        tape.getStyleClass().add("tape");
        tape.setRotate(angle);
        return tape;
    }
}
