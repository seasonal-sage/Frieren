package com.timecapsule.ui.widgets;

import com.timecapsule.core.Mood;
import javafx.scene.control.Label;

/** A tilted ink stamp showing an entry's mood. */
public class MoodStamp extends Label {

    public MoodStamp(Mood mood) {
        super(mood == null ? "· unmarked ·" : "· " + mood.getLabel().toLowerCase() + " ·");
        getStyleClass().add("mood-stamp");
        setRotate(-6);
    }
}
