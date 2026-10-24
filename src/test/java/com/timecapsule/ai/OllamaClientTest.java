package com.timecapsule.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live check against the real Ollama (skipped silently when the model is
 * absent — teammates without the download still build green).
 */
class OllamaClientTest {

    @Test
    void liveModelAnswers() throws Exception {
        OllamaClient client = new OllamaClient(OllamaFairyProvider.MODEL);
        assumeTrue(client.reachable(), "Ollama not reachable — skipping live check");
        assumeTrue(client.modelReady(), "qwen2.5:3b not downloaded — skipping live check");
        try {
            String reply = client.ask("Reply with exactly: awake.", List.of(), "Are you awake?");
            assertFalse(reply.isBlank());
            assertTrue(reply.length() < 600, "Replies stay short: " + reply);
        } catch (IOException | InterruptedException e) {
            assumeTrue(false, "Ollama request failed — skipping live check: " + e.getMessage());
        }
    }
}
