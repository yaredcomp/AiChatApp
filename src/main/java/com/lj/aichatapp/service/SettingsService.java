package com.lj.aichatapp.service;

import com.lj.aichatapp.models.UserPreferences;

public class SettingsService {

    private final UserPreferences preferences;

    public SettingsService(UserPreferences preferences) {
        this.preferences = preferences;
    }

    public UserPreferences getPreferences() {
        return preferences;
    }

    public void updatePreferences(UserPreferences newPrefs) {
        preferences.setTheme(newPrefs.getTheme());
        preferences.setFontSize(newPrefs.getFontSize());
        preferences.setFontFamily(newPrefs.getFontFamily());
        preferences.setProvider(newPrefs.getProvider());
        preferences.setModel(newPrefs.getModel());
        preferences.setOllamaHost(newPrefs.getOllamaHost());
        preferences.setProviderKeys(newPrefs.getProviderKeys());
        preferences.setCustomModels(newPrefs.getCustomModels());
    }

    public String getTheme() {
        return preferences.getTheme();
    }

    public void setTheme(String theme) {
        preferences.setTheme(theme);
    }

    public int getFontSize() {
        return preferences.getFontSize();
    }

    public void setFontSize(int fontSize) {
        preferences.setFontSize(fontSize);
    }

    public String getFontFamily() {
        return preferences.getFontFamily();
    }

    public void setFontFamily(String fontFamily) {
        preferences.setFontFamily(fontFamily);
    }

    public String getProvider() {
        return preferences.getProvider();
    }

    public void setProvider(String provider) {
        preferences.setProvider(provider);
    }

    public String getModel() {
        return preferences.getModel();
    }

    public void setModel(String model) {
        preferences.setModel(model);
    }

    public String getOllamaHost() {
        return preferences.getOllamaHost();
    }

    public void setOllamaHost(String host) {
        preferences.setOllamaHost(host);
    }

    public String getApiKey() {
        return preferences.getApiKey();
    }

    public void setApiKey(String provider, String apiKey) {
        preferences.getProviderKeys().put(provider.toLowerCase(), apiKey);
    }
}