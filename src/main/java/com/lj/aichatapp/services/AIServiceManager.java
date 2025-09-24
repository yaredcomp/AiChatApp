package com.lj.aichatapp.services;

import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.UserPreferences;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manager that returns the appropriate provider instance based on preferences.
 */
public class AIServiceManager {

    private final UserPreferences prefs;

    public AIServiceManager(UserPreferences prefs) {
        this.prefs = prefs;
    }

    public AIProvider getProvider() {
        String provider = prefs.getProvider();
        switch (provider.toLowerCase()) {
            case "ollama":
                return new OllamaService(prefs.getOllamaHost());
            case "openrouter":
                return new OpenRouterService(prefs.getProviderKeys().getOrDefault("openrouter", ""));
            case "groq":
                return new GroqService(prefs.getProviderKeys().getOrDefault("groq", ""));
            default:
                return new OllamaService(prefs.getOllamaHost());
        }
    }

    public CompletableFuture<String> send(List<Message> conversation, String model) {
        return getProvider().sendMessage(conversation, model);
    }
}
