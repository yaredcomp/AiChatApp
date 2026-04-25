package edu.du.et.chatapp.utils;

import edu.du.et.chatapp.context.AppContext;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.function.Consumer;

public class ModernAlert {

    public static void info(String title, String content, Window owner) {
        show(title, content, "fas-info-circle", "text-accent", owner, false, false, null);
    }

    public static void error(String title, String content, Window owner) {
        show(title, content, "fas-exclamation-triangle", "danger-button", owner, false, false, null);
    }

    public static void confirm(String title, String content, Window owner, Consumer<Boolean> onResult) {
        show(title, content, "fas-question-circle", "text-accent", owner, true, false, onResult);
    }
    
    public static void confirmDanger(String title, String content, Window owner, Consumer<Boolean> onResult) {
        show(title, content, "fas-exclamation-circle", "danger-button", owner, true, true, onResult);
    }

    private static void show(String title, String content, String iconLiteral, String iconClass, Window owner, boolean isConfirmation, boolean isDanger, Consumer<Boolean> onResult) {
        Stage stage = createStage(title, owner);

        VBox root = new VBox(16);
        root.getStyleClass().add("settings-card"); // Use card style for dialog
        root.setPadding(new Insets(24));
        root.setPrefWidth(400);

        // Header
        HBox headerBox = new HBox(12);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(24);
        if (isDanger) {
             icon.setIconColor(javafx.scene.paint.Color.RED); // Fallback if class doesn't handle it
             icon.getStyleClass().add("danger-button"); // Reuse danger style for color
        } else {
             icon.getStyleClass().add("app-icon");
        }
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-section-title");
        
        headerBox.getChildren().addAll(icon, titleLabel);

        // Content
        Label contentLabel = new Label(content);
        contentLabel.getStyleClass().add("card-description");
        contentLabel.setWrapText(true);
        // contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-text;"); // REMOVED: Rely on CSS for text-fill

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
            
            Button confirmBtn = new Button(isDanger ? "Delete" : "Confirm");
            confirmBtn.getStyleClass().add(isDanger ? "danger-button" : "save-button");
            confirmBtn.setOnAction(e -> {
                stage.close();
                if (onResult != null) onResult.accept(true);
            });
            
            buttonBox.getChildren().addAll(cancelBtn, confirmBtn);
        } else {
            Button okBtn = new Button("OK");
            okBtn.getStyleClass().add("save-button");
            okBtn.setOnAction(e -> stage.close());
            buttonBox.getChildren().add(okBtn);
        }

        root.getChildren().addAll(headerBox, contentLabel, new Region(), buttonBox);
        VBox.setVgrow(contentLabel, Priority.ALWAYS);
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        root.getChildren().set(2, spacer);

        Scene scene = new Scene(root);
        applyTheme(scene);
        stage.setScene(scene);
        stage.showAndWait();
    }
    
    public static void showProgress(String title, String message, Task<?> task, Window owner) {
        Stage stage = createStage(title, owner);
        
        VBox root = new VBox(16);
        root.getStyleClass().add("settings-card");
        root.setPadding(new Insets(24));
        root.setPrefWidth(400);
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("settings-section-title");
        
        Label msgLabel = new Label(message);
        msgLabel.getStyleClass().add("card-label"); 
        
        ProgressBar progressBar = new ProgressBar();
        progressBar.setMaxWidth(Double.MAX_VALUE);
        progressBar.progressProperty().bind(task.progressProperty());
        
        root.getChildren().addAll(titleLabel, msgLabel, progressBar);
        
        Scene scene = new Scene(root);
        applyTheme(scene);
        stage.setScene(scene);
        
        task.setOnSucceeded(e -> stage.close());
        task.setOnFailed(e -> stage.close());
        
        new Thread(task).start();
        stage.show();
    }
    
    public static void askInput(String title, String header, String defaultValue, Window owner, Consumer<String> onResult) {
        Stage stage = createStage(title, owner);

        VBox root = new VBox(16);
        root.getStyleClass().add("settings-card");
        root.setPadding(new Insets(24));
        root.setPrefWidth(400);

        // Header
        Label titleLabel = new Label(header);
        titleLabel.getStyleClass().add("settings-section-title");

        // Input
        TextField inputField = new TextField(defaultValue);
        inputField.getStyleClass().add("modern-text-field");
        inputField.setPromptText("Enter value...");

        // Buttons
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("cancel-button");
        cancelBtn.setOnAction(e -> stage.close());
        
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
        
        stage.setOnShown(e -> inputField.requestFocus());
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static Stage createStage(String title, Window owner) {
        Stage stage = new Stage();
        stage.setTitle(title);
        if (owner != null) {
            stage.initOwner(owner);
        }
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        return stage;
    }
    
    private static void applyTheme(Scene scene) {
        URL styleCss = ModernAlert.class.getResource("/css/style.css");
        URL themeCss = ModernAlert.class.getResource("/css/theme.css"); // This might be deprecated with AtlantaFX
        
        if (styleCss != null) scene.getStylesheets().add(styleCss.toExternalForm());
        // If theme.css is still used, add it, otherwise it's handled by AtlantaFX
        if (themeCss != null) scene.getStylesheets().add(themeCss.toExternalForm());
        
        // Apply AtlantaFX theme if available
        if (AppContext.getInstance().getPreferences() != null) {
            boolean isDark = "dark".equalsIgnoreCase(AppContext.getInstance().getPreferences().getTheme());
            try {
                if (isDark) {
                    javafx.application.Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerDark().getUserAgentStylesheet());
                } else {
                    javafx.application.Application.setUserAgentStylesheet(new atlantafx.base.theme.PrimerLight().getUserAgentStylesheet());
                }
            } catch (NoClassDefFoundError e) {
                // AtlantaFX not in classpath or other issue, fallback to basic theme application
                Logger.warn("AtlantaFX theme could not be applied in ModernAlert: " + e.getMessage());
            } catch (Exception e) {
                Logger.error("Error applying AtlantaFX theme in ModernAlert", e);
            }

            // Apply font settings
            String style = String.format("-fx-font-family: '%s'; -fx-font-size: %dpx;", 
                AppContext.getInstance().getPreferences().getFontFamily(), 
                AppContext.getInstance().getPreferences().getFontSize());
            scene.getRoot().setStyle(style);
        }
    }
}
