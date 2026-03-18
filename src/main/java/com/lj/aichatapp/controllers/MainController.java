package com.lj.aichatapp.controllers;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import com.lj.aichatapp.context.AppContext;
import com.lj.aichatapp.models.Conversation;
import com.lj.aichatapp.models.Message;
import com.lj.aichatapp.models.MessageRole;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.ChatService;
import com.lj.aichatapp.utils.FxUtils;
import com.lj.aichatapp.utils.Logger;
import com.lj.aichatapp.utils.ResponseFormatter;
import javafx.animation.KeyFrame;
import javafx.util.Duration;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import org.kordamp.ikonli.javafx.FontIcon;

public class MainController {

    @FXML
    public VBox messagesBox;
    @FXML
    public ScrollPane scrollPane;
    @FXML
    public TextArea inputField;
    @FXML
    public Button btnSend;
    @FXML
    public Label typingIndicator;
    @FXML
    public Button btnSettings;
    @FXML
    public Button btnClear;
    @FXML
    public Label currentModelIndicator;
    @FXML
    public TextField searchField;
    @FXML
    public Label charCount;
    @FXML
    public VBox emptyState;
    
    @FXML
    public VBox navPanel;
    @FXML
    public VBox navContent;
    @FXML
    public Button navToggle;
    @FXML
    public ListView<Conversation> chatHistoryList;
    
    @FXML
    private javafx.scene.layout.BorderPane root;
    @FXML
    private javafx.scene.layout.HBox titleBar;

    private Stage stage;
    private Conversation currentConversation;
    private ChatService chatService;
    private UserPreferences prefs;
    
    private CompletableFuture<String> currentRequest;
    private boolean isGenerating = false;
    private FilteredList<Conversation> filteredChats;
    private List<Conversation> allConversations;
    
    private boolean isNavExpanded = true;
    private double expandedWidth = 280;
    private double collapsedWidth = 60;

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void postInit() {
        AppContext ctx = AppContext.getInstance();
        this.chatService = ctx.getChatService();
        this.prefs = ctx.getPreferences();
        
        refreshHistory();
        
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldText, newText) -> {
                if (filteredChats != null) {
                    if (newText == null || newText.isEmpty()) {
                        filteredChats.setPredicate(conv -> true);
                    } else {
                        filteredChats.setPredicate(conv -> 
                            conv.getTitle().toLowerCase().contains(newText.toLowerCase()));
                    }
                }
            });
        }
        
        if (!chatHistoryList.getItems().isEmpty()) {
            loadConversation(chatHistoryList.getItems().get(0));
        } else {
            onNewChat();
        }

        messagesBox.heightProperty().addListener((obs, oldVal, newVal) -> {
            scrollPane.setVvalue(1.0);
        });

        applyFontSettings();
        updateModelIndicator();
        
        btnSend.disableProperty().bind(
            Bindings.createBooleanBinding(() -> {
                boolean hasText = !inputField.getText().trim().isEmpty();
                return !hasText && !isGenerating;
            }, inputField.textProperty())
        );
        
        chatHistoryList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && (currentConversation == null || newVal.getId() != currentConversation.getId())) {
                loadConversation(newVal);
            }
        });
        
        chatHistoryList.setCellFactory(param -> new ListCell<Conversation>() {
            @Override
            protected void updateItem(Conversation item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    VBox vbox = new VBox(2);
                    vbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    HBox topRow = new HBox();
                    topRow.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    Label titleLabel = new Label(item.getTitle());
                    titleLabel.setMaxWidth(140);
                    titleLabel.setEllipsisString("...");
                    titleLabel.setStyle("-fx-font-weight: 500;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Label dateLabel = new Label(getRelativeDate(item.getCreatedAt()));
                    dateLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-text-secondary;");
                    
                    topRow.getChildren().addAll(titleLabel, spacer, dateLabel);
                    
                    Label previewLabel = new Label(item.getPreview());
                    previewLabel.setMaxWidth(180);
                    previewLabel.setEllipsisString("...");
                    previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-text-secondary;");
                    
                    vbox.getChildren().addAll(topRow, previewLabel);
                    
                    Region spacerRight = new Region();
                    HBox.setHgrow(spacerRight, Priority.ALWAYS);
                    
                    Button delBtn = new Button();
                    delBtn.getStyleClass().add("icon-button");
                    FontIcon trashIcon = new FontIcon("fas-trash");
                    trashIcon.setIconSize(12);
                    trashIcon.setIconColor(javafx.scene.paint.Color.GRAY);
                    delBtn.setGraphic(trashIcon);
                    
                    delBtn.setOnAction(e -> {
                        e.consume();
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete chat '" + item.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
                        alert.initOwner(delBtn.getScene().getWindow());
                        alert.showAndWait();
                        
                        if (alert.getResult() == ButtonType.YES) {
                            chatService.deleteConversation(item.getId());
                            refreshHistory();
                            if (currentConversation != null && currentConversation.getId() == item.getId()) {
                                onNewChat();
                            }
                        }
                    });
                    
                    HBox hbox = new HBox(5);
                    hbox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    delBtn.setVisible(false);
                    delBtn.setManaged(false);
                    
                    hbox.setOnMouseEntered(e -> {
                        delBtn.setVisible(true);
                        delBtn.setManaged(true);
                    });
                    hbox.setOnMouseExited(e -> {
                        delBtn.setVisible(false);
                        delBtn.setManaged(false);
                    });
                    
                    hbox.getChildren().addAll(vbox, spacerRight, delBtn);
                    
                    setGraphic(hbox);
                }
            }
        });
        
        setupInputAutoResize();
    }
    
    private void setupInputAutoResize() {
        inputField.textProperty().addListener((obs, oldText, newText) -> {
            updateInputFieldHeight();
            if (charCount != null) {
                int count = newText != null ? newText.length() : 0;
                charCount.setText(count + " characters");
            }
        });
        inputField.widthProperty().addListener((obs, oldWidth, newWidth) -> updateInputFieldHeight());
    }

    private void updateInputFieldHeight() {
        Platform.runLater(() -> {
            final double MAX_HEIGHT = 150;
            String text = inputField.getText();

            if (text == null || text.trim().isEmpty()) {
                inputField.setPrefHeight(Region.USE_COMPUTED_SIZE);
                return;
            }

            Text helper = new Text(text);
            helper.setFont(inputField.getFont());
            
            double wrappingWidth = inputField.getWidth() - inputField.getPadding().getLeft() - inputField.getPadding().getRight();
            if (wrappingWidth <= 0) {
                if (inputField.getScene() != null && inputField.getScene().getWidth() > 0) {
                    wrappingWidth = inputField.getScene().getWidth() * 0.6; 
                } else {
                    return;
                }
            }
            helper.setWrappingWidth(wrappingWidth);

            double requiredHeight = helper.getLayoutBounds().getHeight() + inputField.getPadding().getTop() + inputField.getPadding().getBottom() + 10;

            inputField.setPrefHeight(Math.min(requiredHeight, MAX_HEIGHT));
        });
    }
    
    private void refreshHistory() {
        allConversations = chatService.getAllConversations();
        
        if (searchField != null && searchField.getText() != null && !searchField.getText().isEmpty()) {
            String searchTerm = searchField.getText().toLowerCase();
            filteredChats = new FilteredList<>(FXCollections.observableArrayList(allConversations), 
                conv -> conv.getTitle().toLowerCase().contains(searchTerm));
            chatHistoryList.setItems(filteredChats);
        } else {
            filteredChats = new FilteredList<>(FXCollections.observableArrayList(allConversations), 
                conv -> true);
            chatHistoryList.setItems(filteredChats);
        }
    }
    
    private void loadConversation(Conversation c) {
        currentConversation = c;
        chatService.loadConversationMessages(currentConversation);
        
        messagesBox.getChildren().clear();
        currentConversation.getMessages().forEach(this::addMessageToView);
        
        if (emptyState != null) {
            emptyState.setVisible(currentConversation.getMessages().isEmpty());
        }
    }

    private void applyFontSettings() {
        String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", prefs.getFontFamily(), prefs.getFontSize());
        root.setStyle(style);
    }
    
    private void updateModelIndicator() {
        if (prefs != null) {
            String provider = prefs.getProvider();
            String model = prefs.getModel();
            if (provider != null && model != null) {
                currentModelIndicator.setText(String.format("%s - %s", provider, model));
            } else {
                currentModelIndicator.setText("");
            }
        }
    }
    
    private String getRelativeDate(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        
        LocalDate today = LocalDate.now();
        LocalDate date = dateTime.toLocalDate();
        
        long daysBetween = ChronoUnit.DAYS.between(date, today);
        
        if (daysBetween == 0) {
            return "Today";
        } else if (daysBetween == 1) {
            return "Yesterday";
        } else if (daysBetween < 7) {
            return dateTime.format(DateTimeFormatter.ofPattern("EEEE"));
        } else {
            return dateTime.format(DateTimeFormatter.ofPattern("MMM d"));
        }
    }
    
    @FXML
    public void onNewChat() {
        currentConversation = new Conversation();
        currentConversation.setId(-1);
        messagesBox.getChildren().clear();
        chatHistoryList.getSelectionModel().clearSelection();
        if (emptyState != null) {
            emptyState.setVisible(true);
        }
    }
    
    @FXML
    public void onToggleNav() {
        isNavExpanded = !isNavExpanded;
        
        double targetWidth = isNavExpanded ? expandedWidth : collapsedWidth;
        
        if (navPanel != null && navContent != null) {
            navPanel.setPrefWidth(targetWidth);
            navContent.setVisible(isNavExpanded);
            navContent.setManaged(isNavExpanded);
            
            if (searchField != null) {
                searchField.setVisible(isNavExpanded);
                searchField.setManaged(isNavExpanded);
            }
            
            // Update toggle button icon
            if (navToggle != null) {
                FontIcon icon = new FontIcon(isNavExpanded ? "fas-chevron-left" : "fas-chevron-right");
                icon.setIconSize(14);
                navToggle.setGraphic(icon);
            }
        }
    }

    @FXML
    public void onSendMessage() {
        if (isGenerating) {
            if (currentRequest != null) {
                currentRequest.cancel(true);
            }
            return;
        }

        String text = inputField.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        
        if (emptyState != null) {
            emptyState.setVisible(false);
        }
        
        inputField.clear();
        if (charCount != null) {
            charCount.setText("");
        }
        
        if (currentConversation.getId() == -1) {
            currentConversation = chatService.saveOrGetCurrentConversation(currentConversation, text);
            refreshHistory();
            chatHistoryList.getSelectionModel().select(currentConversation);
        }

        Message userMsg = new Message(MessageRole.USER, text);
        currentConversation.addMessage(userMsg);
        addMessageToView(userMsg);

        setTyping(true);

        Message aiMsg = new Message(MessageRole.ASSISTANT, "");
        
        Node initialBubble = FxUtils.makeStreamingBubble("", "message-ai");
        messagesBox.getChildren().add(initialBubble);
        FxUtils.fadeIn(initialBubble);
        
        AtomicReference<Node> currentBubbleRef = new AtomicReference<>(initialBubble);
        StringBuilder currentResponse = new StringBuilder();
        
        final javafx.animation.Timeline throttleTimeline = new javafx.animation.Timeline();
        throttleTimeline.setCycleCount(javafx.animation.Timeline.INDEFINITE);
        KeyFrame kf = new KeyFrame(
            Duration.millis(150),
            event -> {
                String textToRender = currentResponse.toString();
                Node currentBubble = currentBubbleRef.get();
                int index = messagesBox.getChildren().indexOf(currentBubble);
                
                if (index != -1 && !textToRender.isEmpty()) {
                    Node newBubble = FxUtils.makeStreamingBubble(textToRender, "message-ai");
                    messagesBox.getChildren().set(index, newBubble);
                    currentBubbleRef.set(newBubble);
                    
                    scrollToBottom();
                }
            }
        );
        throttleTimeline.getKeyFrames().add(kf);

        currentRequest = chatService.sendMessage(currentConversation, text, chunk -> {
            Platform.runLater(() -> {
                currentResponse.append(chunk);
            });
        });
        
        Platform.runLater(() -> throttleTimeline.playFromStart());
        
        currentRequest.whenComplete((resp, err) -> {
            Platform.runLater(() -> {
                throttleTimeline.stop();
                setTyping(false);
                
                String finalResponse = currentResponse.toString();
                
                if (err != null) {
                    if (err instanceof java.util.concurrent.CancellationException) {
                        chatService.saveAiResponse(currentConversation, finalResponse);
                    } else {
                        handleAiError(err);
                    }
                } else {
                    ResponseFormatter.ResponseType type = ResponseFormatter.detectType(finalResponse);
                    Node formattedNode = ResponseFormatter.format(finalResponse, type);
                    Node bubble = FxUtils.makeBubbleFromNode(finalResponse, "message-ai", formattedNode);
                    
                    int index = messagesBox.getChildren().indexOf(currentBubbleRef.get());
                    if (index != -1) {
                        messagesBox.getChildren().set(index, bubble);
                        FxUtils.fadeIn(bubble);
                    }
                    
                    chatService.saveAiResponse(currentConversation, resp);
                    scrollToBottom();
                }
                currentRequest = null;
            });
        });
    }

    private void setTyping(boolean on) {
        isGenerating = on;
        Platform.runLater(() -> {
            typingIndicator.setText(on ? "iTutor is responding..." : "");
            
            if (on) {
                btnSend.getStyleClass().add("loading");
            } else {
                btnSend.getStyleClass().remove("loading");
            }
            
            FontIcon icon = new FontIcon(on ? "fas-stop" : "fas-paper-plane");
            icon.setIconSize(16);
            icon.setIconColor(javafx.scene.paint.Color.WHITE);
            btnSend.setGraphic(icon);
            btnSend.setText("");
        });
    }
    
    private void scrollToBottom() {
        if (scrollPane != null) {
            scrollPane.setVvalue(1.0);
        }
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
        
        Platform.runLater(() -> {
            scrollPane.setVvalue(1.0);
        });
        
        FxUtils.fadeIn(bubbleNode);
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
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            if (root.getScene().getRoot().getStyleClass().contains("dark")) {
                scene.getRoot().getStyleClass().add("dark");
            }

            settingsStage.setScene(scene);
            
            SettingsController ctrl = loader.getController();
            ctrl.initWithPreferences(prefs);

            settingsStage.showAndWait();

            prefs = AppContext.getInstance().getPreferences();
            chatService = AppContext.getInstance().getChatService();
            applyFontSettings();
            applyTheme();
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
            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
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
            showError("Error opening prompts", e);
        }
    }

    @FXML
    public void onClearConversation() {
        chatService.clearConversation(currentConversation);
        messagesBox.getChildren().clear();
    }

    @FXML
    private void handleInputKeyPressed(KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.ENTER && !event.isShiftDown() && !event.isControlDown()) {
            onSendMessage();
            event.consume();
        }
    }
    
    public void handleGlobalKeyEvent(javafx.scene.input.KeyEvent event) {
        if (event.getCode() == javafx.scene.input.KeyCode.N && event.isControlDown()) {
            onNewChat();
            event.consume();
        }
    }

    private void handleAiError(Throwable e) {
        Throwable cause = e;
        if (e instanceof CompletionException && e.getCause() != null) {
            cause = e.getCause();
        }
        
        String msg = cause.getMessage();
        if (msg == null) msg = "";
        
        // Enhance message detection logic
        if (msg.contains("429") || msg.toLowerCase().contains("limit")) {
            showFriendlyError("Usage Limit Reached", 
                "You have reached your usage limit with the AI provider.\n\n" +
                "Please check your API plan or try switching to a different model/provider in Settings.", cause);
        } else if (msg.contains("401") || msg.toLowerCase().contains("unauthorized") || msg.toLowerCase().contains("api key")) {
            showFriendlyError("Authentication Error", 
                "There was an issue with your API key.\n\n" +
                "Please check your API key in Settings > Providers.", cause);
        } else if (msg.contains("Connection refused") || msg.contains("ConnectException")) {
            showFriendlyError("Connection Error", 
                "Could not connect to the AI provider.\n\n" +
                "Please check your internet connection. If using Ollama, ensure it is running locally.", cause);
        } else {
            showError("AI Error", cause);
        }
    }

    private void showFriendlyError(String title, String content, Throwable e) {
        Logger.error(title, e);
        
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("AI Service Warning");
        alert.setHeaderText(title);
        alert.setContentText(content);
        
        // Add a "Settings" button to the alert
        ButtonType settingsBtn = new ButtonType("Open Settings");
        alert.getButtonTypes().add(settingsBtn);
        
        alert.initOwner(stage);
        
        alert.showAndWait().ifPresent(btn -> {
            if (btn == settingsBtn) {
                onOpenSettings();
            }
        });
    }

    private void showError(String title, Throwable e) {
        System.err.println("ERROR: " + title);
        e.printStackTrace(System.err);
        Logger.error(title, e);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(title);
        alert.setContentText("An unexpected error occurred.");

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String exceptionText = sw.toString();

        Label label = new Label("The exception stacktrace was:");

        TextArea textArea = new TextArea(exceptionText);
        textArea.setEditable(false);
        textArea.setWrapText(true);

        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea, 0, 1);

        alert.getDialogPane().setExpandableContent(expContent);

        alert.initOwner(stage);
        alert.showAndWait();
    }

    private void applyTheme() {
        if ("dark".equalsIgnoreCase(prefs.getTheme())) {
            if (!root.getScene().getRoot().getStyleClass().contains("dark")) {
                root.getScene().getRoot().getStyleClass().add("dark");
            }
        } else {
            root.getScene().getRoot().getStyleClass().remove("dark");
        }
    }
}