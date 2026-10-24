package com.timecapsule.ai;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves Frieren naps gracefully when the stars are out. */
class OllamaFairyProviderTest {

    private OllamaFairyProvider sleeping() {
        // Nothing listens on port 1 — connection refused, instantly.
        return new OllamaFairyProvider(new OllamaClient("nope", URI.create("http://localhost:1/api/chat")));
    }

    @Test
    void fallbackSpeaksWhenServerDown() {
        String line = sleeping().lineFor("cover");
        assertTrue(line.contains("grimoire"), "Static script must answer: " + line);
    }

    @Test
    void awakeIsFalseWhenServerDown() {
        assertFalse(sleeping().awake());
    }
}
