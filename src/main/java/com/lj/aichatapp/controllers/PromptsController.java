package com.lj.aichatapp.controllers;

import com.lj.aichatapp.context.AppContext;
import com.lj.aichatapp.models.Prompt;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.service.PromptService;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;

public class PromptsController {

    @FXML
    private VBox root;
    @FXML
    private ListView<Prompt> promptsList;
    @FXML
    private TextField searchField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea contentArea;

    private Prompt currentPrompt;
    private Consumer<String> onUseCallback;
    private FilteredList<Prompt> filteredPrompts;
    private PromptService promptService;

    @FXML
    public void initialize() {
        this.promptService = AppContext.getInstance().getPromptService();
        refreshList();
        
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (filteredPrompts != null) {
                filteredPrompts.setPredicate(p -> {
                    if (newVal == null || newVal.isEmpty()) return true;
                    String lowerVal = newVal.toLowerCase();
                    return p.getTitle().toLowerCase().contains(lowerVal) || 
                           p.getText().toLowerCase().contains(lowerVal);
                });
            }
        });
        
        promptsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadPrompt(newVal);
            }
        });
        
        onNewPrompt();
        
        promptsList.setCellFactory(param -> new ListCell<Prompt>() {
            @Override
            protected void updateItem(Prompt item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox box = new HBox(5);
                    box.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    
                    Label lbl = new Label(item.getTitle());
                    lbl.setMaxWidth(140);
                    lbl.setEllipsisString("...");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    Button delBtn = new Button();
                    delBtn.getStyleClass().add("icon-button");
                    FontIcon trashIcon = new FontIcon("fas-trash");
                    trashIcon.setIconSize(12);
                    trashIcon.setIconColor(javafx.scene.paint.Color.GRAY);
                    delBtn.setGraphic(trashIcon);
                    
                    delBtn.setOnAction(e -> {
                        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Delete prompt '" + item.getTitle() + "'?", ButtonType.YES, ButtonType.NO);
                        alert.initOwner(root.getScene().getWindow());
                        alert.showAndWait();
                        
                        if (alert.getResult() == ButtonType.YES) {
                            promptService.deletePrompt(item.getId());
                            refreshList();
                            if (currentPrompt != null && currentPrompt.getId() == item.getId()) {
                                onNewPrompt();
                            }
                        }
                    });
                    
                    delBtn.setVisible(false);
                    box.setOnMouseEntered(e -> delBtn.setVisible(true));
                    box.setOnMouseExited(e -> delBtn.setVisible(false));
                    
                    box.getChildren().addAll(lbl, spacer, delBtn);
                    setGraphic(box);
                }
            }
        });
        
        applyFontSettings();
    }
    
    public void setOnUseCallback(Consumer<String> callback) {
        this.onUseCallback = callback;
    }
    
    public void applyTheme(boolean isDark) {
        if (root.getScene() != null) {
            if (isDark) {
                if (!root.getScene().getRoot().getStyleClass().contains("dark")) {
                    root.getScene().getRoot().getStyleClass().add("dark");
                }
            } else {
                root.getScene().getRoot().getStyleClass().remove("dark");
            }
        }
        applyFontSettings();
    }
    
    private void applyFontSettings() {
        UserPreferences prefs = AppContext.getInstance().getPreferences();
        if (prefs != null) {
            String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", prefs.getFontFamily(), prefs.getFontSize());
            root.setStyle(style);
        }
    }

    private void refreshList() {
        List<Prompt> prompts = promptService.getAllPrompts();
        filteredPrompts = new FilteredList<>(javafx.collections.FXCollections.observableArrayList(prompts), p -> true);
        promptsList.setItems(filteredPrompts);
    }

    private void loadPrompt(Prompt p) {
        currentPrompt = p;
        titleField.setText(p.getTitle());
        contentArea.setText(p.getText());
    }

    @FXML
    private void onNewPrompt() {
        currentPrompt = new Prompt();
        currentPrompt.setId(-1);
        titleField.clear();
        contentArea.clear();
        promptsList.getSelectionModel().clearSelection();
        titleField.requestFocus();
    }

    @FXML
    private void onSave() {
        String title = titleField.getText();
        String text = contentArea.getText();
        
        if (title == null || title.isBlank() || text == null || text.isBlank()) {
            Alert alert = new Alert(Alert.AlertType.WARNING, "Please enter both a title and content.", ButtonType.OK);
            alert.initOwner(root.getScene().getWindow());
            alert.showAndWait();
            return;
        }
        
        currentPrompt.setTitle(title);
        currentPrompt.setText(text);
        
        promptService.savePrompt(currentPrompt);
        refreshList();
        
        for (Prompt p : promptsList.getItems()) {
            if (p.getTitle().equals(title)) {
                promptsList.getSelectionModel().select(p);
                break;
            }
        }
    }

    @FXML
    private void onUse() {
        String text = contentArea.getText();
        if (text != null && !text.isBlank()) {
            if (onUseCallback != null) {
                onUseCallback.accept(text);
            }
            closeWindow();
        }
    }
    
    @FXML
    private void onDelete() {
        Prompt selected = promptsList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, 
            "Are you sure you want to delete \"" + selected.getTitle() + "\"?", 
            ButtonType.YES, ButtonType.NO);
        confirm.initOwner(root.getScene().getWindow());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.YES) {
                promptService.deletePrompt(selected.getId());
                refreshList();
                titleField.clear();
                contentArea.clear();
                currentPrompt = null;
            }
        });
    }
    
    private void closeWindow() {
        Stage stage = (Stage) root.getScene().getWindow();
        stage.close();
    }
}