package com.lj.aichatapp.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import java.io.IOException;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Basic Ollama client talking to local Ollama HTTP server. Ollama default:
 * http://localhost:11434
 */
public class OllamaService implements AIProvider {

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final ObjectMapper mapper = new ObjectMapper();

    public OllamaService(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Override
    public CompletableFuture<String> sendMessage(List<Message> conversation, String model) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Build the conversation in the format Ollama expects
                StringBuilder promptBuilder = new StringBuilder();
                for (Message m : conversation) {
                    String role = m.getRole() == MessageRole.USER ? "user" : "assistant";
                    promptBuilder.append(role).append(": ").append(m.getContent()).append("\n");
                }
                // Add the final "assistant:" to indicate we want a response
                promptBuilder.append("assistant: ");

                String prompt = promptBuilder.toString();

                // Create the payload according to Ollama's API
                String payload = mapper.createObjectNode()
                        .put("model", model)
                        .put("prompt", prompt)
                        .put("stream", false)  // Important: disable streaming for simple response
                        .toString();

                System.out.println("Sending to Ollama: " + payload);

                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(baseUrl + "/api/generate"))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
                
                System.out.println("Ollama response: " + resp.body());

                if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                    JsonNode node = mapper.readTree(resp.body());
                    
                    // Check if response is complete
                    if (node.has("done") && node.get("done").asBoolean()) {
                        if (node.has("response")) {
                            return node.get("response").asText().trim();
                        } else if (node.has("text")) {
                            return node.get("text").asText().trim();
                        } else {
                            return "Received empty response from Ollama";
                        }
                    } else {
                        // Handle streaming response or incomplete response
                        if (node.has("response")) {
                            return node.get("response").asText().trim();
                        } else {
                            return "Response not complete yet";
                        }
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