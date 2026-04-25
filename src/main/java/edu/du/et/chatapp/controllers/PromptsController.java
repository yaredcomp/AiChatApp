/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.controllers;

import edu.du.et.chatapp.context.AppContext;
import edu.du.et.chatapp.models.Prompt;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.services.PromptService;
import edu.du.et.chatapp.utils.ModernAlert;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for the Prompt Library view.
 *
 * <p>Allows users to manage, search, and utilize reusable chat prompts.
 */
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
  @FXML
  private Button deleteBtn;

  // --- Icon Placeholders ---
  @FXML private Label iconLibrary;
  @FXML private Label iconPlus;
  @FXML private Label iconTrash;
  @FXML private Label iconUse;
  @FXML private Label iconSave;

  private Prompt currentPrompt;
  private Consumer<String> onUseCallback;
  private FilteredList<Prompt> filteredPrompts;
  private PromptService promptService;

  /**
   * Initializer called by FXMLLoader.
   */
  public void initialize() {
    this.promptService = AppContext.getInstance().getPromptService();

    initializeIcons();
    setupPromptList();
    setupSearchFilter();

    onNewPrompt(); // Start in "New Prompt" mode
  }

  /**
   * Programmatically sets up icons to avoid FXML LoadExceptions in Scene Builder.
   */
  private void initializeIcons() {
    iconLibrary.setGraphic(new FontIcon("fas-book-open:20"));
    iconPlus.setGraphic(new FontIcon("fas-plus:16"));
    iconTrash.setGraphic(new FontIcon("fas-trash-alt:16"));
    iconUse.setGraphic(new FontIcon("fas-paper-plane:16"));
    iconSave.setGraphic(new FontIcon("fas-save:16:white"));
  }

  /**
   * Configures the prompt list view and its cell factory.
   */
  private void setupPromptList() {
    refreshList();

    promptsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
      loadPrompt(newVal);
    });

    promptsList.setCellFactory(param -> new PromptListCell());
  }

  /**
   * Sets up the real-time search filtering.
   */
  private void setupSearchFilter() {
    searchField.textProperty().addListener((obs, oldVal, newVal) -> {
      if (filteredPrompts != null) {
        String query = (newVal == null) ? "" : newVal.toLowerCase();
        filteredPrompts.setPredicate(p -> {
          if (query.isEmpty()) {
            return true;
          }
          return p.getTitle().toLowerCase().contains(query)
              || p.getText().toLowerCase().contains(query);
        });
      }
    });
  }

  /**
   * Injects the callback for when a prompt is selected for use.
   */
  public void setOnUseCallback(Consumer<String> callback) {
    this.onUseCallback = callback;
  }

  /**
   * Applies the current theme and font settings to the view.
   */
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
      String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;",
          prefs.getFontFamily(), prefs.getFontSize());
      root.setStyle(style);
    }
  }

  /**
   * Reloads the prompts from the service.
   */
  private void refreshList() {
    List<Prompt> prompts = promptService.getAllPrompts();
    filteredPrompts = new FilteredList<>(FXCollections.observableArrayList(prompts), p -> true);
    promptsList.setItems(filteredPrompts);
  }

  /**
   * Loads a prompt into the editor fields.
   */
  private void loadPrompt(Prompt p) {
    currentPrompt = p;
    if (p != null) {
      titleField.setText(p.getTitle());
      contentArea.setText(p.getText());
      deleteBtn.setVisible(true);
      deleteBtn.setManaged(true);
    } else {
      onNewPrompt();
    }
  }

  /**
   * Resets the editor for a new prompt entry.
   */
  @FXML
  private void onNewPrompt() {
    currentPrompt = new Prompt();
    currentPrompt.setId(-1);

    titleField.clear();
    contentArea.clear();
    promptsList.getSelectionModel().clearSelection();

    deleteBtn.setVisible(false);
    deleteBtn.setManaged(false);

    titleField.requestFocus();
  }

  /**
   * Saves the current prompt to the repository.
   */
  @FXML
  private void onSave() {
    String title = titleField.getText();
    String text = contentArea.getText();

    if (title == null || title.isBlank() || text == null || text.isBlank()) {
      ModernAlert.info("Validation Error", "Please enter both a title and content.",
          root.getScene().getWindow());
      return;
    }

    if (currentPrompt == null) {
      currentPrompt = new Prompt();
      currentPrompt.setId(-1);
    }

    currentPrompt.setTitle(title);
    currentPrompt.setText(text);

    promptService.savePrompt(currentPrompt);
    refreshList();

    // Select the saved prompt
    for (Prompt p : promptsList.getItems()) {
      if (p.getTitle().equals(title) && p.getText().equals(text)) {
        promptsList.getSelectionModel().select(p);
        break;
      }
    }

    ModernAlert.info("Saved", "Prompt saved successfully.", root.getScene().getWindow());
  }

  /**
   * Passes the prompt content back to the main chat and closes the window.
   */
  @FXML
  private void onUse() {
    String text = contentArea.getText();
    if (text != null && !text.isBlank()) {
      if (onUseCallback != null) {
        onUseCallback.accept(text);
      }
      closeWindow();
    } else {
      ModernAlert.info("Empty Content", "Please select or write a prompt to use.",
          root.getScene().getWindow());
    }
  }

  /**
   * Event handler for the delete action.
   */
  @FXML
  private void onDelete() {
    if (currentPrompt != null && currentPrompt.getId() != -1) {
      onDeletePrompt(currentPrompt);
    }
  }

  private void onDeletePrompt(Prompt p) {
    ModernAlert.confirmDanger("Delete Prompt",
        "Are you sure you want to delete \"" + p.getTitle() + "\"?",
        root.getScene().getWindow(),
        confirmed -> {
          if (confirmed) {
            promptService.deletePrompt(p.getId());
            refreshList();
            onNewPrompt();
          }
        });
  }

  private void closeWindow() {
    Stage stage = (Stage) root.getScene().getWindow();
    stage.close();
  }

  /**
   * Custom ListCell for rendering Prompt items.
   */
  private class PromptListCell extends ListCell<Prompt> {
    @Override
    protected void updateItem(Prompt item, boolean empty) {
      super.updateItem(item, empty);
      if (empty || item == null) {
        setText(null);
        setGraphic(null);
        getStyleClass().remove("filled");
      } else {
        getStyleClass().add("filled");
        HBox box = new HBox(8);
        box.setAlignment(Pos.CENTER_LEFT);

        Label lbl = new Label(item.getTitle());
        lbl.setMaxWidth(180);
        lbl.setEllipsisString("...");
        lbl.getStyleClass().add("card-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button itemDelBtn = new Button();
        itemDelBtn.getStyleClass().add("window-button");
        itemDelBtn.setGraphic(new FontIcon("fas-trash:12"));
        itemDelBtn.setTooltip(new Tooltip("Delete"));

        itemDelBtn.setOnAction(e -> {
          e.consume();
          onDeletePrompt(item);
        });

        itemDelBtn.setVisible(false);
        box.setOnMouseEntered(e -> itemDelBtn.setVisible(true));
        box.setOnMouseExited(e -> itemDelBtn.setVisible(false));

        box.getChildren().addAll(lbl, spacer, itemDelBtn);
        setGraphic(box);
      }
    }
  }
}
