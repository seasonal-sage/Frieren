package com.timecapsule.ai;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Short rolling memory for Frieren's chats (Module: AI companion). Keeps
 * only the latest exchanges — long-term memory is a future enchantment.
 */
public class ChatHistory {

    /** One turn of conversation. */
    public record ChatMessage(String role, String content) {
    }

    private final int capacity;
    private final Deque<ChatMessage> messages = new ArrayDeque<>();

    public ChatHistory(int capacity) {
        this.capacity = Math.max(2, capacity);
    }

    public synchronized void add(String role, String content) {
        messages.addLast(new ChatMessage(role, content));
        while (messages.size() > capacity) {
            messages.removeFirst();
        }
    }

    public synchronized List<ChatMessage> messages() {
        return List.copyOf(messages);
    }

    public synchronized int size() {
        return messages.size();
    }

    public synchronized void clear() {
        messages.clear();
    }
}
