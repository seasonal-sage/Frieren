package com.timecapsule.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves the rolling memory keeps only the latest exchanges. */
class ChatHistoryTest {

    @Test
    void keepsLatestUpToCapacity() {
        ChatHistory history = new ChatHistory(4);
        for (int i = 1; i <= 6; i++) {
            history.add(i % 2 == 0 ? "assistant" : "user", "line " + i);
        }
        assertEquals(4, history.size());
        assertEquals("line 3", history.messages().get(0).content());
        assertEquals("line 6", history.messages().get(3).content());
    }

    @Test
    void clearEmpties() {
        ChatHistory history = new ChatHistory(8);
        history.add("user", "hello");
        history.clear();
        assertEquals(0, history.size());
        assertTrue(history.messages().isEmpty());
    }
}
