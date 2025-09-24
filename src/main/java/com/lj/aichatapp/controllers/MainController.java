package com.lj.aichatapp.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.lj.aichatapp.models.Conversation;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.services.AIServiceManager;
import com.lj.aichatapp.utils.FxUtils;
import com.lj.aichatapp.utils.PreferencesManager;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;

/**
 * Main chat controller.
 */
public class MainController {

    @FXML
    public VBox messagesBox;
    @FXML
    public ScrollPane scrollPane;
    @FXML
    public TextField inputField;
    @FXML
    public Button btnSend;
    @FXML
    public Label typingIndicator;
    @FXML
    public Button btnSettings;
    @FXML
    public Button btnClear;

    private Stage stage;
    private Conversation conversation;
    private UserPreferences prefs;
    private AIServiceManager aiManager;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void postInit() {
        // load preferences & conversation
        prefs = PreferencesManager.loadPreferences();
        conversation = PreferencesManager.loadConversation(Conversation.class, new Conversation());
        aiManager = new AIServiceManager(prefs);

        // render old messages
        conversation.getMessages().forEach(this::addMessageToView);

        // ensure scroll auto
        scrollPane.vvalueProperty().bind(messagesBox.heightProperty());

        applyFontSettings();
    }

    private void applyFontSettings() {
        // Apply font family and size based on prefs (simple)
        String fontSize = prefs.getFontSize();
        int size;
        switch (fontSize) {
            case "small":
                size = 12;
                break;
            case "large":
                size = 18;
                break;
            default:
                size = 14;
                break;
        }
        messagesBox.setStyle("-fx-font-size: " + size + "px; -fx-font-family: '" + prefs.getFontFamily() + "';");
    }

    @FXML
    public void onSendMessage() {
        String text = inputField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        inputField.clear();

        Message userMsg = new Message(MessageRole.USER, text);
        conversation.addMessage(userMsg);
        addMessageToView(userMsg);

        // persist
        PreferencesManager.saveConversation(conversation);

        // show typing indicator
        setTyping(true);

        // send to AI asynchronously
        CompletableFuture<String> respFuture = aiManager.send(conversation.getMessages(), prefs.getModel());
        respFuture.whenComplete((resp, err) -> {
            Platform.runLater(() -> setTyping(false));
            if (err != null) {
                Platform.runLater(() -> showError("AI error: " + err.getMessage()));
            } else {
                Message aiMsg = new Message(MessageRole.ASSISTANT, resp);
                conversation.addMessage(aiMsg);
                Platform.runLater(() -> {
                    addMessageToView(aiMsg);
                    PreferencesManager.saveConversation(conversation);
                });
            }
        });
    }

    private void setTyping(boolean on) {
        Platform.runLater(() -> typingIndicator.setText(on ? "AI is typing..." : ""));
    }

    public void addMessageToView(Message m) {
        String text = m.getContent();
        Node bubbleNode;
        if (m.getRole() == MessageRole.USER) {
            bubbleNode = FxUtils.makeBubble(text, "message-user");
            bubbleNode.setStyle("-fx-alignment: center-right;");
        } else {
            bubbleNode = FxUtils.makeBubble(text, "message-ai");
            bubbleNode.setStyle("-fx-alignment: center-left;");
        }

        messagesBox.getChildren().add(bubbleNode);
        FxUtils.fadeIn(bubbleNode);
    }

    @FXML
    public void onOpenSettings() {
        try {
            System.out.println("Attempting to load settings FXML...");

            java.net.URL url = getClass().getResource("/views/settings.fxml");
            if (url == null) {
                showError("FXML file not found: /views/settings.fxml");
                return;
            }
            System.out.println("FXML URL: " + url);

            FXMLLoader loader = new FXMLLoader(url);
            AnchorPane settingsPane = loader.load();
            System.out.println("FXML loaded successfully");

            SettingsController ctrl = loader.getController();
            if (ctrl == null) {
                showError("Controller not initialized");
                return;
            }

            // Initialize with preferences
            ctrl.initWithPreferences(prefs);

            // Create and show settings window
            Stage settingsStage = new Stage();
            settingsStage.setTitle("Settings");
            settingsStage.initModality(Modality.APPLICATION_MODAL);
            settingsStage.initOwner(stage);

            Scene scene = new Scene(settingsPane);
            settingsStage.setScene(scene);

            // Set reasonable size
            settingsStage.setWidth(500);
            settingsStage.setHeight(450);

            settingsStage.showAndWait();

            // Reload preferences after settings close
            prefs = PreferencesManager.loadPreferences();
            aiManager = new AIServiceManager(prefs);
            applyFontSettings();
            applyTheme();

        } catch (IOException e) {
            showError("Error opening settings: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    @FXML
    public void onClearConversation() {
        conversation.clear();
        messagesBox.getChildren().clear();
        PreferencesManager.saveConversation(conversation);
    }

    @FXML
    public void onCopyLast() {
        if (conversation.getMessages().isEmpty()) {
            return;
        }
        Message last = conversation.getMessages().get(conversation.getMessages().size() - 1);
        Clipboard cb = Clipboard.getSystemClipboard();
        ClipboardContent content = new ClipboardContent();
        content.putString(last.getContent());
        cb.setContent(content);
    }

    private void showError(String text) {
        Alert a = new Alert(Alert.AlertType.ERROR, text, ButtonType.OK);
        a.initOwner(stage);
        a.showAndWait();
    }

    private void applyTheme() {
        if ("dark".equalsIgnoreCase(prefs.getTheme())) {
            if (!stage.getScene().getRoot().getStyleClass().contains("dark")) {
                stage.getScene().getRoot().getStyleClass().add("dark");
            }
        } else {
            stage.getScene().getRoot().getStyleClass().remove("dark");
        }

        // Also make sure the CSS file is loaded
        if (stage.getScene() != null) {
            stage.getScene().getStylesheets().clear();
            stage.getScene().getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
        }
    }
}
