package com.lj.aichatapp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UserPreferences {
    private String theme = "light";
    private int fontSize = 14; 
    private String fontFamily = "System";
    private String provider = "Ollama";
    private String model = "default";
    private String ollamaHost = "http://localhost:11434";
    private Map<String, String> providerKeys = new HashMap<>();
    private Map<String, List<String>> customModels = new HashMap<>();
    
    // Map: Custom Name -> Actual Filename (e.g. "My Tiny" -> "tinyllama.gguf")
    private Map<String, String> localModelAliases = new HashMap<>();
    
    // This prompt defines the core behavior of the AI. It is not user-editable.
    private final String systemPrompt = "You are an academic assistant named iTutor. Your sole purpose is to help users with their academic tasks, such as learning, research, generating study materials, and answering educational questions. Do not engage in casual conversation or any requests outside of this academic scope. If a user asks for something non-academic, politely decline and remind them of your purpose.";

    public UserPreferences() {
        // Initialize defaults with known working/free models
        
        // Ollama: Start empty. We will auto-fetch installed models to ensure they work.
        customModels.put("ollama", new ArrayList<>()); 
        
        // OpenRouter: Free models
        customModels.put("openrouter", new ArrayList<>(Arrays.asList(
            "google/gemma-7b-it:free",
            "mistralai/mistral-7b-instruct:free",
            "openchat/openchat-7:free",
            "huggingfaceh4/zephyr-7b-beta:free",
            "microsoft/phi-3-mini-128k-instruct:free"
        )));
        
        // Groq: Free-tier models
        customModels.put("groq", new ArrayList<>(Arrays.asList(
            "llama3-8b-8192",
            "llama3-70b-8192",
            "gemma-7b-it"
        )));
        
        // Local: Placeholder
        customModels.put("local lm", new ArrayList<>());
    }

    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getOllamaHost() {
        return ollamaHost;
    }

    public void setOllamaHost(String ollamaHost) {
        this.ollamaHost = ollamaHost;
    }

    public Map<String, String> getProviderKeys() {
        return providerKeys;
    }

    public void setProviderKeys(Map<String, String> providerKeys) {
        this.providerKeys = providerKeys;
    }

    public Map<String, List<String>> getCustomModels() {
        return customModels;
    }

    public void setCustomModels(Map<String, List<String>> customModels) {
        this.customModels = customModels;
    }
    
    public Map<String, String> getLocalModelAliases() {
        return localModelAliases;
    }

    public void setLocalModelAliases(Map<String, String> localModelAliases) {
        this.localModelAliases = localModelAliases;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    /**
     * Gets the API key for the currently configured provider.
     * @return The API key, or an empty string if not found.
     */
    public String getApiKey() {
        if (provider == null) return "";
        // Normalize to lowercase to match how keys are saved in SettingsController
        return providerKeys.getOrDefault(provider.toLowerCase(), "");
    }
}
