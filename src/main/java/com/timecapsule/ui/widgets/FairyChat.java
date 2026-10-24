package com.timecapsule.ui.widgets;

import com.timecapsule.ai.ChatHistory;
import com.timecapsule.ai.ChatHistory.ChatMessage;
import com.timecapsule.ai.OllamaFairyProvider;
import com.timecapsule.auth.AuthService;
import com.timecapsule.auth.User;
import com.timecapsule.core.JournalException;
import com.timecapsule.core.Memory;
import com.timecapsule.search.EntrySearch;
import com.timecapsule.storage.ChatDao;
import com.timecapsule.ui.PageTracker;
import com.timecapsule.audio.SoundService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * A letter-style conversation card with Frieren (Module: AI companion).
 * Perches above her sprite; collapses back to nothing. Thinking happens
 * off the FX thread — the book never freezes mid-thought.
 */
public class FairyChat extends VBox {

    private final OllamaFairyProvider brain;
    private final ChatHistory history = new ChatHistory(8);
    private final ChatDao chats = new ChatDao();
    private long loadedUserId = -1;
    private final VBox transcript = new VBox(6);
    private final ScrollPane scroll = new ScrollPane(transcript);
    private final TextField input = new TextField();
    private final Label thinking = new Label("consulting the stars…");
    private Task<String> current;
    private String pageId = "cover";

    public FairyChat(OllamaFairyProvider brain) {
        super(8);
        this.brain = brain;
        getStyleClass().add("fairy-chat");
        setVisible(false);
        setManaged(false);
        setMaxWidth(320);

        Label head = new Label("✦ Talking with Frieren");
        head.getStyleClass().add("chat-head");
        Button close = new Button("×");
        close.getStyleClass().add("fairy-x");
        close.setOnAction(e -> close());
        Button forget = new Button("forget");
        forget.getStyleClass().add("fairy-x");
        forget.setOnAction(e -> forgetHistory());
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox top = new HBox(head, spacer, forget, close);
        top.setAlignment(Pos.CENTER_LEFT);

        transcript.setAlignment(Pos.TOP_LEFT);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(220);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("clear-scroll");

        thinking.getStyleClass().add("chat-thinking");
        thinking.setVisible(false);
        thinking.setManaged(false);

        input.setPromptText("Ask Frieren…");
        input.getStyleClass().add("field");
        input.setOnAction(e -> send());
        Button send = new Button("Go");
        send.getStyleClass().addAll("ink-btn", "chat-send");
        send.setOnAction(e -> send());
        HBox row = new HBox(8, input, send);
        row.setAlignment(Pos.CENTER);
        HBox.setHgrow(input, Priority.ALWAYS);

        setPadding(new Insets(10));
        Label hint = new Label("Tip: “find birthday” searches pages — “find #tag” finds a tag.");
        hint.getStyleClass().add("chat-hint");
        getChildren().addAll(top, scroll, thinking, row, hint);
    }

    /** Opens the card (greeting on first open, history reloaded per keeper). */
    public void open() {
        pageId = PageTracker.current();
        loadForCurrentUser();
        setVisible(true);
        setManaged(true);
        if (transcript.getChildren().isEmpty()) {
            addFrierenRow("You called, keeper? Ask, and I shall answer.");
        }
        Platform.runLater(input::requestFocus);
    }

    public void close() {
        if (current != null) {
            current.cancel();
        }
        setVisible(false);
        setManaged(false);
    }

    /**
     * (Re)loads Frieren's long memory for the signed-in keeper — latest 30
     * turns, oldest first. No-ops when signed out or already loaded.
     */
    public void loadForCurrentUser() {
        User keeper = AuthService.currentUser();
        if (keeper == null || keeper.getId() == loadedUserId) {
            return;
        }
        loadedUserId = keeper.getId();
        List<ChatDao.ChatRow> rows;
        try {
            rows = chats.recent(loadedUserId, 30);
        } catch (Exception e) {
            return; // Stars unreadable — in-memory chat still works.
        }
        history.clear();
        transcript.getChildren().clear();
        for (ChatDao.ChatRow row : rows) {
            history.add(row.role(), row.content());
            if ("user".equals(row.role())) {
                addUserRow(row.content());
            } else {
                addFrierenRow(row.content());
            }
        }
    }

    /**
     * Wipes Frieren's memory of this keeper — grimoire rows, rolling
     * memory, and the visible transcript — then greets anew.
     */
    private void forgetHistory() {
        User keeper = AuthService.currentUser();
        if (keeper != null) {
            try {
                chats.clear(keeper.getId());
            } catch (Exception ignored) {
                // Visible transcript still clears below.
            }
        }
        history.clear();
        transcript.getChildren().clear();
        addFrierenRow("It is done, keeper — I remember nothing. Shall we begin again?");
    }

    /** Remembers a turn in memory and, when signed in, in the grimoire. */
    private void remember(String role, String content) {
        history.add(role, content);
        User keeper = AuthService.currentUser();
        if (keeper == null) {
            return;
        }
        loadedUserId = keeper.getId();
        try {
            chats.append(keeper.getId(), role, content);
        } catch (Exception ignored) {
            // The conversation survives in memory even if ink fails.
        }
    }

    private void send() {
        String text = input.getText() == null ? "" : input.getText().strip();
        if (text.isEmpty() || (current != null && current.isRunning())) {
            return;
        }
        input.clear();
        if (text.toLowerCase().startsWith("find ") || text.equalsIgnoreCase("find")) {
            answerFind(text.length() > 4 ? text.substring(4).strip() : "");
            return;
        }
        addUserRow(text);
        remember("user", text);
        thinking.setVisible(true);
        thinking.setManaged(true);

        List<ChatMessage> context = history.messages();
        current = new Task<>() {
            @Override
            protected String call() throws Exception {
                return brain.chat(context, text, pageId);
            }
        };
        current.setOnSucceeded(e -> {
            String reply = current.getValue();
            remember("assistant", reply);
            addFrierenRow(reply);
            SoundService.instance().playFrierenSpeak();
            hideThinking();
        });
        current.setOnFailed(e -> {
            addFrierenRow("Frieren sleeps — start Ollama to wake her.");
            hideThinking();
        });
        Thread thread = new Thread(current, "frieren-chat");
        thread.setDaemon(true);
        thread.start();
    }

    /** "find birthday" searches the diary locally — no model call needed. */
    private void answerFind(String words) {
        if (words.isEmpty()) {
            addFrierenRow("Tell me the word to seek, keeper — “find birthday”, for instance.");
            return;
        }
        List<Memory> found;
        try {
            found = EntrySearch.search(words);
        } catch (JournalException e) {
            addFrierenRow("No keeper is signed in, so there are no pages to search.");
            return;
        }
        addUserRow("find " + words);
        remember("user", "find " + words);
        String reply;
        if (found.isEmpty()) {
            reply = "Nothing in these pages remembers \"" + words + "\" — yet. Write it first?";
        } else {
            StringBuilder pages = new StringBuilder(
                    found.size() == 1 ? "One page remembers \"" : found.size() + " pages remember \"")
                    .append(words).append("\":");
            for (Memory memory : found.subList(0, Math.min(4, found.size()))) {
                pages.append("\n· ").append(memory.getTitle()).append(" — ")
                        .append(oneLine(EntrySearch.snippet(memory, words)));
            }
            reply = pages.toString();
        }
        remember("assistant", reply);
        addFrierenRow(reply);
    }

    private static String oneLine(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").strip();
    }

    private void hideThinking() {
        thinking.setVisible(false);
        thinking.setManaged(false);
    }

    private void addUserRow(String text) {
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-user");
        bubble.setWrapText(true);
        bubble.setMaxWidth(230);
        HBox row = new HBox(bubble);
        row.setAlignment(Pos.CENTER_RIGHT);
        transcript.getChildren().add(row);
        autoscroll();
    }

    private void addFrierenRow(String text) {
        Label tag = new Label("✦");
        tag.getStyleClass().add("chat-tag");
        Label bubble = new Label(text);
        bubble.getStyleClass().add("chat-frieren");
        bubble.setWrapText(true);
        bubble.setMaxWidth(230);
        HBox row = new HBox(4, tag, bubble);
        row.setAlignment(Pos.CENTER_LEFT);
        transcript.getChildren().add(row);
        autoscroll();
    }

    private void autoscroll() {
        Platform.runLater(() -> scroll.setVvalue(1.0));
    }
}
