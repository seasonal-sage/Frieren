package com.timecapsule.ui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * Page-turn router (Module: UI). Every screen is a "spread" shown inside
 * the same open book; navigation animates as a slide + fade leaf turn.
 * Hides all transition details from controllers (abstraction).
 */
public class BookNavigator extends StackPane {

    public enum Direction {
        FORWARD, BACKWARD
    }

    private static final Duration TURN_TIME = Duration.millis(220);
    private static final double SLIDE_PX = 48;

    private Node current;
    private boolean turning;

    /** Shows a spread instantly (used for the first page). */
    public void show(Node spread) {
        if (turning) {
            return;
        }
        if (current != null) {
            getChildren().remove(current);
        }
        current = spread;
        getChildren().add(spread);
    }

    /** Turns to a spread with the slide + fade animation. */
    public void show(Node spread, Direction dir) {
        if (turning || spread == current) {
            return;
        }
        if (current == null) {
            show(spread);
            return;
        }
        turning = true;
        double outX = dir == Direction.FORWARD ? -SLIDE_PX : SLIDE_PX;

        spread.setOpacity(0);
        spread.setTranslateX(-outX);
        getChildren().add(spread);

        FadeTransition fadeOut = new FadeTransition(TURN_TIME, current);
        fadeOut.setToValue(0);
        TranslateTransition slideOut = new TranslateTransition(TURN_TIME, current);
        slideOut.setToX(outX);

        FadeTransition fadeIn = new FadeTransition(TURN_TIME, spread);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        TranslateTransition slideIn = new TranslateTransition(TURN_TIME, spread);
        slideIn.setToX(0);

        ParallelTransition out = new ParallelTransition(fadeOut, slideOut);
        ParallelTransition in = new ParallelTransition(fadeIn, slideIn);
        out.setOnFinished(e -> {
            getChildren().remove(current);
            current = spread;
            in.setOnFinished(ev -> {
                spread.setTranslateX(0);
                turning = false;
            });
            in.play();
        });
        out.play();
    }

    public boolean isTurning() {
        return turning;
    }

    /** The spread currently on display (for silent refresh checks). */
    public Node getCurrent() {
        return current;
    }
}
