package com.lj.aichatapp.app;

import com.lj.aichatapp.controllers.MainController;
import com.lj.aichatapp.models.UserPreferences;
import com.lj.aichatapp.utils.PreferencesManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;


/**
 * Main application entry point.
 */
public class App extends Application {


    @Override
    public void start(Stage stage) throws IOException {
        PreferencesManager.ensureAppDirectory();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/main.fxml"));
        Scene scene = new Scene(loader.load());

        // attach css
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("/css/theme.css")).toExternalForm());

        // apply saved theme
        UserPreferences prefs = PreferencesManager.loadPreferences();
        if (prefs != null && "dark".equalsIgnoreCase(prefs.getTheme())) {
            scene.getRoot().getStyleClass().add("dark");
        }

        stage.setTitle("JavaFX AI Chat");
        stage.setScene(scene);
        stage.setMinWidth(600);
        stage.setMinHeight(500);
        // optional app icon
        // stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app.png")));
        stage.show();

        // give controller an opportunity to init after stage
        MainController controller = loader.getController();
        controller.setStage(stage);
        controller.postInit();
    }

    public static void main(String[] args) {
        launch();
    }
}
