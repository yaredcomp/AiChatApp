package edu.du.et.chatapp.services.ai;

import edu.du.et.chatapp.models.Message;
import edu.du.et.chatapp.models.MessageRole;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.service.ai.providers.EmbeddedLlamaService;
import edu.du.et.chatapp.service.ai.providers.GroqService;
import edu.du.et.chatapp.service.ai.providers.OllamaService;
import edu.du.et.chatapp.service.ai.providers.OpenRouterService;
import edu.du.et.chatapp.service.local.LlamaServerManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AIServiceManager {

    private final UserPreferences prefs;
    private final LlamaServerManager llamaServerManager;

    public AIServiceManager(UserPreferences prefs, LlamaServerManager llamaServerManager) {
        this.prefs = prefs;
        this.llamaServerManager = llamaServerManager;
    }

    public UserPreferences getPrefs() {
        return prefs;
    }

    public AIService getProvider() {
        String provider = prefs.getProvider();
        if (provider == null) provider = "Ollama";

        switch (provider.toLowerCase()) {
            case "ollama":
                return new OllamaService(prefs);
            case "openrouter":
                return new OpenRouterService(prefs);
            case "groq":
                return new GroqService(prefs);
            case "local ai":
            case "local lm":
                return new EmbeddedLlamaService(llamaServerManager, prefs);
            default:
                return new OpenRouterService(prefs);
        }
    }

    public CompletableFuture<String> send(List<Message> conversation, String model, Consumer<String> onChunkReceived) {
        String actualModel = model;
        if (actualModel == null || "default".equalsIgnoreCase(actualModel) || actualModel.isBlank()) {
            String provider = prefs.getProvider();
            if (provider == null) provider = "OpenRouter";

            switch (provider.toLowerCase()) {
                case "groq":
                    actualModel = "llama3-8b-8192";
                    break;
                case "openrouter":
                    actualModel = "google/gemma-7b-it:free";
                    break;
                case "local ai":
                case "local lm":
                    actualModel = "local-model";
                    break;
                case "ollama":
                default:
                    actualModel = "llama3";
                    break;
            }
        }
        
        List<Message> conversationWithSystemPrompt = new ArrayList<>();
        conversationWithSystemPrompt.add(new Message(MessageRole.SYSTEM, prefs.getSystemPrompt()));
        conversationWithSystemPrompt.addAll(conversation);
        
        return getProvider().send(conversationWithSystemPrompt, actualModel, onChunkReceived);
    }
}
