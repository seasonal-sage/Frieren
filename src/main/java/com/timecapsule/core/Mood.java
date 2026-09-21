package com.timecapsule.core;

/** Daily sentiment stamps for entries (Module: Analytics feed). */
public enum Mood {

    HAPPY("Happy"),
    CALM("Calm"),
    SAD("Sad"),
    ANXIOUS("Anxious"),
    NEUTRAL("Neutral");

    private final String label;

    Mood(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /** Lenient parse for database values; unknown input becomes NEUTRAL. */
    public static Mood parse(String value) {
        if (value == null) {
            return NEUTRAL;
        }
        try {
            return Mood.valueOf(value.strip().toUpperCase());
        } catch (IllegalArgumentException e) {
            return NEUTRAL;
        }
    }
}
