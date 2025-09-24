package com.lj.aichatapp.models;

import java.util.HashMap;
import java.util.Map;

public class UserPreferences {

    private String theme = "light"; // light/dark
    private String fontSize = "medium"; // small/medium/large
    private String fontFamily = "System";
    private String provider = "Ollama"; // default
    private String model = "default";
    private Map<String, String> providerKeys = new HashMap<>();
    private String ollamaHost = "http://localhost:11434";

    // getters and setters
    public String getTheme() {
        return theme;
    }

    public void setTheme(String theme) {
        this.theme = theme;
    }

    public String getFontSize() {
        return fontSize;
    }

    public void setFontSize(String fontSize) {
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

    public Map<String, String> getProviderKeys() {
        return providerKeys;
    }

    public String getOllamaHost() {
        return ollamaHost;
    }

    public void setOllamaHost(String ollamaHost) {
        this.ollamaHost = ollamaHost;
    }
}
