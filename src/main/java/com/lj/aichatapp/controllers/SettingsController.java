package com.lj.aichatapp.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.infrastructure.preferences.PreferencesManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class SettingsController {

    @FXML
    private VBox root;
    @FXML
    private ToggleButton themeToggle;
    @FXML
    private ComboBox<String> fontFamilyCombo;
    @FXML
    private Spinner<Integer> fontSizeSpinner;
    @FXML
    private TextArea systemPromptArea;
    @FXML
    private ChoiceBox<String> providerChoice;
    @FXML
    private ListView<String> modelsList;
    @FXML
    private TextField newModelField;
    @FXML
    private Label currentModelLabel;
    @FXML
    private TextField ollamaHost;
    @FXML
    private PasswordField openRouterKeyField;
    @FXML
    private TextField openRouterKeyText;
    @FXML
    private ToggleButton openRouterToggle;
    @FXML
    private PasswordField groqKeyField;
    @FXML
    private TextField groqKeyText;
    @FXML
    private ToggleButton groqToggle;
    @FXML
    private VBox generalSection;
    @FXML
    private VBox providersSection;
    @FXML
    private VBox modelsSection;

    private UserPreferences prefs;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @FXML
    public void initialize() {
        // Populate font families
        fontFamilyCombo.setItems(FXCollections.observableArrayList(Font.getFamilies()));

        // Populate providers
        providerChoice.setItems(FXCollections.observableArrayList("Ollama", "OpenRouter", "Groq"));
        providerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> updateModelsList(newVal));

        // Bind visibility of API key fields
        bindApiKeyVisibility(openRouterKeyField, openRouterKeyText, openRouterToggle);
        bindApiKeyVisibility(groqKeyField, groqKeyText, groqToggle);
        
        // Set up theme toggle icons
        themeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> updateThemeToggleIcon(newVal));
        
        // Update current model label when selection changes
        modelsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentModelLabel.setText(newVal);
            }
        });
        
        // Initialize sidebar navigation
        switchToGeneral();
    }
    
    @FXML
    private void switchToGeneral() {
        generalSection.setVisible(true);
        generalSection.setManaged(true);
        providersSection.setVisible(false);
        providersSection.setManaged(false);
        modelsSection.setVisible(false);
        modelsSection.setManaged(false);
    }
    
    @FXML
    private void switchToProviders() {
        generalSection.setVisible(false);
        generalSection.setManaged(false);
        providersSection.setVisible(true);
        providersSection.setManaged(true);
        modelsSection.setVisible(false);
        modelsSection.setManaged(false);
    }
    
    @FXML
    private void switchToModels() {
        generalSection.setVisible(false);
        generalSection.setManaged(false);
        providersSection.setVisible(false);
        providersSection.setManaged(false);
        modelsSection.setVisible(true);
        modelsSection.setManaged(true);
    }

    public void initWithPreferences(UserPreferences prefs) {
        this.prefs = prefs;

        // Set initial values
        boolean isDark = "dark".equalsIgnoreCase(prefs.getTheme());
        themeToggle.setSelected(isDark);
        updateThemeToggleIcon(isDark);
        applyThemeToWindow(isDark);
        applyFontSettings();

        // Correctly set the Integer value for the Spinner
        fontSizeSpinner.getValueFactory().setValue(prefs.getFontSize());
        
        fontFamilyCombo.setValue(prefs.getFontFamily());
        
        // Set system prompt (read-only)
        systemPromptArea.setText(prefs.getSystemPrompt());

        providerChoice.setValue(prefs.getProvider());
        ollamaHost.setText(prefs.getOllamaHost());
        
        // Load keys safely
        String orKey = prefs.getProviderKeys().getOrDefault("openrouter", "");
        openRouterKeyField.setText(orKey);
        openRouterKeyText.setText(orKey);
        
        String gKey = prefs.getProviderKeys().getOrDefault("groq", "");
        groqKeyField.setText(gKey);
        groqKeyText.setText(gKey);
        
        // Initial population of models list
        updateModelsList(prefs.getProvider());
        
        // Select the current model if it exists in the list
        if (prefs.getModel() != null) {
            currentModelLabel.setText(prefs.getModel());
            if (modelsList.getItems().contains(prefs.getModel())) {
                modelsList.getSelectionModel().select(prefs.getModel());
            }
        }
        
        // Auto-fetch models for Ollama if selected, to ensure we show what's installed
        if ("Ollama".equalsIgnoreCase(prefs.getProvider())) {
            fetchModels(true);
        }
    }

    private void updateModelsList(String provider) {
        if (provider == null || prefs == null) return;
        
        String key = provider.toLowerCase();
        List<String> models = prefs.getCustomModels().getOrDefault(key, new ArrayList<>());
        modelsList.setItems(FXCollections.observableArrayList(models));
    }

    private void bindApiKeyVisibility(PasswordField passField, TextField textField, ToggleButton toggle) {
        textField.managedProperty().bind(toggle.selectedProperty());
        textField.visibleProperty().bind(toggle.selectedProperty());
        passField.managedProperty().bind(toggle.selectedProperty().not());
        passField.visibleProperty().bind(toggle.selectedProperty().not());
        textField.textProperty().bindBidirectional(passField.textProperty());
    }
    
    private void updateThemeToggleIcon(boolean isDark) {
        FontIcon icon = new FontIcon(isDark ? "fas-moon" : "fas-sun");
        icon.setIconSize(16);
        themeToggle.setGraphic(icon);
        applyThemeToWindow(isDark);
    }
    
    private void applyThemeToWindow(boolean isDark) {
        if (root.getScene() != null) {
            if (isDark) {
                if (!root.getScene().getRoot().getStyleClass().contains("dark")) {
                    root.getScene().getRoot().getStyleClass().add("dark");
                }
            } else {
                root.getScene().getRoot().getStyleClass().remove("dark");
            }
        }
    }
    
    private void applyFontSettings() {
        if (prefs != null) {
            String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", prefs.getFontFamily(), prefs.getFontSize());
            root.setStyle(style);
        }
    }
    
    @FXML
    private void onAddModel() {
        String newModel = newModelField.getText();
        if (newModel != null && !newModel.isBlank()) {
            String provider = providerChoice.getValue().toLowerCase();
            List<String> models = prefs.getCustomModels().computeIfAbsent(provider, k -> new ArrayList<>());
            
            if (!models.contains(newModel)) {
                models.add(newModel);
                updateModelsList(providerChoice.getValue());
                modelsList.getSelectionModel().select(newModel);
                newModelField.clear();
            }
        }
    }
    
    @FXML
    private void onDeleteModel() {
        String selected = modelsList.getSelectionModel().getSelectedItem();
        if (selected != null) {
            String provider = providerChoice.getValue().toLowerCase();
            List<String> models = prefs.getCustomModels().get(provider);
            if (models != null) {
                models.remove(selected);
                updateModelsList(providerChoice.getValue());
            }
        }
    }
    
    @FXML
    private void onFetchModels() {
        fetchModels(false);
    }
    
    private void fetchModels(boolean silent) {
        String provider = providerChoice.getValue();
        if (provider == null) return;
        
        CompletableFuture.runAsync(() -> {
            try {
                List<String> fetchedModels = new ArrayList<>();
                String url = "";
                String authHeader = null;
                
                switch (provider.toLowerCase()) {
                    case "ollama":
                        url = (ollamaHost.getText().endsWith("/") ? ollamaHost.getText() : ollamaHost.getText() + "/") + "api/tags";
                        break;
                    case "openrouter":
                        url = "https://openrouter.ai/api/v1/models";
                        break;
                    case "groq":
                        url = "https://api.groq.com/openai/v1/models";
                        authHeader = "Bearer " + groqKeyField.getText();
                        break;
                }
                
                if (url.isEmpty()) return;
                
                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url));
                if (authHeader != null) {
                    builder.header("Authorization", authHeader);
                }
                
                HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonNode jsonRoot = objectMapper.readTree(response.body());
                    if (provider.equalsIgnoreCase("ollama")) {
                        if (jsonRoot.has("models")) {
                            jsonRoot.get("models").forEach(node -> fetchedModels.add(node.get("name").asText()));
                        }
                    } else {
                        // OpenRouter and Groq follow OpenAI format
                        if (jsonRoot.has("data")) {
                            jsonRoot.get("data").forEach(node -> fetchedModels.add(node.get("id").asText()));
                        }
                    }
                    
                    Platform.runLater(() -> {
                        if (!fetchedModels.isEmpty()) {
                            String pKey = provider.toLowerCase();
                            List<String> currentModels = prefs.getCustomModels().computeIfAbsent(pKey, k -> new ArrayList<>());
                            
                            // For Ollama, we want to REPLACE the list to ensure it matches installed models exactly
                            if (pKey.equals("ollama")) {
                                currentModels.clear();
                                currentModels.addAll(fetchedModels);
                            } else {
                                // For others, merge
                                for (String m : fetchedModels) {
                                    if (!currentModels.contains(m)) {
                                        currentModels.add(m);
                                    }
                                }
                            }
                            
                            updateModelsList(provider);
                            if (!silent) {
                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Successfully fetched " + fetchedModels.size() + " models.");
                                alert.initOwner(root.getScene().getWindow());
                                alert.show();
                            }
                        }
                    });
                } else {
                    if (!silent) {
                        Platform.runLater(() -> {
                            Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to fetch models. Status: " + response.statusCode());
                            alert.initOwner(root.getScene().getWindow());
                            alert.show();
                        });
                    }
                }
                
            } catch (Exception e) {
                if (!silent) {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Error fetching models: " + e.getMessage());
                        alert.initOwner(root.getScene().getWindow());
                        alert.show();
                    });
                }
            }
        });
    }

    @FXML
    private void onSave() {
        prefs.setTheme(themeToggle.isSelected() ? "dark" : "light");
        prefs.setFontSize(fontSizeSpinner.getValue());
        prefs.setFontFamily(fontFamilyCombo.getValue());
        prefs.setProvider(providerChoice.getValue());
        
        // Save selected model
        String selectedModel = modelsList.getSelectionModel().getSelectedItem();
        if (selectedModel != null) {
            prefs.setModel(selectedModel);
        }

        prefs.setOllamaHost(ollamaHost.getText());
        
        // Get key from the field, ensuring we get the latest value
        String orKey = openRouterKeyField.getText();
        String gKey = groqKeyField.getText();
        
        prefs.getProviderKeys().put("openrouter", orKey);
        prefs.getProviderKeys().put("groq", gKey);

        PreferencesManager.savePreferences(prefs);
        closeWindow();
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) fontFamilyCombo.getScene().getWindow();
        stage.close();
    }
}
