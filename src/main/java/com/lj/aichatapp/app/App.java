package com.lj.aichatapp.app;

import com.lj.aichatapp.context.AppContext;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.utils.Logger;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage stage) {
        Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
            Logger.error("Uncaught exception in JavaFX thread", throwable);
            showFatalError(throwable);
        });

        try {
            Logger.logSystemInfo();

            AppContext ctx = AppContext.getInstance();
            UserPreferences prefs = ctx.getPreferences();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
            BorderPane root = loader.load();

            Scene scene = new Scene(root);

            scene.getStylesheets().add(getClass().getResource("/css/theme.css").toExternalForm());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            if ("dark".equalsIgnoreCase(prefs.getTheme())) {
                scene.getRoot().getStyleClass().add("dark");
            }

            stage.setTitle("iTutor");
            stage.setScene(scene);
            stage.setWidth(1000);
            stage.setHeight(650);
            stage.setMinWidth(700);
            stage.setMinHeight(500);

            com.lj.aichatapp.controllers.MainController controller = loader.getController();
            controller.setStage(stage);

            controller.postInit();

            stage.show();

            Logger.info("Application UI loaded successfully.");

        } catch (Exception e) {
            Logger.error("Failed to start application", e);
            showFatalError(e);
        }
    }

    private void showFatalError(Throwable t) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fatal Error");
            alert.setHeaderText("An unexpected error occurred");
            alert.setContentText("The application encountered a critical error and may need to restart.\n\nError: " + t.getMessage());
            alert.showAndWait();
        });
    }

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            Logger.error("Uncaught exception in thread " + thread.getName(), throwable);
        });

        launch(args);
    }
}
