package com.lj.aichatapp.utils;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class StreamingTextPane {

    private final VBox container;
    private final StringBuilder textBuffer = new StringBuilder();
    private final ObjectProperty<Node> currentBubble = new SimpleObjectProperty<>();
    private final AtomicBoolean isStreaming = new AtomicBoolean(false);
    
    private Timeline updateTimeline;
    private static final int THROTTLE_MS = 150;
    private static final int DEBOUNCE_MS = 300;
    
    private Consumer<String> onCompleteCallback;
    private String fullText = "";

    public StreamingTextPane(VBox container) {
        this.container = container;
        setupTimeline();
    }

    private void setupTimeline() {
        updateTimeline = new Timeline(
            new KeyFrame(Duration.millis(THROTTLE_MS), event -> {
                synchronized (textBuffer) {
                    if (textBuffer.length() > 0) {
                        String text = textBuffer.toString();
                        updateStreamingView(text);
                    }
                }
            })
        );
        updateTimeline.setCycleCount(Timeline.INDEFINITE);
    }

    public void startStreaming() {
        isStreaming.set(true);
        textBuffer.setLength(0);
        fullText = "";
        
        if (container.getChildren().isEmpty()) {
            addInitialBubble();
        }
        
        updateTimeline.playFromStart();
    }

    public void appendChunk(String chunk) {
        if (!isStreaming.get()) return;
        
        synchronized (textBuffer) {
            textBuffer.append(chunk);
            fullText += chunk;
        }
    }

    public void finishStreaming() {
        isStreaming.set(false);
        updateTimeline.stop();
        
        synchronized (textBuffer) {
            if (textBuffer.length() > 0) {
                updateStreamingView(textBuffer.toString());
            }
        }
        
        Platform.runLater(() -> {
            if (onCompleteCallback != null) {
                onCompleteCallback.accept(fullText);
            }
        });
    }

    public void cancelStreaming() {
        isStreaming.set(false);
        updateTimeline.stop();
    }

    private void addInitialBubble() {
        Node bubble = FxUtils.makeBubble("", "message-ai");
        container.getChildren().add(bubble);
        currentBubble.set(bubble);
        FxUtils.fadeIn(bubble);
    }

    private void updateStreamingView(String text) {
        if (container.getChildren().isEmpty()) {
            addInitialBubble();
        }

        Node current = currentBubble.get();
        if (current == null) return;

        int index = container.getChildren().indexOf(current);
        if (index == -1) {
            index = container.getChildren().size() - 1;
        }

        Node newBubble = FxUtils.makeStreamingBubble(text, "message-ai");
        container.getChildren().set(index, newBubble);
        currentBubble.set(newBubble);
    }

    public void setOnComplete(Consumer<String> callback) {
        this.onCompleteCallback = callback;
    }

    public void replaceWithFormatted(String formattedText) {
        if (container.getChildren().isEmpty()) return;

        Node current = currentBubble.get();
        if (current == null) return;

        int index = container.getChildren().indexOf(current);
        if (index == -1) {
            index = container.getChildren().size() - 1;
        }

        ResponseFormatter.ResponseType type = ResponseFormatter.detectType(formattedText);
        Node formattedNode = ResponseFormatter.format(formattedText, type);

        Node bubble = FxUtils.makeBubbleFromNode(formattedText, "message-ai", formattedNode);
        container.getChildren().set(index, bubble);
        currentBubble.set(bubble);
        
        FxUtils.fadeIn(bubble);
    }

    public boolean isStreaming() {
        return isStreaming.get();
    }

    public String getFullText() {
        return fullText;
    }

    public void cleanup() {
        updateTimeline.stop();
        textBuffer.setLength(0);
    }
}