/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.service.ai.providers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import edu.du.et.chatapp.models.Message;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.services.ai.AIService;
import edu.du.et.chatapp.utils.Logger;

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

/**
 * Service implementation for the OpenRouter AI provider.
 *
 * <p>Handles streaming completions via the OpenRouter API.
 */
public class OpenRouterService implements AIService {

  private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;
  private final UserPreferences prefs;

  /**
   * Constructs a new OpenRouterService.
   *
   * @param prefs User preferences containing API keys and model configuration.
   */
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
      String apiKey = prefs.getApiKey().trim();
      if (apiKey.isEmpty()) {
        future.completeExceptionally(new RuntimeException(
            "OpenRouter API Key is missing. Please enter it in Settings > Providers."));
        return future;
      }

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

      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(API_URL))
          .header("Authorization", "Bearer " + apiKey)
          .header("Content-Type", "application/json")
          .header("User-Agent", "AcademiaChatAI-Desktop")
          .header("X-Title", "Academia ChatAI")
          .POST(HttpRequest.BodyPublishers.ofString(requestBody))
          .build();

      httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofLines())
          .thenAccept(response -> handleStreamResponse(response, future, onChunk))
          .exceptionally(ex -> {
            future.completeExceptionally(ex);
            return null;
          });

    } catch (JsonProcessingException e) {
      future.completeExceptionally(e);
    }

    return future;
  }

  /**
   * Processes the streaming response lines from the API.
   */
  private void handleStreamResponse(HttpResponse<java.util.stream.Stream<String>> response,
                                     CompletableFuture<String> future,
                                     Consumer<String> onChunk) {
    if (response.statusCode() == 200) {
      StringBuilder fullResponse = new StringBuilder();

      response.body().forEach(line -> {
        if (line.startsWith("data: ")) {
          String json = line.substring(6).trim();
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
            Logger.error("Failed to parse JSON stream line", e);
          }
        }
      });
      future.complete(fullResponse.toString());
    } else {
      String responseBody = response.body().collect(Collectors.joining("\n"));
      future.completeExceptionally(new RuntimeException(
          String.format("OpenRouter API error (Status: %d): %s", response.statusCode(), responseBody)));
    }
  }
}
