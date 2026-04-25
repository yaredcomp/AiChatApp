package edu.du.et.chatapp.service.ai.providers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.du.et.chatapp.models.Message;
import edu.du.et.chatapp.models.MessageRole;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.service.local.LlamaServerManager;
import edu.du.et.chatapp.services.ai.BaseAIService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EmbeddedLlamaService extends BaseAIService {

    private final LlamaServerManager manager;
    private final UserPreferences prefs;

    public EmbeddedLlamaService(LlamaServerManager manager, UserPreferences prefs) {
        super();
        this.manager = manager;
        this.prefs = prefs;
    }

    @Override
    public CompletableFuture<String> send(List<Message> messages, String model, Consumer<String> onChunk) {
        // Use the model provided in the parameter, fallback to preferences if null
        String selectedModel = (model != null && !model.isEmpty() && !"default".equalsIgnoreCase(model)) 
                               ? model : prefs.getModel();
                               
        // Resolve alias if present (e.g. "My Tiny" -> "tinyllama.gguf")
        String localModelFilename = (selectedModel != null) 
                                    ? prefs.getLocalModelAliases().getOrDefault(selectedModel, selectedModel)
                                    : null;

        // Ensure server is running or switch model if needed
        try {
            if (localModelFilename == null || localModelFilename.isEmpty() || "default".equalsIgnoreCase(localModelFilename)) {
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
        
        // Extract system prompt and merge messages
        StringBuilder systemPromptBuilder = new StringBuilder();
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
                continue; 
            }

            String role = m.getRole().toString().toLowerCase();
            String content = m.getContent();

            if (lastRole != null && lastRole.equals(role)) {
                String newContent = lastNode.get("content").asText() + "\n\n" + content;
                lastNode.put("content", newContent);
            } else {
                lastNode = messagesNode.addObject();
                lastNode.put("role", role);
                lastNode.put("content", content);
                lastRole = role;
            }
        }
        
        // Ensure strictly starts with USER
        if (messagesNode.size() > 0) {
            JsonNode firstMsg = messagesNode.get(0);
            String firstRole = firstMsg.get("role").asText();
            
            if ("assistant".equals(firstRole)) {
                ObjectNode dummyUser = mapper.createObjectNode();
                dummyUser.put("role", "user");
                dummyUser.put("content", systemPromptBuilder.length() > 0 ? systemPromptBuilder.toString() : "(Conversation started)");
                messagesNode.insert(0, dummyUser);
                systemPromptBuilder.setLength(0);
            } else if ("user".equals(firstRole)) {
                if (systemPromptBuilder.length() > 0) {
                    ObjectNode firstUserMsg = (ObjectNode) firstMsg;
                    String newContent = systemPromptBuilder.toString() + "\n\n" + firstUserMsg.get("content").asText();
                    firstUserMsg.put("content", newContent);
                }
            }
        } else if (systemPromptBuilder.length() > 0) {
            ObjectNode sysAsUser = messagesNode.addObject();
            sysAsUser.put("role", "user");
            sysAsUser.put("content", systemPromptBuilder.toString());
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

        client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                .thenAccept(response -> {
                    if (response.statusCode() != 200) {
                        try (InputStream is = response.body()) {
                            String errorBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                            future.completeExceptionally(new RuntimeException("Local Llama Error: " + response.statusCode() + " - " + errorBody));
                        } catch (IOException e) {
                            future.completeExceptionally(new RuntimeException("Local Llama Error: " + response.statusCode()));
                        }
                        return;
                    }

                    try (InputStream is = response.body();
                         BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
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
