package com.timecapsule.search;

import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;

import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Searches the diary in memory (Module: UI support, Phase 2 polish).
 * Reads only decrypted views — sealed bodies are {@code null} by
 * construction, so they can match on title/tags but never leak words.
 * Ranked: title hits outrank tags, tags outrank body.
 */
public final class EntrySearch {

    /** One tag with its page count (alphabetical from {@link #allTags()}). */
    public record TagCount(String name, long count) {
    }

    private EntrySearch() {
    }

    /**
     * Ranked matches; blank query returns everything, newest first.
     * A {@code #tag} token matches tags only (exact, case-insensitive).
     */
    public static List<Memory> search(String query) {
        List<String> words = wordTokens(query);
        List<String> tags = tagTokens(query);
        List<Memory> entries = JournalManager.listEntries(); // throws when signed out
        if (words.isEmpty() && tags.isEmpty()) {
            return entries;
        }
        return entries.stream()
                .filter(memory -> matchesAllTags(memory, tags))
                .filter(memory -> words.isEmpty() || score(memory, words) > 0)
                .map(memory -> new Scored(memory, score(memory, words) + tagScore(memory, tags)))
                .filter(scored -> scored.score() > 0)
                .sorted(Comparator.comparingInt(Scored::score).reversed()
                        .thenComparing((Scored s) -> s.memory().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .map(Scored::memory)
                .collect(Collectors.toList());
    }

    /** Every tag in the diary with its page count, A to Z (throws when signed out). */
    public static List<TagCount> allTags() {
        Map<String, Long> counts = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        Map<String, String> display = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Memory memory : JournalManager.listEntries()) {
            for (String tag : memory.getTags()) {
                String clean = tag == null ? "" : tag.strip();
                if (clean.isEmpty()) {
                    continue;
                }
                String key = clean.toLowerCase(Locale.ROOT);
                counts.merge(key, 1L, Long::sum);
                display.putIfAbsent(key, clean);
            }
        }
        return counts.entrySet().stream()
                .map(entry -> new TagCount(display.get(entry.getKey()), entry.getValue()))
                .collect(Collectors.toList());
    }

    /** Short context excerpt for a hit (title/tags fallback when sealed). */
    public static String snippet(Memory memory, String query) {
        List<String> tokens = plainTokens(query);
        String body = memory.getBody();
        if (body != null) {
            String flat = body.replaceAll("\\s+", " ").strip();
            String lower = flat.toLowerCase();
            for (String token : tokens) {
                int at = lower.indexOf(token);
                if (at >= 0) {
                    int from = Math.max(0, at - 30);
                    int to = Math.min(flat.length(), at + token.length() + 50);
                    return (from > 0 ? "…" : "") + flat.substring(from, to).strip()
                            + (to < flat.length() ? "…" : "");
                }
            }
            if (!flat.isEmpty()) {
                return flat.length() <= 80 ? flat : flat.substring(0, 80).strip() + "…";
            }
        }
        return memory.getTags().isEmpty() ? memory.getTitle()
                : memory.getTitle() + " [" + String.join(", ", memory.getTags()) + "]";
    }

    private static int score(Memory memory, List<String> tokens) {        int points = 0;
        String title = memory.getTitle() == null ? "" : memory.getTitle().toLowerCase();
        String tags = String.join(" ", memory.getTags()).toLowerCase();
        String body = memory.getBody() == null ? "" : memory.getBody().toLowerCase();
        for (String token : tokens) {
            if (title.contains(token)) {
                points += 3;
            }
            if (tags.contains(token)) {
                points += 2;
            }
            if (!body.isEmpty() && body.contains(token)) {
                points += 1;
            }
        }
        return points;
    }

    /** Exact tag hits outrank everything (a keeper asked for this tag). */
    private static int tagScore(Memory memory, List<String> tags) {
        if (tags.isEmpty()) {
            return 0;
        }
        List<String> mine = memory.getTags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        int points = 0;
        for (String tag : tags) {
            if (mine.contains(tag)) {
                points += 4;
            }
        }
        return points;
    }

    /** Every {@code #tag} token must sit on the entry (AND semantics). */
    private static boolean matchesAllTags(Memory memory, List<String> tags) {
        if (tags.isEmpty()) {
            return true;
        }
        List<String> mine = memory.getTags().stream()
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .collect(Collectors.toList());
        return mine.containsAll(tags);
    }

    private static List<String> tokens(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        return Arrays.stream(query.toLowerCase().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .collect(Collectors.toList());
    }

    /** Plain words only ({@code #tag} tokens excluded). */
    private static List<String> wordTokens(String query) {
        return tokens(query).stream()
                .filter(token -> !token.startsWith("#"))
                .collect(Collectors.toList());
    }

    /** {@code #tag} tokens, hash stripped and lowered (lone "#" ignored). */
    private static List<String> tagTokens(String query) {
        return tokens(query).stream()
                .filter(token -> token.startsWith("#") && token.length() > 1)
                .map(token -> token.substring(1))
                .collect(Collectors.toList());
    }

    /** Snippet tokens: {@code #tag} counts as its word, so excerpts still quote. */
    private static List<String> plainTokens(String query) {
        return tokens(query).stream()
                .map(token -> token.startsWith("#") && token.length() > 1
                        ? token.substring(1) : token)
                .filter(token -> !token.isEmpty() && !token.equals("#"))
                .collect(Collectors.toList());
    }

    private record Scored(Memory memory, int score) {
    }
}
