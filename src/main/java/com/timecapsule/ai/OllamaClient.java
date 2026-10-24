package com.timecapsule.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.timecapsule.ai.ChatHistory.ChatMessage;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Speaks to a local Ollama server (Module: AI companion). Plain JDK HTTP +
 * Jackson — no new libraries. Blocking by design: callers run it off the
 * FX thread (see FairyChat). Everything stays on this machine.
 */
public class OllamaClient {

    private static final Duration ASK_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration PROBE_TIMEOUT = Duration.ofSeconds(3);

    private final String model;
    private final URI endpoint;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaClient(String model) {
        this(model, URI.create("http://localhost:11434/api/chat"));
    }

    /** Test seam: point at any chat-compatible endpoint. */
    OllamaClient(String model, URI endpoint) {
        this.model = model;
        this.endpoint = endpoint;
        this.http = HttpClient.newBuilder()
                .connectTimeout(PROBE_TIMEOUT)
                .build();
    }

    /**
     * Asks the model with a persona, recent history, and the keeper's words.
     * Replies are capped short and warm in the request itself.
     */
    public String ask(String system, List<ChatMessage> history, String user)
            throws IOException, InterruptedException {
        ObjectNode request = mapper.createObjectNode();
        request.put("model", model);
        request.put("stream", false);
        ArrayNode messages = request.putArray("messages");
        messages.addObject().put("role", "system").put("content", system);
        for (ChatMessage turn : history) {
            messages.addObject().put("role", turn.role()).put("content", turn.content());
        }
        messages.addObject().put("role", "user").put("content", user);
        ObjectNode options = request.putObject("options");
        options.put("num_predict", 120);
        options.put("temperature", 0.7);

        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint)
                .timeout(ASK_TIMEOUT)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(request)))
                .build();
        HttpResponse<String> response = http.send(httpRequest, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("Frieren's stars are cloudy (HTTP " + response.statusCode() + ").");
        }
        JsonNode content = mapper.readTree(response.body()).path("message").path("content");
        if (content.isMissingNode() || content.asText().isBlank()) {
            throw new IOException("Frieren answered in riddles (empty reply).");
        }
        return content.asText().strip();
    }

    /** Is the Ollama server awake (model or not)? */
    public boolean reachable() {
        try {
            URI tags = URI.create(endpoint.getScheme() + "://" + endpoint.getAuthority() + "/api/tags");
            HttpRequest probe = HttpRequest.newBuilder(tags).timeout(PROBE_TIMEOUT).GET().build();
            return http.send(probe, HttpResponse.BodyHandlers.ofString()).statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    /** Is our model downloaded and listed? */
    public boolean modelReady() {
        try {
            URI tags = URI.create(endpoint.getScheme() + "://" + endpoint.getAuthority() + "/api/tags");
            HttpRequest probe = HttpRequest.newBuilder(tags).timeout(PROBE_TIMEOUT).GET().build();
            HttpResponse<String> response =
                    http.send(probe, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }
            for (JsonNode node : mapper.readTree(response.body()).path("models")) {
                if (model.equals(node.path("name").asText())
                        || node.path("name").asText().startsWith(model + ":")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
