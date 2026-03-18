package com.lj.aichatapp.service.ai;

import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.ai.providers.GroqService;
import com.lj.aichatapp.service.ai.providers.OllamaService;
import com.lj.aichatapp.service.ai.providers.OpenRouterService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class AIServiceManager {

    private final UserPreferences prefs;

    public AIServiceManager(UserPreferences prefs) {
        this.prefs = prefs;
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