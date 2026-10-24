package com.timecapsule.audio;

import javafx.scene.media.AudioClip;

import java.util.HashMap;
import java.util.Map;

/**
 * Grimoire sound effects (Module: UI polish). Loads short {@link AudioClip}s
 * from {@code /com/timecapsule/sounds/} and plays them on UI events. Every
 * load is wrapped in try/catch so a missing sound file never breaks the
 * book — drop real .wav files in later and they just work.
 * <p>
 * Sound is on by default; call {@link #setEnabled(false)} to silence.
 */
public final class SoundService {

    private static final String SOUND_ROOT = "/com/timecapsule/sounds/";

    private static final SoundService INSTANCE = new SoundService();

    private final Map<String, AudioClip> clips = new HashMap<>();
    private boolean enabled = true;

    private SoundService() {
    }

    /** The one sound manager for the whole grimoire. */
    public static SoundService instance() {
        return INSTANCE;
    }

    /** Preloads every sound we know about (silently skips absent files). */
    public void loadAll() {
        load("click", "click.wav");
        load("page-turn", "page-turn.wav");
        load("seal-crack", "seal-crack.wav");
        load("frieren-greet", "frieren-greet.wav");
        load("frieren-speak", "frieren-speak.wav");
        load("entry-saved", "entry-saved.wav");
    }

    private void load(String key, String fileName) {
        try {
            var url = SoundService.class.getResource(SOUND_ROOT + fileName);
            if (url != null) {
                clips.put(key, new AudioClip(url.toExternalForm()));
            }
        } catch (Exception ignored) {
            // Missing or unreadable sound — silence is acceptable.
        }
    }

    /** Whether sounds are currently audible. */
    public boolean isEnabled() {
        return enabled;
    }

    /** Turns all grimoire sounds on or off. */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** Soft button tap. */
    public void playClick() {
        play("click");
    }

    /** Page-turn slide + fade. */
    public void playPageTurn() {
        play("page-turn");
    }

    /** Wax seal cracking open. */
    public void playSealCrack() {
        play("seal-crack");
    }

    /** Frieren first appears / greets. */
    public void playFrierenGreet() {
        play("frieren-greet");
    }

    /** Frieren speaks a line in chat. */
    public void playFrierenSpeak() {
        play("frieren-speak");
    }

    /** Entry created or updated. */
    public void playEntrySaved() {
        play("entry-saved");
    }

    private void play(String key) {
        if (!enabled) {
            return;
        }
        AudioClip clip = clips.get(key);
        if (clip != null) {
            try {
                clip.play();
            } catch (Exception ignored) {
                // Playback failure is not fatal.
            }
        }
    }
}
