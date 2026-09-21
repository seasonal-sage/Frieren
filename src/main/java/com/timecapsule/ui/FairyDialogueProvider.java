package com.timecapsule.ui;

/**
 * Dialogue source for Frieren (Module: UI). Static scripted lines today;
 * a future Ollama-backed implementation can take over without touching
 * FairyGuide or any page — the AI-fairy seam.
 */
public interface FairyDialogueProvider {

    /** Returns Frieren's line for a page id such as "cover" or "contents". */
    String lineFor(String pageId);
}
