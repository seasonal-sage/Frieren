package com.timecapsule.search;

import com.timecapsule.auth.AuthService;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.JournalManager;
import com.timecapsule.core.Memory;
import com.timecapsule.core.Mood;
import com.timecapsule.storage.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Proves ranking, sealed-body silence, and sign-in requirement. */
class EntrySearchTest {

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        DatabaseManager.useDatabaseFile(tempDir.resolve("search.db"));
        AuthService.register("seeker", "secret123");
    }

    @AfterEach
    void tearDown() {
        AuthService.logout();
        DatabaseManager.shutdown();
    }

    @Test
    void titleOutranksBody() {
        JournalManager.createEntry("Calm day", "a storm raged", Mood.CALM, List.of(), null);
        JournalManager.createEntry("Storm", "perfectly calm", Mood.HAPPY, List.of(), null);
        List<Memory> hits = EntrySearch.search("storm");
        assertEquals(2, hits.size());
        assertEquals("Storm", hits.get(0).getTitle());
    }

    @Test
    void tagsMatch() {
        JournalManager.createEntry("Tuesday", "nothing special", Mood.NEUTRAL, List.of("birthday"), null);
        List<Memory> hits = EntrySearch.search("birthday");
        assertEquals(1, hits.size());
        assertEquals("Tuesday", hits.get(0).getTitle());
    }

    @Test
    void sealedBodiesNeverMatch() {
        JournalManager.createEntry("Future", "SECRETXYZ hidden", Mood.HAPPY, List.of(),
                Instant.now().plus(30, ChronoUnit.DAYS));
        assertTrue(EntrySearch.search("secretxyz").isEmpty(), "Sealed bodies must stay silent");
        assertEquals(1, EntrySearch.search("future").size(), "Titles still match");
    }

    @Test
    void blankQueryReturnsAllNewestFirst() {
        JournalManager.createEntry("Older", "x", Mood.CALM, List.of(), null);
        JournalManager.createEntry("Newer", "x", Mood.CALM, List.of(), null);
        List<Memory> hits = EntrySearch.search("   ");
        assertEquals(2, hits.size());
        assertEquals("Newer", hits.get(0).getTitle());
    }

    @Test
    void unsignedKeepersTurnedAway() {
        AuthService.logout();
        assertThrows(JournalException.class, () -> EntrySearch.search("anything"));
    }

    @Test
    void snippetsQuoteBodyOrFallBack() {        Memory open = JournalManager.createEntry(
                "Rain", "The rain fell all afternoon.", Mood.SAD, List.of(), null);
        assertTrue(EntrySearch.snippet(open, "rain").contains("rain"));
        Memory sealed = JournalManager.createEntry(
                "Future", "SECRETXYZ hidden", Mood.HAPPY, List.of("hopes"),
                Instant.now().plus(30, ChronoUnit.DAYS));
        String fallback = EntrySearch.snippet(sealed, "future");
        assertTrue(fallback.contains("Future"));
        assertTrue(!fallback.contains("SECRETXYZ"));
    }

    @Test
    void hashTagMatchesTagsOnly() {
        JournalManager.createEntry("Party", "birthday cake everywhere", Mood.HAPPY, List.of("birthday"), null);
        JournalManager.createEntry("Quiet", "birthday neighbours sang", Mood.CALM, List.of(), null);
        List<Memory> hits = EntrySearch.search("#birthday");
        assertEquals(1, hits.size(), "Body mentions must not match #tag");
        assertEquals("Party", hits.get(0).getTitle());
    }

    @Test
    void hashTagIsCaseInsensitive() {
        JournalManager.createEntry("Party", "cake", Mood.HAPPY, List.of("Birthday"), null);
        assertEquals(1, EntrySearch.search("#birthday").size());
        assertEquals(1, EntrySearch.search("#BIRTHDAY").size());
    }

    @Test
    void mixedTagAndWordQuery() {
        JournalManager.createEntry("Beach", "rain at noon", Mood.CALM, List.of("holiday"), null);
        JournalManager.createEntry("Hills", "sun all day", Mood.HAPPY, List.of("holiday"), null);
        List<Memory> hits = EntrySearch.search("#holiday rain");
        assertEquals(1, hits.size());
        assertEquals("Beach", hits.get(0).getTitle());
    }

    @Test
    void allTagsSortedWithCounts() {
        JournalManager.createEntry("One", "x", Mood.CALM, List.of("Zebra", "apple"), null);
        JournalManager.createEntry("Two", "x", Mood.CALM, List.of("apple"), null);
        List<EntrySearch.TagCount> tags = EntrySearch.allTags();
        assertEquals(2, tags.size());
        assertEquals("apple", tags.get(0).name());
        assertEquals(2, tags.get(0).count());
        assertEquals("Zebra", tags.get(1).name());
    }

    @Test
    void sealedTagsMatchWithoutLeaking() {
        JournalManager.createEntry("Future", "SECRETXYZ hidden", Mood.HAPPY, List.of("hopes"),
                Instant.now().plus(30, ChronoUnit.DAYS));
        List<Memory> hits = EntrySearch.search("#hopes");
        assertEquals(1, hits.size());
        assertTrue(!EntrySearch.snippet(hits.get(0), "#hopes").contains("SECRETXYZ"));
    }
}
