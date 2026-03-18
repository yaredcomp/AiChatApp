package com.lj.aichatapp.service.ai.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.ai.AIService;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class OpenRouterService implements AIService {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final UserPreferences prefs;

    public OpenRouterService(UserPreferences prefs) {
        this.prefs = prefs;
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    @Override
    public CompletableFuture<String> send(List<Message> messages, String model, Consumer<String> onChunk) {
        CompletableFuture<String> future = new CompletableFuture<>();

        try {
            List<Map<String, String>> apiMessages = new ArrayList<>();
            for (Message msg : messages) {
                Map<String, String> m = new HashMap<>();
                m.put("role", msg.getRole().toValue());
                m.put("content", msg.getContent());
                apiMessages.add(m);
            }

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", apiMessages);
            body.put("stream", true);

            String requestBody = objectMapper.writeValueAsString(body);
            String apiKey = prefs.getApiKey().trim();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://openrouter.ai/api/v1/chat/completions"))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Java-HttpClient")
                    .header("X-Title", "iTutor")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
                .thenAccept(response -> {
                    if (response.statusCode() == 200) {
                        StringBuilder fullResponse = new StringBuilder();

                        response.body().forEach(line -> {
                            if (line.startsWith("data: ")) {
                                String json = line.substring(6);
                                if ("[DONE]".equals(json)) {
                                    return;
                                }
                                try {
                                    JsonNode node = objectMapper.readTree(json);
                                    if (node.has("choices")) {
                                        JsonNode delta = node.get("choices").get(0).get("delta");
                                        if (delta.has("content")) {
                                            String chunk = delta.get("content").asText();
                                            fullResponse.append(chunk);
                                            if (onChunk != null) {
                                                onChunk.accept(chunk);
                                            }
                                        }
                                    }
                                } catch (JsonProcessingException e) {
                                }
                            }
                        });
                        future.complete(fullResponse.toString());
                    } else {
                        String responseBody = response.body().collect(Collectors.joining("\n"));
                        String errorMessage = String.format("API call failed with status: %d and body: %s", response.statusCode(), responseBody);
                        future.completeExceptionally(new RuntimeException(errorMessage));
                    }
                })
                .exceptionally(ex -> {
                    future.completeExceptionally(ex);
                    return null;
                });

        } catch (JsonProcessingException e) {
            future.completeExceptionally(e);
        }

        return future;
    }
}