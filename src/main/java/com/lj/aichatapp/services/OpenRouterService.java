package com.lj.aichatapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lj.aichatapp.models.Message;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * OpenRouter implementation for chat completion requests.
 * See: https://openrouter.ai/docs
 */
public class OpenRouterService implements AIProvider {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public OpenRouterService(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public CompletableFuture<String> sendMessage(List<Message> conversation, String model) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = "https://openrouter.ai/api/v1/chat/completions";

                // Build JSON payload in OpenAI-compatible format
                ObjectNode root = mapper.createObjectNode();
                root.put("model", model);

                ArrayNode messagesNode = root.putArray("messages");
                for (Message m : conversation) {
                    ObjectNode msgNode = messagesNode.addObject();
                    msgNode.put("role", m.getRole().name().toLowerCase());
                    msgNode.put("content", m.getContent());
                }

                String payload = mapper.writeValueAsString(root);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Authorization", "Bearer " + apiKey)
                        .header("HTTP-Referer", "http://localhost") // recommended by OpenRouter
                        .header("X-Title", "AI Chat App")          // optional but recommended
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    JsonNode jsonResp = mapper.readTree(resp.body());
                    return jsonResp.path("choices").get(0).path("message").path("content").asText();
                } else {
                    throw new RuntimeException("OpenRouter error: " + resp.statusCode() + ": " + resp.body());
                }

            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException(ex);
            }
        });
    }
}
