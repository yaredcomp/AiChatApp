package com.lj.aichatapp.utils;

import com.lj.aichatapp.context.AppContext;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.function.Consumer;

public class ModernAlert {

    public static void info(String title, String content, Window owner) {
        show(title, content, "fas-info-circle", "dialog-icon-info", owner, false, false, null);
    }

    public static void error(String title, String content, Window owner) {
        show(title, content, "fas-exclamation-triangle", "dialog-icon-error", owner, false, false, null);
    }

    public static void confirm(String title, String content, Window owner, Consumer<Boolean> onResult) {
        show(title, content, "fas-question-circle", "dialog-icon-confirm", owner, true, false, onResult);
    }
    
    public static void confirmDanger(String title, String content, Window owner, Consumer<Boolean> onResult) {
        show(title, content, "fas-exclamation-circle", "dialog-icon-error", owner, true, true, onResult);
    }

    private static void show(String title, String content, String iconLiteral, String iconClass, Window owner, boolean isConfirmation, boolean isDanger, Consumer<Boolean> onResult) {
        Stage stage = createStage(owner);
        stage.setTitle(title);

        VBox root = new VBox(16);
        root.getStyleClass().add("modern-dialog");
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);

        // Header
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(24);
        icon.getStyleClass().add(iconClass);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        
        headerBox.getChildren().addAll(icon, titleLabel);

        // Content
        Text contentText = new Text(content);
        contentText.getStyleClass().add("dialog-content");
        contentText.setWrappingWidth(350);
        TextFlow textFlow = new TextFlow(contentText);

        // Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        if (isConfirmation) {
            Button cancelBtn = new Button("Cancel");
            cancelBtn.getStyleClass().add("cancel-button");
            cancelBtn.setOnAction(e -> {
                stage.close();
                if (onResult != null) onResult.accept(false);
            });
            
            Button confirmBtn = new Button("Confirm");
            confirmBtn.getStyleClass().add(isDanger ? "danger-button" : "save-button");
            confirmBtn.setOnAction(e -> {
                stage.close();
                if (onResult != null) onResult.accept(true);
            });
            
            buttonBox.getChildren().addAll(cancelBtn, confirmBtn);
        } else {
            Button okBtn = new Button("OK");
            okBtn.getStyleClass().add("action-button");
            okBtn.setOnAction(e -> stage.close());
            buttonBox.getChildren().add(okBtn);
        }

        root.getChildren().addAll(headerBox, textFlow, new Region(), buttonBox);
        VBox.setVgrow(textFlow, Priority.ALWAYS);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().set(2, spacer);

        Scene scene = new Scene(root);
        applyTheme(scene);

        // Manual fix for dark mode text visibility
        if ("dark".equalsIgnoreCase(AppContext.getInstance().getPreferences().getTheme())) {
             contentText.setStyle("-fx-fill: white;");
             titleLabel.setStyle("-fx-text-fill: white;");
        }

        stage.setScene(scene);
        stage.showAndWait();
    }
    
    public static void showProgress(String title, String message, Task<?> task, Window owner) {
        Stage stage = createStage(owner);
        stage.setTitle(title);
        
        VBox root = new VBox(16);
        root.getStyleClass().add("modern-dialog");
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPrefWidth(400);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("dialog-title");
        
        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("dialog-content-label"); 
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.progressProperty().bind(task.progressProperty());
        
        root.getChildren().addAll(titleLabel, msgLabel, progressBar);
        
        Scene scene = new Scene(root);
        applyTheme(scene);

        // Manual fix for dark mode text visibility
        if ("dark".equalsIgnoreCase(AppContext.getInstance().getPreferences().getTheme())) {
             titleLabel.setStyle("-fx-text-fill: white;");
             msgLabel.setStyle("-fx-text-fill: white;");
        }

        stage.setScene(scene);
        
        task.setOnSucceeded(e -> stage.close());
        task.setOnFailed(e -> stage.close());
        
        new Thread(task).start();
        stage.show();
    }
    
    public static void askInput(String title, String header, String defaultValue, Window owner, Consumer<String> onResult) {
        Stage stage = createStage(owner);
        stage.setTitle(title);

        VBox root = new VBox(16);
        root.getStyleClass().add("modern-dialog");
        root.setPadding(new Insets(24));
        root.setAlignment(Pos.CENTER_LEFT);
        root.setPrefWidth(400);

        // Header
        Label titleLabel = new Label(header);
        titleLabel.getStyleClass().add("dialog-title");

        // Input
        TextField inputField = new TextField(defaultValue);
        inputField.getStyleClass().add("modern-text-field");
        inputField.setPromptText("Enter value...");

        // Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-button");
        cancelBtn.setOnAction(e -> {
            stage.close();
        });
        
        Button okBtn = new Button("OK");
        okBtn.getStyleClass().add("save-button");
        okBtn.setDefaultButton(true);
        okBtn.setOnAction(e -> {
            stage.close();
            if (onResult != null) onResult.accept(inputField.getText());
        });
        
        buttonBox.getChildren().addAll(cancelBtn, okBtn);

        root.getChildren().addAll(titleLabel, inputField, new Region(), buttonBox);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().set(2, spacer);

        Scene scene = new Scene(root);
        applyTheme(scene);

        // Manual fix for dark mode text visibility
        if ("dark".equalsIgnoreCase(AppContext.getInstance().getPreferences().getTheme())) {
             titleLabel.setStyle("-fx-text-fill: white;");
        }

        stage.setScene(scene);
        
        stage.setOnShown(e -> inputField.requestFocus());
        
        stage.showAndWait();
    }

    private static Stage createStage(Window owner) {
        Stage stage = new Stage();
        stage.initOwner(owner);
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        return stage;
    }
    
    private static void applyTheme(Scene scene) {
        URL styleCss = ModernAlert.class.getResource("/css/style.css");
        URL themeCss = ModernAlert.class.getResource("/css/theme.css");
        
        if (styleCss != null) scene.getStylesheets().add(styleCss.toExternalForm());
        if (themeCss != null) scene.getStylesheets().add(themeCss.toExternalForm());
        
        // Check current theme
        if ("dark".equalsIgnoreCase(AppContext.getInstance().getPreferences().getTheme())) {
            scene.getRoot().getStyleClass().add("dark");
        }
        
        // Apply font settings
        String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", 
            AppContext.getInstance().getPreferences().getFontFamily(), 
            AppContext.getInstance().getPreferences().getFontSize());
        scene.getRoot().setStyle(style);
    }
}
