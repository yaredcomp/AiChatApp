/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.du.et.chatapp.context.AppContext;
import edu.du.et.chatapp.infrastructure.preferences.PreferencesManager;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.service.local.LlamaServerManager;
import edu.du.et.chatapp.utils.ModernAlert;
import edu.du.et.chatapp.utils.ThemeManager;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controller for the application settings view.
 *
 * <p>Manages configuration for AI providers, local model management, and UI customization.
 */
public class SettingsController {

  // --- FXML UI Components ---
  @FXML private VBox root;
  @FXML private ToggleButton themeToggle;
  @FXML private ComboBox<String> fontFamilyCombo;
  @FXML private Spinner<Integer> fontSizeSpinner;
  @FXML private TextArea systemPromptArea;
  @FXML private ChoiceBox<String> providerChoice;
  @FXML private ListView<String> modelsList;
  @FXML private TextField newModelField;
  @FXML private Label currentModelLabel;

  // --- Icon Placeholders ---
  @FXML private Label iconGeneralTab;
  @FXML private Label iconGeneralHeader;
  @FXML private Label iconTheme;
  @FXML private Label iconProvidersTab;
  @FXML private Label iconProvidersHeader;
  @FXML private Label iconBrowse;
  @FXML private Label iconOpenRouterEye;
  @FXML private Label iconGroqEye;
  @FXML private Label iconModelsHeader;
  @FXML private Label iconRefresh;
  @FXML private Label iconDelete;
  @FXML private Label iconPlusCircle;
  @FXML private Label iconFileImport;
  @FXML private Label iconSave;

  // --- Config Sections ---
  @FXML private VBox ollamaConfigBox;
  @FXML private VBox localLlamaConfigBox;
  @FXML private VBox openRouterConfigBox;
  @FXML private VBox groqConfigBox;

  // --- Input Fields ---
  @FXML private TextField ollamaHost;
  @FXML private TextField localModelPathField;
  @FXML private PasswordField openRouterKeyField;
  @FXML private TextField openRouterKeyText;
  @FXML private ToggleButton openRouterToggle;
  @FXML private PasswordField groqKeyField;
  @FXML private TextField groqKeyText;
  @FXML private ToggleButton groqToggle;

  // --- Internal State ---
  private UserPreferences prefs;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final ObjectMapper objectMapper = new ObjectMapper();

  /**
   * Initializer called by FXMLLoader.
   */
  public void initialize() {
    initializeIcons();
    setupProviderConfiguration();
    setupModelsListListener();
    setupThemeBinding();

    fontFamilyCombo.setItems(FXCollections.observableArrayList(Font.getFamilies()));
    bindApiKeyVisibility(openRouterKeyField, openRouterKeyText, openRouterToggle, iconOpenRouterEye);
    bindApiKeyVisibility(groqKeyField, groqKeyText, groqToggle, iconGroqEye);
  }

  /**
   * Programmatically sets up icons to maintain FXML compatibility with Scene Builder.
   */
  private void initializeIcons() {
    iconGeneralTab.setGraphic(new FontIcon("fas-sliders-h:16"));
    iconGeneralHeader.setGraphic(new FontIcon("fas-sliders-h:20"));
    iconTheme.setGraphic(new FontIcon("fas-moon:16"));
    iconProvidersTab.setGraphic(new FontIcon("fas-server:16"));
    iconProvidersHeader.setGraphic(new FontIcon("fas-server:20"));
    iconBrowse.setGraphic(new FontIcon("fas-folder-open:16"));
    iconOpenRouterEye.setGraphic(new FontIcon("fas-eye:16"));
    iconGroqEye.setGraphic(new FontIcon("fas-eye:16"));
    iconModelsHeader.setGraphic(new FontIcon("fas-database:20"));
    iconRefresh.setGraphic(new FontIcon("fas-sync-alt:16:white"));
    iconDelete.setGraphic(new FontIcon("fas-trash-alt:16:white"));
    iconPlusCircle.setGraphic(new FontIcon("fas-plus-circle:16:white"));
    iconFileImport.setGraphic(new FontIcon("fas-file-import:16:white"));
    iconSave.setGraphic(new FontIcon("fas-check:16:white"));
  }

  /**
   * Configures the provider selection logic.
   */
  private void setupProviderConfiguration() {
    providerChoice.setItems(FXCollections.observableArrayList("Ollama", "Local AI", "OpenRouter", "Groq"));
    providerChoice.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      updateModelsList(newVal);
      updateConfigVisibility(newVal);
    });
  }

  /**
   * Sets up selection monitoring for the models list.
   */
  private void setupModelsListListener() {
    modelsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null) {
        currentModelLabel.setText(newVal);
        String provider = providerChoice.getValue();
        if ("Local AI".equalsIgnoreCase(provider) || "Local LM".equalsIgnoreCase(provider)) {
          LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
          String filename = prefs.getLocalModelAliases().getOrDefault(newVal, newVal);
          localModelPathField.setText(manager.getModelsDir().resolve(filename).toString());
        }
      }
    });
  }

  /**
   * Binds the theme toggle button to the global ThemeManager.
   */
  private void setupThemeBinding() {
    ThemeManager tm = ThemeManager.getInstance();
    
    // Bidirectional binding between UI toggle and Manager property
    themeToggle.selectedProperty().bindBidirectional(tm.darkModeProperty());
    
    // Reactive style update for settings window itself
    tm.darkModeProperty().addListener((obs, oldVal, newVal) -> {
        tm.applyThemeToParent(root);
        updateThemeToggleIcon(newVal);
    });
  }

  /**
   * Injects the current preferences into the controller and populates the UI.
   */
  public void initWithPreferences(UserPreferences prefs) {
    this.prefs = prefs;

    // Set initial state from prefs (ThemeManager is updated by binding)
    fontSizeSpinner.getValueFactory().setValue(prefs.getFontSize());
    fontFamilyCombo.setValue(prefs.getFontFamily());
    systemPromptArea.setText(prefs.getSystemPrompt());

    String provider = prefs.getProvider();
    if ("Local LM".equalsIgnoreCase(provider)) provider = "Local AI";
    providerChoice.setValue(provider);
    
    ollamaHost.setText(prefs.getOllamaHost());

    String orKey = prefs.getProviderKeys().getOrDefault("openrouter", "");
    openRouterKeyField.setText(orKey);
    openRouterKeyText.setText(orKey);

    String gKey = prefs.getProviderKeys().getOrDefault("groq", "");
    groqKeyField.setText(gKey);
    groqKeyText.setText(gKey);

    updateModelsList(provider);

    if (prefs.getModel() != null) {
      String currentModel = prefs.getModel();
      currentModelLabel.setText(currentModel);
      if (modelsList.getItems().contains(currentModel)) {
        modelsList.getSelectionModel().select(currentModel);
      }
    }

    if ("Ollama".equalsIgnoreCase(provider) || "Local AI".equalsIgnoreCase(provider)) {
      fetchModels(true);
    }
    
    applyFontSettings();
    ThemeManager.getInstance().applyThemeToParent(root);
    updateThemeToggleIcon(ThemeManager.getInstance().isDarkMode());
  }

  private void updateConfigVisibility(String provider) {
    if (provider == null) return;
    String p = provider.toLowerCase();

    ollamaConfigBox.setVisible("ollama".equals(p));
    ollamaConfigBox.setManaged("ollama".equals(p));
    localLlamaConfigBox.setVisible("local ai".equals(p) || "local lm".equals(p));
    localLlamaConfigBox.setManaged("local ai".equals(p) || "local lm".equals(p));
    openRouterConfigBox.setVisible("openrouter".equals(p));
    openRouterConfigBox.setManaged("openrouter".equals(p));
    groqConfigBox.setVisible("groq".equals(p));
    groqConfigBox.setManaged("groq".equals(p));
  }

  private void updateModelsList(String provider) {
    if (provider == null || prefs == null) return;
    String key = provider.toLowerCase();
    if (key.equals("local ai")) key = "local ai"; // Key in map matches new name
    
    List<String> models = prefs.getCustomModels().getOrDefault(key, 
        prefs.getCustomModels().getOrDefault("local lm", new ArrayList<>()));
        
    modelsList.setItems(FXCollections.observableArrayList(models));
  }

  private void bindApiKeyVisibility(PasswordField pass, TextField text, ToggleButton toggle, Label icon) {
    text.managedProperty().bind(toggle.selectedProperty());
    text.visibleProperty().bind(toggle.selectedProperty());
    pass.managedProperty().bind(toggle.selectedProperty().not());
    pass.visibleProperty().bind(toggle.selectedProperty().not());
    text.textProperty().bindBidirectional(pass.textProperty());

    toggle.selectedProperty().addListener((obs, oldVal, newVal) -> {
      icon.setGraphic(new FontIcon(newVal ? "fas-eye-slash:16" : "fas-eye:16"));
    });
  }

  private void updateThemeToggleIcon(boolean isDark) {
    iconTheme.setGraphic(new FontIcon(isDark ? "fas-moon:16" : "fas-sun:16"));
  }

  @FXML
  private void onBrowseModel() {
    FileChooser fileChooser = new FileChooser();
    fileChooser.setTitle("Select GGUF Model");
    fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("GGUF Models", "*.gguf"));

    File file = fileChooser.showOpenDialog(root.getScene().getWindow());
    if (file != null) {
      String defaultName = file.getName().replace(".gguf", "");
      ModernAlert.askInput("Model Name", "Enter a name for this model:", defaultName,
          root.getScene().getWindow(), modelName -> {
            String finalName = (modelName == null || modelName.isBlank()) ? file.getName() : modelName;
            importLocalModelWithProgress(file, finalName);
          });
    }
  }

  private void importLocalModelWithProgress(File sourceFile, String modelName) {
    LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
    Path destPath = manager.getModelsDir().resolve(sourceFile.getName());

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
      fetchModels(true);

      Platform.runLater(() -> {
        String provider = providerChoice.getValue();
        if (provider != null) {
          String key = provider.toLowerCase();
          List<String> models = prefs.getCustomModels().computeIfAbsent(key, k -> new ArrayList<>());
          if (!models.contains(modelName)) {
            models.add(modelName);
            updateModelsList(provider);
          }
        }
        modelsList.getSelectionModel().select(modelName);
        currentModelLabel.setText(modelName);
        prefs.setModel(modelName);
        PreferencesManager.savePreferences(prefs);
        ModernAlert.info("Import Successful", "Model '" + modelName + "' imported.",
            root.getScene().getWindow());
      });
    });

    copyTask.setOnFailed(e -> ModernAlert.error("Import Failed",
        copyTask.getException().getMessage(), root.getScene().getWindow()));

    ModernAlert.showProgress("Importing", "Copying model...", copyTask, root.getScene().getWindow());
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
        currentModelLabel.setText(newModel);
        prefs.setModel(newModel);
        PreferencesManager.savePreferences(prefs);
        newModelField.clear();
      }
    }
  }

  @FXML
  private void onDeleteModel() {
    String selected = modelsList.getSelectionModel().getSelectedItem();
    if (selected == null) return;
    String provider = providerChoice.getValue().toLowerCase();

    if ("local ai".equals(provider) || "local lm".equals(provider)) {
      String filename = prefs.getLocalModelAliases().getOrDefault(selected, selected);
      Path modelPath = AppContext.getInstance().getLlamaServerManager().getModelsDir().resolve(filename);

      ModernAlert.confirmDanger("Delete", "Delete '" + selected + "'?", root.getScene().getWindow(),
          confirmed -> {
            if (confirmed) {
              try {
                Files.deleteIfExists(modelPath);
                prefs.getLocalModelAliases().remove(selected);
                List<String> models = prefs.getCustomModels().get(provider);
                if (models != null) {
                  models.remove(selected);
                  updateModelsList(providerChoice.getValue());
                }
                if (selected.equals(currentModelLabel.getText())) {
                  currentModelLabel.setText("None");
                  prefs.setModel(null);
                }
                PreferencesManager.savePreferences(prefs);
              } catch (IOException ex) {
                ModernAlert.error("Error", ex.getMessage(), root.getScene().getWindow());
              }
            }
          });
    } else {
      List<String> models = prefs.getCustomModels().get(provider);
      if (models != null) {
        models.remove(selected);
        updateModelsList(providerChoice.getValue());
        if (selected.equals(currentModelLabel.getText())) {
          currentModelLabel.setText("None");
          prefs.setModel(null);
        }
        PreferencesManager.savePreferences(prefs);
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

    if ("Local AI".equalsIgnoreCase(provider) || "Local LM".equalsIgnoreCase(provider)) {
      LlamaServerManager manager = AppContext.getInstance().getLlamaServerManager();
      List<String> filenames = manager.listAvailableModels();
      List<String> displayNames = new ArrayList<>();
      for (String fn : filenames) {
        String alias = null;
        for (var entry : prefs.getLocalModelAliases().entrySet()) {
          if (entry.getValue().equals(fn)) {
            alias = entry.getKey();
            break;
          }
        }
        displayNames.add(alias != null ? alias : fn);
      }
      String key = "local ai";
      prefs.getCustomModels().computeIfAbsent(key, k -> new ArrayList<>()).clear();
      prefs.getCustomModels().get(key).addAll(displayNames);
      updateModelsList(provider);
      if (!silent) ModernAlert.info("Models", "Found " + displayNames.size() + " local models.",
          root.getScene().getWindow());
      return;
    }

    CompletableFuture.runAsync(() -> {
      try {
        List<String> fetched = new ArrayList<>();
        String url = "";
        String auth = null;
        switch (provider.toLowerCase()) {
          case "ollama" -> url = prefs.getOllamaHost() + "/api/tags";
          case "openrouter" -> url = "https://openrouter.ai/api/v1/models";
          case "groq" -> {
            url = "https://api.groq.com/openai/v1/models";
            auth = "Bearer " + (groqKeyField.isVisible() ? groqKeyField.getText() : groqKeyText.getText());
          }
        }
        if (url.isEmpty()) return;

        HttpRequest.Builder rb = HttpRequest.newBuilder().uri(URI.create(url));
        if (auth != null) rb.header("Authorization", auth);
        HttpResponse<String> resp = httpClient.send(rb.build(), HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() == 200) {
          JsonNode json = objectMapper.readTree(resp.body());
          if (provider.equalsIgnoreCase("ollama")) {
            if (json.has("models")) json.get("models").forEach(n -> fetched.add(n.get("name").asText()));
          } else {
            if (json.has("data")) json.get("data").forEach(n -> fetched.add(n.get("id").asText()));
          }

          Platform.runLater(() -> {
            if (!fetched.isEmpty()) {
              List<String> current = prefs.getCustomModels().computeIfAbsent(provider.toLowerCase(), k -> new ArrayList<>());
              if (provider.equalsIgnoreCase("ollama")) {
                current.clear();
                current.addAll(fetched);
              } else {
                fetched.forEach(m -> { if (!current.contains(m)) current.add(m); });
              }
              updateModelsList(provider);
              if (!silent) ModernAlert.info("Success", "Fetched " + fetched.size() + " models.",
                  root.getScene().getWindow());
            }
          });
        }
      } catch (Exception ignored) { }
    });
  }

  @FXML
  private void onSave() {
    String provider = providerChoice.getValue();
    prefs.setProvider(provider);
    if (!"Local AI".equalsIgnoreCase(provider) && !"Local LM".equalsIgnoreCase(provider)) {
        AppContext.getInstance().getLlamaServerManager().stopServer();
    }

    prefs.setTheme(themeToggle.isSelected() ? "dark" : "light");
    prefs.setFontSize(fontSizeSpinner.getValue());
    prefs.setFontFamily(fontFamilyCombo.getValue());
    
    String model = modelsList.getSelectionModel().getSelectedItem();
    if (model != null) prefs.setModel(model);

    prefs.setOllamaHost(ollamaHost.getText());
    prefs.setSystemPrompt(systemPromptArea.getText());
    prefs.getProviderKeys().put("openrouter", openRouterKeyField.getText());
    prefs.getProviderKeys().put("groq", groqKeyField.getText());

    PreferencesManager.savePreferences(prefs);
    closeWindow();
  }

  @FXML
  private void onCancel() {
    closeWindow();
  }

  private void closeWindow() {
    ((Stage) root.getScene().getWindow()).close();
  }

  private void applyFontSettings() {
    if (prefs != null) {
      String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", 
          prefs.getFontFamily(), prefs.getFontSize());
      root.setStyle(style);
    }
  }
}
