package com.lj.aichatapp.service.ai.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lj.aichatapp.context.AppContext;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.ai.AIService;
import com.lj.aichatapp.service.local.LlamaServerManager;
import com.lj.aichatapp.utils.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EmbeddedLlamaService implements AIService {

    private final HttpClient client;
    private final ObjectMapper mapper;

    public EmbeddedLlamaService() {
        this.client = HttpClient.newHttpClient();
        this.mapper = new ObjectMapper();
    }

    @Override
    public CompletableFuture<String> send(List<Message> messages, String model, Consumer<String> onChunk) {
        LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
        UserPreferences prefs = AppContext.getInstance().getPreferences();
        
        // Use the selected model filename from preferences
        String selectedModel = prefs.getModel();
        // Resolve alias if present (e.g. "My Tiny" -> "tinyllama.gguf")
        String localModelFilename = prefs.getLocalModelAliases().getOrDefault(selectedModel, selectedModel);

        // Ensure server is running or switch model if needed
        try {
            if (localModelFilename == null || localModelFilename.isEmpty() || "default".equals(localModelFilename)) {
                return CompletableFuture.failedFuture(new IllegalStateException("No local model selected. Please select or import a model in Settings > Providers > Local Llama."));
            }
            // Always call startServer. The manager handles "already running same model" check efficiently.
            manager.startServer(localModelFilename);
        } catch (IOException e) {
            return CompletableFuture.failedFuture(new RuntimeException("Failed to start/switch local Llama server", e));
        }

        ObjectNode payload = mapper.createObjectNode();
        payload.put("model", "local-model"); 
        payload.put("stream", true);
        
        ArrayNode messagesNode = payload.putArray("messages");
        
        // Strategy: Strict role alternation enforcement.
        // 1. Extract system prompt.
        // 2. Merge consecutive messages of the same role.
        // 3. Ensure the first message is 'user' by prepending system prompt to it (or dummy if needed).
        
        StringBuilder systemPromptBuilder = new StringBuilder();
        
        // Extract system messages
        for (Message m : messages) {
            if (m.getRole() == MessageRole.SYSTEM) {
                if (systemPromptBuilder.length() > 0) systemPromptBuilder.append("\n\n");
                systemPromptBuilder.append(m.getContent());
            }
        }

        String lastRole = null;
        ObjectNode lastNode = null;

        for (Message m : messages) {
            if (m.getRole() == MessageRole.SYSTEM) {
                continue; // Already extracted
            }

            String role = m.getRole().toString().toLowerCase();
            String content = m.getContent();

            if (lastRole != null && lastRole.equals(role)) {
                // Merge with previous message to enforce alternation
                if (lastNode != null) {
                    String newContent = lastNode.get("content").asText() + "\n\n" + content;
                    lastNode.put("content", newContent);
                }
            } else {
                // Add new message
                lastNode = messagesNode.addObject();
                lastNode.put("role", role);
                lastNode.put("content", content);
                lastRole = role;
            }
        }
        
        // Ensure strictly starts with USER for models like Gemma
        if (messagesNode.size() > 0) {
            JsonNode firstMsg = messagesNode.get(0);
            String firstRole = firstMsg.get("role").asText();
            
            if ("assistant".equals(firstRole)) {
                // Insert a dummy user message at the beginning if history started with assistant
                ObjectNode dummyUser = mapper.createObjectNode();
                dummyUser.put("role", "user");
                dummyUser.put("content", systemPromptBuilder.length() > 0 ? systemPromptBuilder.toString() : "(Conversation started)");
                messagesNode.insert(0, dummyUser);
                
                // Clear system prompt builder as it's now used
                systemPromptBuilder.setLength(0);
            } else if ("user".equals(firstRole)) {
                // Prepend system prompt to first user message
                if (systemPromptBuilder.length() > 0) {
                    ObjectNode firstUserMsg = (ObjectNode) firstMsg;
                    String newContent = systemPromptBuilder.toString() + "\n\n" + firstUserMsg.get("content").asText();
                    firstUserMsg.put("content", newContent);
                }
            }
        } else {
            // No messages? Send just the system prompt as a user message to start
            if (systemPromptBuilder.length() > 0) {
                ObjectNode sysAsUser = messagesNode.addObject();
                sysAsUser.put("role", "user");
                sysAsUser.put("content", systemPromptBuilder.toString());
            }
        }

        String requestBody;
        try {
            requestBody = mapper.writeValueAsString(payload);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(manager.getApiUrl() + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        CompletableFuture<String> future = new CompletableFuture<>();
        StringBuilder fullContent = new StringBuilder();

        // Asynchronous streaming
        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        try {
                            String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                            future.completeExceptionally(new RuntimeException("Local Llama Error: " + response.statusCode() + " - " + errorBody));
                        } catch (IOException e) {
                            future.completeExceptionally(new RuntimeException("Local Llama Error: " + response.statusCode()));
                        }
                        return;
                    }

                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) break;

                                try {
                                    JsonNode chunkNode = mapper.readTree(data);
                                    if (chunkNode.has("choices") && chunkNode.get("choices").isArray() && chunkNode.get("choices").size() > 0) {
                                        JsonNode choice = chunkNode.get("choices").get(0);
                                        if (choice.has("delta") && choice.get("delta").has("content")) {
                                            JsonNode contentNode = choice.get("delta").get("content");
                                            if (contentNode != null && !contentNode.isNull()) {
                                                String content = contentNode.asText();
                                                if (content != null && !content.isEmpty()) {
                                                    fullContent.append(content);
                                                    if (onChunk != null) {
                                                        onChunk.accept(content);
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) { }
                            }
                        }
                        future.complete(fullContent.toString());
                    } catch (IOException e) {
                        future.completeExceptionally(e);
                    }
                })
                .exceptionally(e -> {
                    future.completeExceptionally(e);
                    return null;
                });

        return future;
    }
}
