package com.timecapsule.ui.widgets;

import com.timecapsule.core.Memory;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * The time-capsule seal: deep red while the date holds, cracked gold once
 * it lifts — with a plain countdown so keepers know how long to wait.
 */
public class WaxSeal extends VBox {

    public WaxSeal(Memory memory, Instant now) {
        super(8);
        setAlignment(Pos.CENTER);

        boolean sealed = memory.isLocked() && !memory.isUnlockedAt(now);
        boolean opened = memory.isLocked() && memory.isUnlockedAt(now);

        Circle wax = new Circle(46);
        wax.getStyleClass().add(opened ? "wax-opened" : "wax-sealed");

        String title = memory.getTitle() == null ? "" : memory.getTitle().strip();
        Label initial = new Label(title.isEmpty() ? "✦" : title.substring(0, 1).toUpperCase());
        initial.getStyleClass().add("wax-initial");
        initial.setMouseTransparent(true);

        StackPane stack = new StackPane(wax, initial);

        Label caption = new Label(
                sealed ? countdown(memory.getUnlockDate(), now) : opened ? "opened — read freely" : "open");
        caption.getStyleClass().add("wax-count");

        getChildren().addAll(stack, caption);
    }

    /** Keeper-friendly countdown ("opens in 12 days", "opens tomorrow"…). */
    public static String countdown(Instant date, Instant now) {
        if (date == null) {
            return "open";
        }
        LocalDate today = LocalDate.now();
        LocalDate day = date.atZone(ZoneId.systemDefault()).toLocalDate();
        long days = ChronoUnit.DAYS.between(today, day);
        if (days <= 0) {
            long hours = ChronoUnit.HOURS.between(now, date);
            if (hours <= 1) {
                return "opens within the hour";
            }
            return "opens in " + hours + " hours";
        }
        if (days == 1) {
            return "opens tomorrow";
        }
        return "opens in " + days + " days";
    }
}
