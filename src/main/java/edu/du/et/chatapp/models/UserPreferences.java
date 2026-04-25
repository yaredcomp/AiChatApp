/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.*;

/**
 * Represents the user's customized application settings and configuration.
 *
 * <p>This model is persisted to disk and loaded at application startup.
 */
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

  /**
   * Maps a display name (alias) to the actual model filename for local models.
   * Example: "My Model" -> "llama3.gguf"
   */
  private Map<String, String> localModelAliases = new HashMap<>();

  /**
   * The core system instruction that defines the AI's persona and limitations.
   */
  private String systemPrompt = "You are an academic assistant named Academia ChatAI. "
      + "Your sole purpose is to help users with their academic tasks, such as learning, research, "
      + "generating study materials, and answering educational questions. "
      + "Do not engage in casual conversation or any requests outside of this academic scope. "
      + "If a user asks for something non-academic, politely decline and remind them of your purpose.";

  /**
   * Default constructor initializing provider-specific model lists.
   */
  public UserPreferences() {
    customModels.put("ollama", new ArrayList<>());

    customModels.put("openrouter", new ArrayList<>(Arrays.asList(
        "google/gemma-7b-it:free",
        "mistralai/mistral-7b-instruct:free",
        "openchat/openchat-7:free",
        "huggingfaceh4/zephyr-7b-beta:free",
        "microsoft/phi-3-mini-128k-instruct:free"
    )));

    customModels.put("groq", new ArrayList<>(Arrays.asList(
        "llama3-8b-8192",
        "llama3-70b-8192",
        "gemma-7b-it"
    )));

    customModels.put("local ai", new ArrayList<>());
  }

  // --- Getters and Setters ---

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

  public void setSystemPrompt(String systemPrompt) {
    this.systemPrompt = systemPrompt;
  }

  /**
   * Retrieves the API key for the currently active provider.
   *
   * @return The API key string, or an empty string if none is configured.
   */
  public String getApiKey() {
    if (provider == null) {
      return "";
    }
    String p = provider.toLowerCase();
    // Local providers do not use API keys
    if (p.equals("local ai") || p.equals("local lm") || p.equals("ollama")) {
        return "";
    }
    return providerKeys.getOrDefault(p, "");
  }
}
