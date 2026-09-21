package com.timecapsule.ui.widgets;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Frieren, guardian of the grimoire: a summonable companion. She rests as a
 * tiny ✦ tab (blocking nothing) and appears — sprite plus tailed speech
 * bubble with typewriter speech — when summoned. Runs bubble-only while her
 * portrait file is absent (see FrierenImage).
 */
public class FairyGuide extends StackPane {

    private static final double TYPE_MS_PER_CHAR = 18;

    private final Label text = new Label();
    private final VBox card = new VBox(0);
    private final VBox assembly = new VBox(0);
    private final Button recall = new Button("✦");
    private String lastLine = "";
    private Timeline typer;

    public FairyGuide() {
        setPickOnBounds(false);
        // Hug content: stops the desk StackPane stretching the bubble full-width.
        setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);

        Label name = new Label("✦ Frieren");
        name.getStyleClass().add("fairy-name");
        VBox.setMargin(name, new Insets(0, 0, 4, 56));

        text.getStyleClass().add("fairy-text");
        text.setWrapText(true);
        text.setMaxWidth(200);
        text.setMinHeight(64);

        Button close = new Button("×");
        close.getStyleClass().add("fairy-x");
        close.setOnAction(e -> dismiss());

        StackPane bubble = new StackPane(text, close);
        bubble.getStyleClass().add("fairy-bubble");
        bubble.setAlignment(Pos.TOP_LEFT);
        StackPane.setAlignment(close, Pos.TOP_RIGHT);
        StackPane.setMargin(text, new Insets(26, 30, 12, 14));

        Region tail = new Region();
        tail.setPrefSize(18, 18);
        tail.setMaxSize(18, 18);
        tail.getStyleClass().add("fairy-tail");
        tail.setRotate(45);
        tail.setTranslateY(-10);
        HBox tailRow = new HBox(tail);
        tailRow.setPadding(new Insets(0, 0, 0, 78));

        card.getChildren().addAll(name, bubble, tailRow);
        card.setAlignment(Pos.BOTTOM_LEFT);
        card.setPickOnBounds(false);
        card.setMaxWidth(300);

        assembly.setAlignment(Pos.BOTTOM_LEFT);
        assembly.setPickOnBounds(false);
        assembly.setMaxWidth(Region.USE_PREF_SIZE);
        assembly.setVisible(false); // Rests minimized: only the ✦ tab shows.
        Image portrait = FrierenImage.load();
        if (portrait != null) {
            ImageView sprite = new ImageView(portrait);
            sprite.setFitHeight(170);
            sprite.setPreserveRatio(true);
            sprite.setSmooth(true);
            VBox.setMargin(sprite, new Insets(0, 0, 0, 30));
            assembly.getChildren().addAll(card, sprite);
        } else {
            assembly.getChildren().add(card);
        }

        recall.getStyleClass().add("fairy-star");
        recall.setVisible(true);
        recall.setTooltip(new Tooltip("Summon Frieren"));
        recall.setOnAction(e -> summon());

        getChildren().addAll(assembly, recall);
        StackPane.setAlignment(assembly, Pos.BOTTOM_LEFT);
        StackPane.setAlignment(recall, Pos.BOTTOM_LEFT);
        StackPane.setMargin(recall, new Insets(0, 0, 12, 12));
    }

    /**
     * Stores a line and speaks it if summoned; silent while minimized so
     * page turns always leave fresh context for the next summon.
     */
    public void say(String line) {
        lastLine = line == null ? "" : line;
        if (assembly.isVisible()) {
            playTypewriter(lastLine);
        }
    }

    /** Appears with the current line (typewriter from the top). */
    public void summon() {
        assembly.setVisible(true);
        recall.setVisible(false);
        playTypewriter(lastLine.isEmpty() ? "…" : lastLine);
    }

    /** Stores a line and appears with it (seal openings, login answers). */
    public void announce(String line) {
        lastLine = line == null ? "" : line;
        summon();
    }

    private void playTypewriter(String spoken) {
        if (typer != null) {
            typer.stop();
        }
        text.setText("");
        DoubleProperty progress = new SimpleDoubleProperty(0);
        progress.addListener((o, a, b) ->
                text.setText(spoken.substring(0, Math.min(spoken.length(), b.intValue()))));
        typer = new Timeline(new KeyFrame(
                Duration.millis(Math.max(1, spoken.length() * TYPE_MS_PER_CHAR)),
                new KeyValue(progress, spoken.length())));
        typer.play();
    }

    private void dismiss() {
        assembly.setVisible(false);
        recall.setVisible(true);
    }
}
