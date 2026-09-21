package com.timecapsule.ui.widgets;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

/**
 * A cluster of two lit candles (tall + short) on a shared saucer, drawn
 * with pure shapes + CSS. Each flame breathes on its own slow loop so the
 * lamplight feels alive without distracting from reading.
 */
public class CandleCluster extends StackPane {

    public CandleCluster() {
        setPrefSize(150, 240);

        Ellipse saucer = new Ellipse(74, 15);
        saucer.getStyleClass().add("candle-saucer");
        StackPane.setAlignment(saucer, Pos.BOTTOM_CENTER);

        HBox candles = new HBox(20, makeCandle(150, 1500), makeCandle(108, 1900));
        candles.setAlignment(Pos.BOTTOM_CENTER);
        StackPane.setMargin(candles, new Insets(0, 0, 8, 0));

        getChildren().addAll(saucer, candles);
    }

    private VBox makeCandle(double waxHeight, long flickerMs) {
        Circle glow = new Circle(32);
        glow.getStyleClass().add("flame-glow");

        Ellipse outer = new Ellipse(9, 18);
        outer.getStyleClass().add("flame-outer");
        outer.setTranslateY(2);

        Ellipse core = new Ellipse(4, 8.5);
        core.getStyleClass().add("flame-core");
        core.setTranslateY(-1);

        StackPane flame = new StackPane(glow, outer, core);
        flame.setPrefSize(64, 64);
        flame.setTranslateY(8);

        Rectangle wick = new Rectangle(3, 9);
        wick.getStyleClass().add("candle-wick");

        Rectangle wax = new Rectangle(36, waxHeight);
        wax.setArcWidth(14);
        wax.setArcHeight(18);
        wax.getStyleClass().add("candle-wax");

        VBox candle = new VBox(0, flame, wick, wax);
        candle.setAlignment(Pos.BOTTOM_CENTER);

        flicker(flame, glow, flickerMs);
        return candle;
    }

    private void flicker(StackPane flame, Circle glow, long periodMs) {
        Timeline t = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(flame.scaleXProperty(), 1.0),
                        new KeyValue(flame.scaleYProperty(), 1.0),
                        new KeyValue(glow.opacityProperty(), 0.9)),
                new KeyFrame(Duration.millis(periodMs),
                        new KeyValue(flame.scaleXProperty(), 0.94),
                        new KeyValue(flame.scaleYProperty(), 1.05),
                        new KeyValue(glow.opacityProperty(), 1.0)));
        t.setAutoReverse(true);
        t.setCycleCount(Timeline.INDEFINITE);
        t.play();
    }
}
