/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.controllers;

import edu.du.et.chatapp.context.AppContext;
import edu.du.et.chatapp.models.Conversation;
import edu.du.et.chatapp.models.Message;
import edu.du.et.chatapp.models.MessageRole;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.services.ChatService;
import edu.du.et.chatapp.utils.Logger;
import edu.du.et.chatapp.utils.ModernAlert;
import edu.du.et.chatapp.utils.ThemeManager;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Main Controller for the application.
 *
 * <p>Manages the chat interface, conversation history, and interaction with the AI services.
 */
public class MainController {

  // --- Constants ---
  private static final double INPUT_MIN_HEIGHT = 100;
  private static final double INPUT_MAX_HEIGHT = 500;
  private static final int THROTTLE_MS = 300;
  private static final double NAV_EXPANDED_WIDTH = 280;
  private static final double NAV_COLLAPSED_WIDTH = 60;

  // --- FXML UI Components ---
  @FXML
  private WebView chatWebView;
  @FXML
  private TextArea inputField;
  @FXML
  private Button btnSend;
  @FXML
  private Label typingIndicator;
  @FXML
  private Label charCount;
  @FXML
  private Label currentModelIndicator;
  @FXML
  private TextField searchField;
  @FXML
  private VBox emptyState;
  @FXML
  private ImageView appLogoView;
  @FXML
  private ImageView emptyStateLogo;
  @FXML
  private VBox navPanel;
  @FXML
  private VBox navContent;
  @FXML
  private Button navToggle;
  @FXML
  private ListView<Conversation> chatHistoryList;
  @FXML
  private BorderPane root;

  // --- Icon Placeholders ---
  @FXML private Label iconSettings;
  @FXML private Label iconNavToggle;
  @FXML private Label iconNewChat;
  @FXML private Label iconPrompts;
  @FXML private Label iconSend;

  // --- Internal State ---
  private Stage stage;
  private WebEngine webEngine;
  private Conversation currentConversation;
  private ChatService chatService;
  private UserPreferences prefs;
  private CompletableFuture<String> currentRequest;
  private boolean isGenerating = false;
  private FilteredList<Conversation> filteredChats;
  private boolean isNavExpanded = true;
  private String logoBase64 = "";

  /**
   * Sets the primary stage reference.
   */
  public void setStage(Stage stage) {
    this.stage = stage;
  }

  /**
   * Initializer called by FXMLLoader.
   */
  public void initialize() {
    setupInputAutoResize();
    initializeIcons();
  }

  /**
   * Post-initialization setup.
   */
  public void postInit() {
    AppContext ctx = AppContext.getInstance();
    this.chatService = ctx.getChatService();
    this.prefs = ctx.getPreferences();

    loadAppLogos();
    setupHistoryList();
    setupWebView();
    updateModelIndicator();
    setupBindings();

    if (!chatHistoryList.getItems().isEmpty()) {
      chatHistoryList.getSelectionModel().select(0);
    } else {
      onNewChat();
    }
  }

  /**
   * Programmatically sets up icons to avoid FXML LoadExceptions in Scene Builder.
   */
  private void initializeIcons() {
    if (iconSettings != null) iconSettings.setGraphic(new FontIcon("fas-cog:16"));
    if (iconNavToggle != null) iconNavToggle.setGraphic(new FontIcon("fas-align-left:16"));
    if (iconNewChat != null) iconNewChat.setGraphic(new FontIcon("fas-plus:16:white"));
    if (iconPrompts != null) iconPrompts.setGraphic(new FontIcon("fas-bookmark:16"));
    if (iconSend != null) iconSend.setGraphic(new FontIcon("fas-paper-plane:16:white"));
  }

  private void setupWebView() {
    webEngine = chatWebView.getEngine();
    webEngine.load(getClass().getResource("/html/chat_template.html").toExternalForm());

    webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
      if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
        syncThemeToWebView();
        if (!logoBase64.isEmpty()) {
          webEngine.executeScript(String.format("setAiLogo('%s')", logoBase64));
        }
        if (currentConversation != null && !currentConversation.getMessages().isEmpty()) {
          renderCurrentConversation();
        }
      }
    });
  }

  private void setupHistoryList() {
    refreshHistory();

    if (searchField != null) {
      searchField.textProperty().addListener((obs, oldText, newText) -> {
        if (filteredChats != null) {
          filteredChats.setPredicate(conv -> {
            if (newText == null || newText.isEmpty()) {
              return true;
            }
            return conv.getTitle().toLowerCase().contains(newText.toLowerCase());
          });
        }
      });
    }

    chatHistoryList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      if (newVal != null && (currentConversation == null || newVal.getId() != currentConversation.getId())) {
        loadConversation(newVal);
      }
    });

    chatHistoryList.setCellFactory(param -> new ConversationCell());
  }

  private void setupBindings() {
    btnSend.disableProperty().bind(
        Bindings.createBooleanBinding(() -> {
          boolean hasText = !inputField.getText().trim().isEmpty();
          // Allow button to be enabled if either there is text to send OR we are currently generating (to stop)
          return !hasText && !isGenerating;
        }, inputField.textProperty())
    );
  }

  /**
   * Loads and styles the app logo. Ensures images are center-cropped and circular.
   */
  private void loadAppLogos() {
    try {
      URL logoUrl = getClass().getResource("/image/chattAI Logo.png");
      if (logoUrl != null) {
        Image logoImage = new Image(logoUrl.toExternalForm(), true);
        
        logoImage.progressProperty().addListener((obs, oldVal, newVal) -> {
           if (newVal.doubleValue() == 1.0) {
               Platform.runLater(() -> {
                   if (appLogoView != null) {
                       styleImageViewAsCircle(appLogoView, logoImage, 32);
                   }
                   if (emptyStateLogo != null) {
                       styleImageViewAsCircle(emptyStateLogo, logoImage, 120);
                   }
               });
           }
        });

        // Fallback if already loaded
        if (!logoImage.isError() && logoImage.getProgress() == 1.0) {
            styleImageViewAsCircle(appLogoView, logoImage, 32);
            styleImageViewAsCircle(emptyStateLogo, logoImage, 120);
        }

        try (InputStream is = getClass().getResourceAsStream("/image/chattAI Logo.png")) {
          if (is != null) {
            byte[] bytes = is.readAllBytes();
            this.logoBase64 = Base64.getEncoder().encodeToString(bytes);
          }
        }
      }
    } catch (Exception e) {
      Logger.error("Failed to load application branding assets", e);
    }
  }

  /**
   * Utility to center-crop an Image into an ImageView and apply a circular clip.
   */
  private void styleImageViewAsCircle(ImageView iv, Image img, double size) {
    if (iv == null || img == null || img.isError()) return;
    
    iv.setImage(img);
    iv.setSmooth(true);
    iv.setPreserveRatio(true);
    
    // Calculate square crop from center
    double width = img.getWidth();
    double height = img.getHeight();
    double side = Math.min(width, height);
    
    double x = (width - side) / 2;
    double y = (height - side) / 2;
    
    iv.setViewport(new Rectangle2D(x, y, side, side));
    iv.setFitWidth(size);
    iv.setFitHeight(size);
    
    // Apply the circular clip at the center of the Fit size
    double radius = size / 2;
    iv.setClip(new Circle(radius, radius, radius));
  }

  private void setupInputAutoResize() {
    inputField.textProperty().addListener((obs, oldText, newText) -> updateInputFieldHeight());
    inputField.widthProperty().addListener((obs, oldWidth, newWidth) -> updateInputFieldHeight());
    inputField.setMinHeight(Region.USE_COMPUTED_SIZE);
  }

  private void updateInputFieldHeight() {
    Platform.runLater(() -> {
      String text = inputField.getText();
      if (text == null || text.trim().isEmpty()) {
        inputField.setPrefHeight(INPUT_MIN_HEIGHT);
        if (charCount != null) charCount.setText("");
        return;
      }

      Text helper = new Text(text);
      helper.setFont(inputField.getFont());
      helper.setWrappingWidth(inputField.getWidth() - 20);

      double requiredHeight = helper.getLayoutBounds().getHeight() + 25;
      double finalHeight = Math.min(Math.max(requiredHeight, INPUT_MIN_HEIGHT), INPUT_MAX_HEIGHT);

      inputField.setPrefHeight(finalHeight);
      if (charCount != null) {
        charCount.setText(text.length() + " characters");
      }
    });
  }

  private void refreshHistory() {
    List<Conversation> conversations = chatService.getAllConversations();
    filteredChats = new FilteredList<>(FXCollections.observableArrayList(conversations), conv -> true);
    chatHistoryList.setItems(filteredChats);
  }

  private void loadConversation(Conversation c) {
    currentConversation = c;
    chatService.loadConversationMessages(currentConversation);
    renderCurrentConversation();

    if (emptyState != null) {
      emptyState.setVisible(currentConversation.getMessages().isEmpty());
    }
  }

  private void renderCurrentConversation() {
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      webEngine.executeScript("clearMessages()");
      currentConversation.getMessages().forEach(this::addMessageToViewSync);
      chatWebView.toFront();
    }
  }

  private void updateModelIndicator() {
    if (prefs != null && currentModelIndicator != null) {
      String provider = prefs.getProvider();
      String model = prefs.getModel();
      currentModelIndicator.setText(String.format("%s - %s", provider, model != null ? model : "Default"));
    }
  }

  private String getRelativeDate(LocalDateTime dateTime) {
    if (dateTime == null) return "";
    LocalDate today = LocalDate.now();
    LocalDate date = dateTime.toLocalDate();
    long daysBetween = ChronoUnit.DAYS.between(date, today);

    if (daysBetween == 0) return "Today";
    else if (daysBetween == 1) return "Yesterday";
    else if (daysBetween < 7) return dateTime.format(DateTimeFormatter.ofPattern("EEEE"));
    else return dateTime.format(DateTimeFormatter.ofPattern("MMM d"));
  }

  @FXML
  public void onNewChat() {
    currentConversation = new Conversation();
    currentConversation.setId(-1);
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      webEngine.executeScript("clearMessages()");
    }
    chatHistoryList.getSelectionModel().clearSelection();
    if (emptyState != null) {
      emptyState.setVisible(true);
      emptyState.toFront();
    }
  }

  @FXML
  public void onToggleNav() {
    isNavExpanded = !isNavExpanded;
    double targetWidth = isNavExpanded ? NAV_EXPANDED_WIDTH : NAV_COLLAPSED_WIDTH;

    navPanel.setPrefWidth(targetWidth);
    navContent.setVisible(isNavExpanded);
    navContent.setManaged(isNavExpanded);

    if (searchField != null) {
      searchField.setVisible(isNavExpanded);
      searchField.setManaged(isNavExpanded);
    }

    if (iconNavToggle != null) {
      iconNavToggle.setGraphic(new FontIcon(isNavExpanded ? "fas-chevron-left:14" : "fas-chevron-right:14"));
    }
  }

  @FXML
  public void onSendMessage() {
    if (isGenerating) {
      // User clicked the button while generating -> TERMINATE STREAM
      if (currentRequest != null) {
          currentRequest.cancel(true);
          Logger.info("Stream terminated by user.");
      }
      return;
    }

    String text = inputField.getText();
    if (text == null || text.isBlank()) return;

    if (emptyState != null) emptyState.setVisible(false);

    inputField.clear();
    updateInputFieldHeight();

    if (currentConversation.getId() == -1) {
      currentConversation = chatService.saveOrGetCurrentConversation(currentConversation, text);
      refreshHistory();
      chatHistoryList.getSelectionModel().select(currentConversation);
    }

    Message userMsg = new Message(MessageRole.USER, text);
    currentConversation.addMessage(userMsg);
    addMessageToViewSync(userMsg);

    setGeneratingState(true);

    String aiMessageId = "msg_ai_" + System.currentTimeMillis();
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      webEngine.executeScript(String.format("appendMessage('%s', 'ai', '')", aiMessageId));
    }

    StringBuilder currentResponse = new StringBuilder();
    Timeline throttleTimeline = createStreamingTimeline(aiMessageId, currentResponse);

    currentRequest = chatService.sendMessage(currentConversation, text, chunk -> {
      if (currentRequest != null && currentRequest.isCancelled()) throw new RuntimeException("Stream aborted by user");
      Platform.runLater(() -> currentResponse.append(chunk));
    });

    throttleTimeline.playFromStart();

    currentRequest.whenComplete((resp, err) -> {
      Platform.runLater(() -> {
        throttleTimeline.stop();
        setGeneratingState(false);

        String finalResponse = currentResponse.toString();
        if (err != null) {
          if (currentRequest != null && currentRequest.isCancelled()) {
              updateWebViewMessage(aiMessageId, finalResponse + "\n\n[Generation stopped by user]");
          } else {
              handleAiError(err, finalResponse, aiMessageId);
          }
        } else {
          updateWebViewMessage(aiMessageId, finalResponse);
          chatService.saveAiResponse(currentConversation, resp);
        }
        currentRequest = null;
      });
    });
  }

  private Timeline createStreamingTimeline(String msgId, StringBuilder content) {
    Timeline timeline = new Timeline();
    timeline.setCycleCount(Timeline.INDEFINITE);
    timeline.getKeyFrames().add(new KeyFrame(Duration.millis(THROTTLE_MS), event -> {
      String text = content.toString();
      if (!text.isEmpty()) updateWebViewMessage(msgId, text);
    }));
    return timeline;
  }

  private void setGeneratingState(boolean generating) {
    this.isGenerating = generating;
    Platform.runLater(() -> {
      if (typingIndicator != null) {
          typingIndicator.setText(generating ? "Academia ChatAI is responding..." : "");
      }
      if (btnSend != null) {
          if (generating) {
            btnSend.getStyleClass().add("loading");
            btnSend.setStyle("-fx-background-color: #ef4444;");
          } else {
            btnSend.getStyleClass().remove("loading");
            btnSend.setStyle("");
          }
      }
      if (iconSend != null) {
          iconSend.setGraphic(new FontIcon(generating ? "fas-stop:16:white" : "fas-paper-plane:16:white"));
      }
    });
  }

  private String encodeBase64(String text) {
    if (text == null) return "";
    return Base64.getEncoder().encodeToString(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));
  }

  private void updateWebViewMessage(String id, String content) {
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
        webEngine.executeScript(String.format("updateMessage('%s', '%s')", id, encodeBase64(content)));
    }
  }

  private void addMessageToViewSync(Message m) {
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      String id = "msg_" + System.currentTimeMillis() + "_" + Math.random();
      String role = m.getRole() == MessageRole.USER ? "user" : "ai";
      String base64 = encodeBase64(m.getContent());
      webEngine.executeScript(String.format("appendMessage('%s', '%s', '%s')", id, role, base64));
      chatWebView.toFront();
    }
  }

  @FXML
  public void onOpenSettings() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/settings.fxml"));
      VBox settingsPane = loader.load();
      Stage settingsStage = new Stage();
      settingsStage.setTitle("Settings");
      settingsStage.initModality(Modality.APPLICATION_MODAL);
      settingsStage.initOwner(stage);
      Scene scene = new Scene(settingsPane);
      scene.getStylesheets().addAll(root.getScene().getStylesheets());
      if (root.getScene().getRoot().getStyleClass().contains("dark")) {
        scene.getRoot().getStyleClass().add("dark");
      }
      settingsStage.setScene(scene);
      SettingsController ctrl = loader.getController();
      ctrl.initWithPreferences(prefs);
      settingsStage.showAndWait();
      prefs = AppContext.getInstance().getPreferences();
      updateModelIndicator();
    } catch (Exception e) {
      showError("Error opening settings", e);
    }
  }

  @FXML
  public void onOpenPrompts() {
    try {
      FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/prompts.fxml"));
      VBox promptsPane = loader.load();
      Stage promptsStage = new Stage();
      promptsStage.setTitle("Saved Prompts");
      promptsStage.initModality(Modality.APPLICATION_MODAL);
      promptsStage.initOwner(stage);
      Scene scene = new Scene(promptsPane);
      scene.getStylesheets().addAll(root.getScene().getStylesheets());
      if (root.getScene().getRoot().getStyleClass().contains("dark")) {
        scene.getRoot().getStyleClass().add("dark");
      }
      promptsStage.setScene(scene);
      PromptsController ctrl = loader.getController();
      ctrl.applyTheme(root.getScene().getRoot().getStyleClass().contains("dark"));
      ctrl.setOnUseCallback(text -> {
        inputField.setText(text);
        inputField.requestFocus();
        inputField.positionCaret(text.length());
        updateInputFieldHeight();
      });
      promptsStage.showAndWait();
    } catch (Exception e) {
      showError("Error opening prompt library", e);
    }
  }

  @FXML
  public void onClearConversation() {
    chatService.clearConversation(currentConversation);
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      webEngine.executeScript("clearMessages()");
    }
  }

  @FXML
  private void handleInputKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ENTER && !event.isShiftDown() && !event.isControlDown()) {
      onSendMessage();
      event.consume();
    }
  }

  public void handleGlobalKeyEvent(KeyEvent event) {
    if (event.getCode() == KeyCode.N && event.isControlDown()) {
      onNewChat();
      event.consume();
    }
  }

  private void handleAiError(Throwable e, String partialResponse, String msgId) {
    Throwable cause = (e instanceof CompletionException) ? e.getCause() : e;
    String msg = (cause.getMessage() != null) ? cause.getMessage() : "";
    if (!partialResponse.isEmpty()) {
      updateWebViewMessage(msgId, partialResponse + "\n\n[Generation Interrupted]");
      chatService.saveAiResponse(currentConversation, partialResponse);
    }
    if (msg.contains("429") || msg.toLowerCase().contains("limit")) {
      showFriendlyError("Usage Limit Reached", "You have reached your limit with the AI provider.");
    } else if (msg.contains("401") || msg.toLowerCase().contains("api key")) {
      showFriendlyError("Authentication Error", "Issue with your API key. Check Settings.");
    } else {
      showError("AI Service Error", cause);
    }
  }

  private void showFriendlyError(String title, String content) {
    Alert alert = new Alert(Alert.AlertType.WARNING);
    alert.setTitle("Warning");
    alert.setHeaderText(title);
    alert.setContentText(content);
    ButtonType settingsBtn = new ButtonType("Open Settings");
    alert.getButtonTypes().add(settingsBtn);
    alert.initOwner(stage);
    alert.showAndWait().ifPresent(btn -> {
      if (btn == settingsBtn) onOpenSettings();
    });
  }

  private void showError(String title, Throwable e) {
    Logger.error(title, e);
    Alert alert = new Alert(Alert.AlertType.ERROR);
    alert.setTitle("Error");
    alert.setHeaderText(title);
    alert.setContentText("An unexpected error occurred.");
    StringWriter sw = new StringWriter();
    e.printStackTrace(new PrintWriter(sw));
    TextArea textArea = new TextArea(sw.toString());
    textArea.setEditable(false);
    textArea.setWrapText(true);
    alert.getDialogPane().setExpandableContent(new VBox(new Label("Details:"), textArea));
    alert.initOwner(stage);
    alert.showAndWait();
  }

  private void applyTheme() {
    syncThemeToWebView();
  }

  public void syncThemeToWebView() {
    boolean isDark = ThemeManager.getInstance().isDarkMode();
    if (webEngine != null && webEngine.getLoadWorker().getState() == javafx.concurrent.Worker.State.SUCCEEDED) {
      webEngine.executeScript(String.format("setTheme(%b)", isDark));
    }
  }

  private class ConversationCell extends ListCell<Conversation> {
    @Override
    protected void updateItem(Conversation item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
        setStyle("-fx-background-color: transparent;"); // Reset background
      } else {
        VBox vbox = new VBox(2);
        vbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        HBox topRow = new HBox();
        topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label titleLabel = new Label(item.getTitle());
        titleLabel.setMaxWidth(140);
        titleLabel.setEllipsisString("...");
        titleLabel.getStyleClass().add("card-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dateLabel = new Label(getRelativeDate(item.getCreatedAt()));
        dateLabel.getStyleClass().add("card-description");

        topRow.getChildren().addAll(titleLabel, spacer, dateLabel);

        Label previewLabel = new Label(item.getPreview());
        previewLabel.setMaxWidth(200);
        previewLabel.setEllipsisString("...");
        previewLabel.getStyleClass().add("card-description");

        vbox.getChildren().addAll(topRow, previewLabel);

        Region spacerRight = new Region();
        HBox.setHgrow(spacerRight, Priority.ALWAYS);

        // Edit Button
        Button editBtn = new Button();
        editBtn.getStyleClass().add("window-button");
        editBtn.setGraphic(new FontIcon("fas-pen:12"));
        editBtn.setTooltip(new Tooltip("Rename"));

        editBtn.setOnAction(e -> {
          e.consume();
          ModernAlert.askInput("Rename Chat", "Enter new title:", item.getTitle(),
              editBtn.getScene().getWindow(),
              newTitle -> {
                if (newTitle != null && !newTitle.isBlank()) {
                  chatService.updateConversationTitle(item.getId(), newTitle);
                  refreshHistory();
                }
              });
        });

        // Delete Button
        Button delBtn = new Button();
        delBtn.getStyleClass().add("danger-button");
        delBtn.setGraphic(new FontIcon("fas-trash:12"));
        delBtn.setTooltip(new Tooltip("Delete"));

        delBtn.setOnAction(e -> {
          e.consume();
          ModernAlert.confirmDanger("Delete Chat", "Are you sure you want to delete '" + item.getTitle() + "'?",
              delBtn.getScene().getWindow(),
              confirmed -> {
                if (confirmed) {
                  chatService.deleteConversation(item.getId());
                  refreshHistory();
                  if (currentConversation != null && currentConversation.getId() == item.getId()) {
                    onNewChat();
                  }
                }
              });
        });

        HBox btnBox = new HBox(4, editBtn, delBtn);
        btnBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        btnBox.setVisible(false);

        HBox hbox = new HBox(5);
        hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        hbox.setOnMouseEntered(e -> btnBox.setVisible(true));
        hbox.setOnMouseExited(e -> btnBox.setVisible(false));

        hbox.getChildren().addAll(vbox, spacerRight, btnBox);

        setGraphic(hbox);
        setStyle("-fx-background-color: transparent;"); // Ensure cell container is transparent
      }
    }
  }
}
