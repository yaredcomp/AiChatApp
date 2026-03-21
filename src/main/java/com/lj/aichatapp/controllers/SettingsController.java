package com.lj.aichatapp.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lj.aichatapp.context.AppContext;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.infrastructure.preferences.PreferencesManager;
import com.lj.aichatapp.service.local.LlamaServerManager;
import com.lj.aichatapp.utils.ModernAlert;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    
    // Config Sections
    @FXML
    private VBox ollamaConfigBox;
    @FXML
    private VBox localLlamaConfigBox;
    @FXML
    private VBox openRouterConfigBox;
    @FXML
    private VBox groqConfigBox;
    
    // Fields
    @FXML
    private TextField ollamaHost;
    @FXML
    private TextField localModelPathField;
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
        providerChoice.setItems(FXCollections.observableArrayList("Ollama", "Local LM", "OpenRouter", "Groq"));
        providerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            updateModelsList(newVal);
            updateConfigVisibility(newVal);
        });

        // Bind visibility of API key fields
        bindApiKeyVisibility(openRouterKeyField, openRouterKeyText, openRouterToggle);
        bindApiKeyVisibility(groqKeyField, groqKeyText, groqToggle);
        
        // Set up theme toggle icons
        themeToggle.selectedProperty().addListener((obs, oldVal, newVal) -> updateThemeToggleIcon(newVal));
        
        // Update current model label when selection changes
        modelsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                currentModelLabel.setText(newVal);
                // Also update local path field if in local llama mode
                if ("Local LM".equalsIgnoreCase(providerChoice.getValue())) {
                    LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
                    localModelPathField.setText(manager.getModelsDir().resolve(newVal).toString());
                }
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
        updateConfigVisibility(prefs.getProvider());
        
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
            String currentModel = prefs.getModel();
            currentModelLabel.setText(currentModel);
            if (modelsList.getItems().contains(currentModel)) {
                modelsList.getSelectionModel().select(currentModel);
            }
        }
        
        // Auto-fetch models
        if ("Ollama".equalsIgnoreCase(prefs.getProvider()) || "Local LM".equalsIgnoreCase(prefs.getProvider())) {
            fetchModels(true);
        }
    }

    private void updateConfigVisibility(String provider) {
        if (provider == null) return;
        String p = provider.toLowerCase();
        
        if (ollamaConfigBox != null) {
            ollamaConfigBox.setVisible("ollama".equals(p));
            ollamaConfigBox.setManaged("ollama".equals(p));
        }
        
        if (localLlamaConfigBox != null) {
            localLlamaConfigBox.setVisible("local lm".equals(p));
            localLlamaConfigBox.setManaged("local lm".equals(p));
        }
        
        if (openRouterConfigBox != null) {
            openRouterConfigBox.setVisible("openrouter".equals(p));
            openRouterConfigBox.setManaged("openrouter".equals(p));
        }
        
        if (groqConfigBox != null) {
            groqConfigBox.setVisible("groq".equals(p));
            groqConfigBox.setManaged("groq".equals(p));
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
    private void onBrowseModel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select GGUF Model");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GGUF Models", "*.gguf"));
        
        File file = fileChooser.showOpenDialog(root.getScene().getWindow());
        if (file != null) {
            String defaultName = file.getName().replace(".gguf", "");
            
            // Replaced native TextInputDialog with ModernAlert.askInput
            ModernAlert.askInput("Model Name", "Enter a name for this model (optional):", defaultName, root.getScene().getWindow(), modelName -> {
                 String finalName = (modelName == null || modelName.isBlank()) ? file.getName() : modelName;
                 importLocalModelWithProgress(file, finalName);
            });
        }
    }
    
    private void importLocalModelWithProgress(File sourceFile, String modelName) {
        LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
        Path destDir = manager.getModelsDir();
        Path destPath = destDir.resolve(sourceFile.getName()); 
        
        // Replaced custom Dialog with ModernAlert.showProgress
        Task<Void> copyTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                long totalBytes = sourceFile.length();
                try (InputStream in = new FileInputStream(sourceFile);
                     OutputStream out = new FileOutputStream(destPath.toFile())) {
                    
                    byte[] buffer = new byte[8192];
                    long bytesCopied = 0;
                    int read;
                    
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        bytesCopied += read;
                        updateProgress(bytesCopied, totalBytes);
                    }
                }
                return null;
            }
        };
        
        copyTask.setOnSucceeded(e -> {
            if (!modelName.equals(sourceFile.getName())) {
                prefs.getLocalModelAliases().put(modelName, sourceFile.getName());
            }
            
            fetchModels(false);
            
            ModernAlert.info("Import Complete", "Model imported to " + destDir.toString(), root.getScene().getWindow());
        });
        
        copyTask.setOnFailed(e -> {
            ModernAlert.error("Import Failed", "Failed to import model: " + copyTask.getException().getMessage(), root.getScene().getWindow());
        });
        
        ModernAlert.showProgress("Importing Model", "Copying " + sourceFile.getName() + "...", copyTask, root.getScene().getWindow());
    }
    
    @FXML
    private void onAddModel() {
        if ("Local LM".equalsIgnoreCase(providerChoice.getValue())) {
            onBrowseModel();
            return;
        }
        
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
        if (selected == null) return;
        
        String provider = providerChoice.getValue().toLowerCase();
        
        if ("local lm".equals(provider)) {
            String filename = prefs.getLocalModelAliases().getOrDefault(selected, selected);
            
            LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
            Path modelPath = manager.getModelsDir().resolve(filename);
            
            ModernAlert.confirmDanger("Delete Model", "Are you sure you want to delete '" + selected + "' from disk?", 
                root.getScene().getWindow(), 
                confirmed -> {
                    if (confirmed) {
                        try {
                            if (Files.exists(modelPath)) {
                                Files.delete(modelPath);
                            }
                            prefs.getLocalModelAliases().remove(selected);

                            List<String> models = prefs.getCustomModels().get(provider);
                            if (models != null) {
                                models.remove(selected);
                                updateModelsList(providerChoice.getValue());
                            }
                        } catch (IOException e) {
                            ModernAlert.error("Delete Failed", "Failed to delete model file: " + e.getMessage(), root.getScene().getWindow());
                        }
                    }
                });
            return;
        }
        
        List<String> models = prefs.getCustomModels().get(provider);
        if (models != null) {
            models.remove(selected);
            updateModelsList(providerChoice.getValue());
        }
    }
    
    @FXML
    private void onFetchModels() {
        fetchModels(false);
    }
    
    private void fetchModels(boolean silent) {
        String provider = providerChoice.getValue();
        if (provider == null) return;
        
        if ("Local LM".equalsIgnoreCase(provider)) {
            LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
            List<String> filenames = manager.listAvailableModels();
            
            List<String> displayNames = new ArrayList<>();
            for (String filename : filenames) {
                String alias = null;
                for (var entry : prefs.getLocalModelAliases().entrySet()) {
                    if (entry.getValue().equals(filename)) {
                        alias = entry.getKey();
                        break;
                    }
                }
                if (alias != null) {
                    displayNames.add(alias);
                } else {
                    displayNames.add(filename);
                }
            }
            
            String pKey = "local lm";
            List<String> currentModels = prefs.getCustomModels().computeIfAbsent(pKey, k -> new ArrayList<>());
            currentModels.clear();
            currentModels.addAll(displayNames);
            
            updateModelsList(provider);
            if (!silent) {
                ModernAlert.info("Models Found", "Found " + displayNames.size() + " local models.", root.getScene().getWindow());
            }
            return;
        }
        
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
                        if (jsonRoot.has("data")) {
                            jsonRoot.get("data").forEach(node -> fetchedModels.add(node.get("id").asText()));
                        }
                    }
                    
                    Platform.runLater(() -> {
                        if (!fetchedModels.isEmpty()) {
                            String pKey = provider.toLowerCase();
                            List<String> currentModels = prefs.getCustomModels().computeIfAbsent(pKey, k -> new ArrayList<>());
                            
                            if (pKey.equals("ollama")) {
                                currentModels.clear();
                                currentModels.addAll(fetchedModels);
                            } else {
                                for (String m : fetchedModels) {
                                    if (!currentModels.contains(m)) {
                                        currentModels.add(m);
                                    }
                                }
                            }
                            
                            updateModelsList(provider);
                            if (!silent) {
                                ModernAlert.info("Models Found", "Successfully fetched " + fetchedModels.size() + " models.", root.getScene().getWindow());
                            }
                        }
                    });
                } else {
                    if (!silent) {
                        Platform.runLater(() -> {
                            ModernAlert.error("Fetch Failed", "Failed to fetch models. Status: " + response.statusCode(), root.getScene().getWindow());
                        });
                    }
                }
                
            } catch (Exception e) {
                if (!silent) {
                    Platform.runLater(() -> {
                        ModernAlert.error("Fetch Error", "Error fetching models: " + e.getMessage(), root.getScene().getWindow());
                    });
                }
            }
        });
    }

    @FXML
    private void onSave() {
        String selectedProvider = providerChoice.getValue();
        prefs.setProvider(selectedProvider);
        
        // Stop local server if we switched AWAY from Local LM
        if (!"Local LM".equalsIgnoreCase(selectedProvider)) {
            AppContext.getInstance().getLlamaServerManager().stopServer();
        }
        
        prefs.setTheme(themeToggle.isSelected() ? "dark" : "light");
        prefs.setFontSize(fontSizeSpinner.getValue());
        prefs.setFontFamily(fontFamilyCombo.getValue());
        
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
