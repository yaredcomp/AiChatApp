/*
 * Copyright (c) 2026 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.app;

import edu.du.et.chatapp.context.AppContext;
import edu.du.et.chatapp.controllers.MainController;
import edu.du.et.chatapp.models.UserPreferences;
import edu.du.et.chatapp.utils.Logger;
import edu.du.et.chatapp.utils.ThemeManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.net.URL;

/**
 * Main application entry point for Academia ChatAI.
 */
public class App extends Application {

  private static final double DEFAULT_WIDTH = 1000;
  private static final double DEFAULT_HEIGHT = 650;
  private static final double MIN_WIDTH = 700;
  private static final double MIN_HEIGHT = 500;

  private MainController mainController;

  @Override
  public void init() {
      // Force Ikonli to register the font pack. This helps in packaged EXEs.
      try {
          // Touching FontIcon with a literal ensures the font provider is loaded.
          FontIcon.of(FontAwesomeSolid.COG);
      } catch (Exception ignored) { }
  }

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

      // Initialize ThemeManager before loading UI
      ThemeManager themeManager = ThemeManager.getInstance();
      themeManager.setDarkMode("dark".equalsIgnoreCase(prefs.getTheme()));

      // Use a more robust resource loading method for packaged apps
      URL fxmlLocation = App.class.getResource("/views/main.fxml");
      if (fxmlLocation == null) {
          throw new IOException("Cannot find resource: /views/main.fxml");
      }
      
      FXMLLoader loader = new FXMLLoader(fxmlLocation);
      BorderPane root = loader.load();

      // Ensure the root node has the correct initial style class
      themeManager.applyThemeToParent(root);
      
      // Bind root style class to theme changes reactively
      themeManager.darkModeProperty().addListener((obs, oldVal, newVal) -> {
          themeManager.applyThemeToParent(root);
          if (mainController != null) {
              mainController.syncThemeToWebView();
          }
      });

      Scene scene = new Scene(root);
      applyStylesheets(scene);

      setupStageBranding(stage);

      stage.setScene(scene);
      stage.setWidth(DEFAULT_WIDTH);
      stage.setHeight(DEFAULT_HEIGHT);
      stage.setMinWidth(MIN_WIDTH);
      stage.setMinHeight(MIN_HEIGHT);

      mainController = loader.getController();
      if (mainController != null) {
          mainController.setStage(stage);
          mainController.postInit();
      }

      stage.show();
      Logger.info("Application UI loaded successfully.");

    } catch (IOException e) {
      Logger.error("Failed to start application due to I/O error", e);
      showFatalError(e);
    } catch (Exception e) {
      Logger.error("Unexpected error during application startup", e);
      showFatalError(e);
    }
  }

  private void applyStylesheets(Scene scene) {
    URL themeCss = App.class.getResource("/css/theme.css");
    URL styleCss = App.class.getResource("/css/style.css");

    if (themeCss != null) {
      scene.getStylesheets().add(themeCss.toExternalForm());
    }
    if (styleCss != null) {
      scene.getStylesheets().add(styleCss.toExternalForm());
    }
  }

  private void setupStageBranding(Stage stage) {
    stage.setTitle("DU Academia ChatAI");
    URL logoUrl = App.class.getResource("/image/chattAI Logo.png");
    if (logoUrl != null) {
      stage.getIcons().add(new Image(logoUrl.toExternalForm()));
    }
  }

  private void showFatalError(Throwable t) {
    Platform.runLater(() -> {
      Alert alert = new Alert(Alert.AlertType.ERROR);
      alert.setTitle("Fatal Error");
      alert.setHeaderText("An unexpected error occurred");
      
      String details = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
      if (t.getCause() != null) {
          details += "\nCause: " + t.getCause().getMessage();
      }
      
      alert.setContentText("The application encountered a critical error and may need to restart."
          + "\n\nError: " + details);
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
