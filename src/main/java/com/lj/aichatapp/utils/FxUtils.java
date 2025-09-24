package com.lj.aichatapp.utils;

import javafx.animation.FadeTransition;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

/**
 * Small helper utilities for UI effects etc.
 */
public class FxUtils {

    public static void fadeIn(Node node) {
        node.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(220), node);
        ft.setToValue(1.0);
        ft.play();
    }

    public static HBox makeBubble(String text, String styleClass) {
        HBox box = new HBox();
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.getStyleClass().addAll("message-bubble", styleClass);
        box.getChildren().add(lbl);
        return box;
    }
}
