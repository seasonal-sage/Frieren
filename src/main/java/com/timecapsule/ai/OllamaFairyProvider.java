package com.timecapsule.ai;

import com.timecapsule.ai.ChatHistory.ChatMessage;
import com.timecapsule.ui.FairyDialogueProvider;
import com.timecapsule.ui.StaticFairyLines;

import java.util.List;

/**
 * Frieren's mind (Module: AI companion). Implements the same
 * {@link FairyDialogueProvider} seam as the static script — greetings come
 * from the local model, and anything the stars refuse falls back to the
 * scripted lines. Swap {@link #MODEL} to change brains.
 */
public class OllamaFairyProvider implements FairyDialogueProvider {

    public static final String MODEL = "qwen2.5:3b";

    private static final String PERSONA = "You are Frieren, a gentle, slightly sleepy elven mage"
            + " bound as guardian to a black grimoire that serves as someone's personal diary."
            + " Speak warmly and briefly (1 to 3 sentences), with old-world charm and the occasional"
            + " quiet observation about time and memory. Never break character, never mention being"
            + " an AI or language model, never lecture at length.";

    private final OllamaClient client;
    private final FairyDialogueProvider fallback = new StaticFairyLines();

    public OllamaFairyProvider() {
        this(new OllamaClient(MODEL));
    }

    /** Test seam: inject a client pointed anywhere (or nowhere). */
    OllamaFairyProvider(OllamaClient client) {
        this.client = client;
    }

    @Override
    public String lineFor(String pageId) {
        try {
            return client.ask(PERSONA + " The keeper has just opened the " + pageName(pageId)
                    + " of the grimoire. Greet them in 1 or 2 sentences, in character.",
                    List.of(), "Greet me.");
        } catch (Exception e) {
            return fallback.lineFor(pageId);
        }
    }

    /** Free conversation for the chat panel (throws when the stars are out). */
    public String chat(List<ChatMessage> history, String user, String pageId) throws Exception {
        return client.ask(PERSONA + " You are speaking with the keeper of the grimoire while"
                + " they look at " + pageName(pageId) + ".", history, user);
    }

    /** True when the server answers and our model is downloaded. */
    public boolean awake() {
        return client.reachable() && client.modelReady();
    }

    private static String pageName(String pageId) {
        switch (pageId == null ? "" : pageId) {
            case "cover":
                return "the front cover";
            case "contents":
                return "the table of contents";
            case "entries":
                return "the entries pages";
            case "capsules":
                return "the sealed capsules";
            case "calendar":
                return "the calendar";
            case "habits":
                return "the practices page";
            case "stats":
                return "the star charts";
            case "export":
                return "the back cover";
            default:
                return "a page";
        }
    }
}
