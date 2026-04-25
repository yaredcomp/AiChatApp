/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.utils;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Parent;

/**
 * Global manager for application themes.
 *
 * <p>Uses JavaFX properties to allow reactive UI updates when the theme changes.
 */
public class ThemeManager {

  private static final ThemeManager instance = new ThemeManager();

  // true = dark, false = light
  private final BooleanProperty darkMode = new SimpleBooleanProperty(false);

  private ThemeManager() {
    // Listen for property changes to apply global CSS changes
    darkMode.addListener((obs, oldVal, newVal) -> applyGlobalTheme(newVal));
  }

  public static ThemeManager getInstance() {
    return instance;
  }

  public BooleanProperty darkModeProperty() {
    return darkMode;
  }

  public boolean isDarkMode() {
    return darkMode.get();
  }

  public void setDarkMode(boolean dark) {
    darkMode.set(dark);
  }

  /**
   * Applies the theme to a specific root node (adds/removes .dark class).
   */
  public void applyThemeToParent(Parent root) {
    if (isDarkMode()) {
      if (!root.getStyleClass().contains("dark")) {
        root.getStyleClass().add("dark");
      }
    } else {
      root.getStyleClass().remove("dark");
    }
  }

  /**
   * Internal helper to switch AtlantaFX user agent stylesheets.
   */
  private void applyGlobalTheme(boolean dark) {
    if (dark) {
      Application.setUserAgentStylesheet(new PrimerDark().getUserAgentStylesheet());
    } else {
      Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
    }
  }
}
