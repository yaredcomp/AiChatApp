package com.lj.aichatapp.service.ai.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.ai.AIService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public class OllamaService implements AIService {

    private final HttpClient http;
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaService(UserPreferences prefs) {
        this.http = HttpClient.newHttpClient();
        String host = prefs.getOllamaHost();
        if (host != null && host.endsWith("/")) {
            this.baseUrl = host.substring(0, host.length() - 1);
        } else {
            this.baseUrl = host;
        }
        this.mapper.registerModule(new JavaTimeModule());
    }

    @Override
    public CompletableFuture<String> send(List<Message> conversation, String model, Consumer<String> onChunkReceived) {
        if (onChunkReceived == null) {
            return sendNonStreaming(conversation, model);
        }
        
        try {
            String endpoint = baseUrl + "/api/chat";

            ObjectNode root = mapper.createObjectNode();
            root.put("model", model);
            root.put("stream", true);

            ArrayNode messagesNode = root.putArray("messages");
            for (Message m : conversation) {
                ObjectNode msgNode = messagesNode.addObject();
                msgNode.put("role", m.getRole().name().toLowerCase());
                msgNode.put("content", m.getContent());
            }

            String payload = mapper.writeValueAsString(root);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            CompletableFuture<String> finalFuture = new CompletableFuture<>();
            AtomicReference<InputStream> streamRef = new AtomicReference<>();

            finalFuture.whenComplete((res, ex) -> {
                if (finalFuture.isCancelled()) {
                    InputStream is = streamRef.get();
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {
                        }
                    }
                }
            });

            http.sendAsync(req, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(resp -> {
                        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                            InputStream is = resp.body();
                            streamRef.set(is);

                            if (finalFuture.isCancelled()) {
                                try {
                                    is.close();
                                } catch (IOException e) {
                                }
                                return;
                            }

                            CompletableFuture.runAsync(() -> {
                                StringBuilder fullResponse = new StringBuilder();
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                                    String line;
                                    while ((line = reader.readLine()) != null) {
                                        if (finalFuture.isCancelled()) {
                                            break;
                                        }
                                        if (line.isBlank()) {
                                            continue;
                                        }

                                        try {
                                            JsonNode node = mapper.readTree(line);
                                            String chunk = "";
                                            if (node.has("message") && node.get("message").has("content")) {
                                                chunk = node.get("message").get("content").asText();
                                            } else if (node.has("response")) {
                                                chunk = node.get("response").asText();
                                            }

                                            if (!chunk.isEmpty()) {
                                                fullResponse.append(chunk);
                                                onChunkReceived.accept(chunk);
                                            }

                                            if (node.has("done") && node.get("done").asBoolean()) {
                                                break;
                                            }
                                        } catch (Exception e) {
                                            System.err.println("Error parsing stream line: " + line);
                                        }
                                    }

                                    if (!finalFuture.isCancelled()) {
                                        finalFuture.complete(fullResponse.toString());
                                    }
                                } catch (IOException e) {
                                    if (!finalFuture.isCancelled()) {
                                        finalFuture.completeExceptionally(e);
                                    }
                                }
                            });
                        } else {
                            try (InputStream is = resp.body()) {
                                String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                                finalFuture.completeExceptionally(new RuntimeException("Ollama error: " + resp.statusCode() + " - " + errorBody));
                            } catch (IOException e) {
                                finalFuture.completeExceptionally(new RuntimeException("Ollama error: " + resp.statusCode()));
                            }
                        }
                    })
                    .exceptionally(ex -> {
                        finalFuture.completeExceptionally(ex);
                        return null;
                    });

            return finalFuture;
        } catch (Exception e) {
            CompletableFuture<String> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    private CompletableFuture<String> sendNonStreaming(List<Message> conversation, String model) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String endpoint = baseUrl + "/api/chat";

                ObjectNode root = mapper.createObjectNode();
                root.put("model", model);
                root.put("stream", false);

                ArrayNode messagesNode = root.putArray("messages");
                for (Message m : conversation) {
                    ObjectNode msgNode = messagesNode.addObject();
                    msgNode.put("role", m.getRole().name().toLowerCase());
                    msgNode.put("content", m.getContent());
                }

                String payload = mapper.writeValueAsString(root);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(endpoint))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    JsonNode node = mapper.readTree(resp.body());
                    
                    if (node.has("message") && node.get("message").has("content")) {
                        return node.get("message").get("content").asText();
                    } else if (node.has("response")) {
                        return node.get("response").asText();
                    } else {
                        throw new RuntimeException("Unexpected response format from Ollama");
                    }
                } else {
                    throw new RuntimeException("Ollama error: " + resp.statusCode() + " " + resp.body());
                }
            } catch (IOException | InterruptedException ex) {
                throw new RuntimeException("Failed to communicate with Ollama: " + ex.getMessage(), ex);
            }
        });
    }
}