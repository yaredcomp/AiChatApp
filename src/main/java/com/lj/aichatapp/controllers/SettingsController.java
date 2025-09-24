package com.lj.aichatapp.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.utils.PreferencesManager;

import java.util.Arrays;
import java.util.List;

/**
 * Settings controller - small and focused.
 */
public class SettingsController {

    @FXML
    private ChoiceBox<String> themeChoice;
    @FXML
    private ChoiceBox<String> fontSizeChoice;
    @FXML
    private ComboBox<String> fontFamilyCombo;
    @FXML
    private ChoiceBox<String> providerChoice;
    @FXML
    private ChoiceBox<String> modelChoice;
    @FXML
    private TextField openRouterKey;
    @FXML
    private TextField groqKey;
    @FXML
    private TextField ollamaHost;

    private UserPreferences prefs;

    @FXML
    public void initialize() {
        System.out.println("SettingsController.initialize() called");
        
        // Initialize with safe null checks
        initializeChoiceBox(themeChoice, Arrays.asList("light", "dark"));
        initializeChoiceBox(fontSizeChoice, Arrays.asList("small", "medium", "large"));
        initializeChoiceBox(providerChoice, Arrays.asList("Ollama", "OpenRouter", "Groq"));
        
        // Initialize font family combo
        if (fontFamilyCombo != null) {
            try {
                fontFamilyCombo.getItems().addAll(javafx.scene.text.Font.getFamilies());
            } catch (Exception e) {
                fontFamilyCombo.getItems().addAll("System", "Arial", "Verdana");
            }
        }
        
        // Initialize model choice with empty list for now
        if (modelChoice != null) {
            modelChoice.getItems().clear();
        }
        
        // Set default Ollama host
        if (ollamaHost != null) {
            ollamaHost.setText("http://localhost:11434");
        }
    }

    private void initializeChoiceBox(ChoiceBox<String> choiceBox, List<String> items) {
        if (choiceBox != null) {
            choiceBox.getItems().clear();
            choiceBox.getItems().addAll(items);
        }
    }

    public void initWithPreferences(UserPreferences p) {
        System.out.println("SettingsController.initWithPreferences() called");
        this.prefs = p;
        
        // Set values with null safety
        setChoiceBoxValue(themeChoice, p.getTheme(), "light");
        setChoiceBoxValue(fontSizeChoice, p.getFontSize(), "medium");
        setComboBoxValue(fontFamilyCombo, p.getFontFamily(), "System");
        
        String provider = p.getProvider() != null ? p.getProvider() : "Ollama";
        setChoiceBoxValue(providerChoice, provider, "Ollama");
        
        // Initialize models AFTER setting the provider
        updateModelsForProvider(provider);
        
        String model = p.getModel() != null ? p.getModel() : "qwen3:4b";
        setChoiceBoxValue(modelChoice, model, "qwen3:4b");
        
        if (openRouterKey != null) {
            openRouterKey.setText(p.getProviderKeys().getOrDefault("openrouter", ""));
        }
        
        if (groqKey != null) {
            groqKey.setText(p.getProviderKeys().getOrDefault("groq", ""));
        }
        
        String host = p.getOllamaHost() != null ? p.getOllamaHost() : "http://localhost:11434";
        if (ollamaHost != null) {
            ollamaHost.setText(host);
        }

        // Add listener for provider changes
        if (providerChoice != null) {
            providerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null) {
                    updateModelsForProvider(newVal);
                }
            });
        }
    }

    private void setChoiceBoxValue(ChoiceBox<String> choiceBox, String value, String defaultValue) {
        if (choiceBox != null) {
            if (value != null && choiceBox.getItems().contains(value)) {
                choiceBox.setValue(value);
            } else {
                choiceBox.setValue(defaultValue);
            }
        }
    }

    private void setComboBoxValue(ComboBox<String> comboBox, String value, String defaultValue) {
        if (comboBox != null) {
            if (value != null && comboBox.getItems().contains(value)) {
                comboBox.setValue(value);
            } else if (!comboBox.getItems().isEmpty()) {
                comboBox.setValue(comboBox.getItems().get(0));
            } else {
                comboBox.setValue(defaultValue);
            }
        }
    }

    private void updateModelsForProvider(String provider) {
        if (modelChoice == null) {
            System.out.println("modelChoice is null, cannot update models");
            return;
        }
        
        List<String> models = getModelsForProvider(provider);
        System.out.println("Updating models for provider: " + provider + " -> " + models);
        
        modelChoice.getItems().clear();
        modelChoice.getItems().addAll(models);
        
        if (!models.isEmpty()) {
            modelChoice.setValue(models.get(0));
        }
    }

    private List<String> getModelsForProvider(String provider) {
        if (provider == null) {
            return Arrays.asList("qwen3:4b", "gemma3:1b");
        }
        
        switch (provider.toLowerCase()) {
            case "openrouter":
                return Arrays.asList("x-ai/grok-4-fast:free", "deepseek/deepseek-chat-v3.1:free","nvidia/nemotron-nano-9b-v2:free","openai/gpt-oss-20b:free");
            case "groq":
                return Arrays.asList("gemma2-9b-it", "lama3-8b-8192","whisper-large-v3-turbo","llama-3.1-8b-instant");
            case "ollama":
            default:
                return Arrays.asList("qwen3:4b", "gemma3:1b");
        }
    }

    @FXML
    public void onSave() {
        if (prefs == null) {
            System.out.println("Preferences is null, cannot save");
            return;
        }
        
        prefs.setTheme(themeChoice.getValue());
        prefs.setFontSize(fontSizeChoice.getValue());
        prefs.setFontFamily(fontFamilyCombo.getValue());
        prefs.setProvider(providerChoice.getValue());
        prefs.setModel(modelChoice.getValue());
        prefs.getProviderKeys().put("openrouter", openRouterKey.getText());
        prefs.getProviderKeys().put("groq", groqKey.getText());
        
        String host = ollamaHost.getText();
        if (host == null || host.trim().isEmpty()) {
            host = "http://localhost:11434";
        }
        prefs.setOllamaHost(host);

        PreferencesManager.savePreferences(prefs);
        closeWindow();
    }

    @FXML
    public void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        if (themeChoice != null && themeChoice.getScene() != null) {
            Stage stage = (Stage) themeChoice.getScene().getWindow();
            stage.close();
        }
    }
}