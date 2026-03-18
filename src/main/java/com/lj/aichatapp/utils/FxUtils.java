package com.lj.aichatapp.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public static Node makeBubble(String text, String styleClass) {
        boolean isUser = styleClass.contains("message-user");
        
        VBox messageContainer = new VBox(4);
        messageContainer.getStyleClass().add("message-row");
        
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon avatar = new FontIcon(isUser ? "fas-user" : "fas-robot");
        avatar.setIconSize(16);
        avatar.getStyleClass().add("message-avatar");
        
        Label nameLabel = new Label(isUser ? "You" : "iTutor");
        nameLabel.getStyleClass().add("message-name");
        
        Label timestamp = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));
        timestamp.getStyleClass().add("message-timestamp");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button copyBtn = new Button();
        copyBtn.getStyleClass().add("copy-button");
        FontIcon copyIcon = new FontIcon("fas-copy");
        copyIcon.setIconSize(12);
        copyBtn.setGraphic(copyIcon);
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            copyIcon.setIconLiteral("fas-check");
            javafx.application.Platform.runLater(() -> {
                try { Thread.sleep(1000); } catch (Exception ex) {}
                copyIcon.setIconLiteral("fas-copy");
            });
        });
        
        headerRow.getChildren().addAll(avatar, nameLabel, spacer, timestamp, copyBtn);
        
        VBox bubbleContainer = new VBox(5);
        bubbleContainer.getStyleClass().addAll("message-bubble", styleClass);
        
        if (isUser) {
            Label lbl = new Label(text);
            lbl.setWrapText(true);
            lbl.getStyleClass().add("message-text");
            bubbleContainer.getChildren().add(lbl);
        } else {
            parseContent(bubbleContainer, text);
        }
        
        messageContainer.getChildren().addAll(headerRow, bubbleContainer);
        
        HBox wrapper = new HBox();
        if (isUser) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }
        wrapper.getChildren().add(messageContainer);
        
        return wrapper;
    }

    private static void parseContent(VBox container, String text) {
        // Regex to find code blocks: ```language code ``` or just ``` code ```
        Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n?(.*?)```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(text);

        int lastIndex = 0;

        while (matcher.find()) {
            // Text before the code block
            String preText = text.substring(lastIndex, matcher.start());
            if (!preText.isEmpty()) {
                parseTextAndListsAndTables(container, preText);
            }

            // The code block
            String language = matcher.group(1); // e.g. "java" or ""
            String code = matcher.group(2);     // the code content
            container.getChildren().add(createCodeBlock(language, code));

            lastIndex = matcher.end();
        }

        // Remaining text after the last code block
        String remaining = text.substring(lastIndex);
        if (!remaining.isEmpty()) {
            parseTextAndListsAndTables(container, remaining);
        }
    }

    private static void parseTextAndListsAndTables(VBox container, String text) {
        // Split by newlines to detect list items and tables
        String[] lines = text.split("\\r?\\n");

        List<String> tableBuffer = new ArrayList<>();
        boolean inTable = false;

        for (String line : lines) {
            String trimmedLine = line.trim();
            // A valid table row must start and end with '|' and have at least 2 characters (e.g., "|a|")
            boolean isTableRow = trimmedLine.startsWith("|") && trimmedLine.endsWith("|") && trimmedLine.length() > 1;

            if (isTableRow) {
                if (!inTable) {
                    inTable = true;
                }
                tableBuffer.add(line);
            } else {
                if (inTable) {
                    // End of table detected, process buffer
                    container.getChildren().add(createTable(tableBuffer));
                    tableBuffer.clear();
                    inTable = false;
                }

                // Process normal line or list item
                processLine(container, line);
            }
        }

        // If we ended while still in a table
        if (inTable && !tableBuffer.isEmpty()) {
            container.getChildren().add(createTable(tableBuffer));
        }
    }

    private static void processLine(VBox container, String line) {
        // Check for list item: starts with * or - followed by space
        Matcher listMatcher = Pattern.compile("^\\s*[*-]\\s+(.*)").matcher(line);

        // Check for header: starts with one or more #
        Matcher headerMatcher = Pattern.compile("^\\s*(#+)\\s+(.*)").matcher(line);
        
        // Check for horizontal rule
        Matcher hrMatcher = Pattern.compile("^\\s*---+\\s*$").matcher(line);

        if (hrMatcher.matches()) {
            container.getChildren().add(new Separator());
        } else if (listMatcher.find()) {
            String content = listMatcher.group(1);

            HBox listItem = new HBox(8); // Spacing between bullet and text
            listItem.getStyleClass().add("list-item");

            Label bullet = new Label("•");
            bullet.getStyleClass().add("list-bullet");
            bullet.setMinWidth(Region.USE_PREF_SIZE);

            TextFlow contentFlow = createRichText(content);
            HBox.setHgrow(contentFlow, Priority.ALWAYS);

            listItem.getChildren().addAll(bullet, contentFlow);
            container.getChildren().add(listItem);
        } else if (headerMatcher.find()) {
            String hashes = headerMatcher.group(1);
            String content = headerMatcher.group(2);
            TextFlow headerFlow = createRichText(content);
            
            int level = hashes.length();
            String styleClass = "header-text-" + level; // e.g., header-text-1, header-text-2
            double fontSize = 1.0 + (4 - level) * 0.1; // h1 -> 1.3em, h2 -> 1.2em, etc.

            // Apply a header style class to all text nodes in the flow
            headerFlow.getChildren().forEach(node -> {
                if (node instanceof Text) {
                    node.getStyleClass().add("header-text");
                    node.getStyleClass().add(styleClass);
                    node.setStyle("-fx-font-weight: bold; -fx-font-size: " + fontSize + "em;");
                }
            });
            container.getChildren().add(headerFlow);
        } else {
            if (!line.trim().isEmpty()) {
                container.getChildren().add(createRichText(line));
            }
        }
    }

    public static Node createTable(List<String> tableLines) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("markdown-table");
        grid.setHgap(1);
        grid.setVgap(1);

        int row = 0;
        int maxCols = 0;
        
        // First pass to determine max columns
        for (String line : tableLines) {
             if (line.matches(".*\\|\\s*:?-+:?\\s*\\|.*")) continue;
             String trimmedLine = line.trim();
             if (trimmedLine.length() < 2) continue; // Defensive check
             String[] cells = trimmedLine.substring(1, trimmedLine.length() - 1).split("\\|");
             maxCols = Math.max(maxCols, cells.length);
        }
        
        // Add column constraints to allow horizontal growth
        for (int i = 0; i < maxCols; i++) {
            ColumnConstraints colConst = new ColumnConstraints();
            colConst.setHgrow(Priority.ALWAYS);
            colConst.setPercentWidth(100.0 / maxCols); // Distribute width equally
            grid.getColumnConstraints().add(colConst);
        }

        for (String line : tableLines) {
            // Skip separator lines like |---|---|
            if (line.matches(".*\\|\\s*:?-+:?\\s*\\|.*")) {
                continue;
            }

            String trimmedLine = line.trim();
            if (trimmedLine.length() < 2) continue; // Defensive check

            String[] cells = trimmedLine.substring(1, trimmedLine.length() - 1).split("\\|");
            int col = 0;
            for (String cellText : cells) {
                VBox cellBox = new VBox();
                cellBox.getStyleClass().add(row == 0 ? "table-header-cell" : "table-cell");

                TextFlow content = createRichText(cellText.trim());
                cellBox.getChildren().add(content);

                GridPane.setHgrow(cellBox, Priority.ALWAYS);
                grid.add(cellBox, col, row);
                col++;
            }
            row++;
        }

        return grid;
    }

    private static TextFlow createRichText(String text) {
        TextFlow flow = new TextFlow();
        
        // Combined pattern for bold (double and single asterisk) and inline code
        Pattern pattern = Pattern.compile("(\\*\\*(.*?)\\*\\*)|(\\*([^*]+?)\\*)|(`(.*?)`)");
        Matcher matcher = pattern.matcher(text);
        
        int lastIndex = 0;
        while (matcher.find()) {
            // Add normal text before the match
            String normalPart = text.substring(lastIndex, matcher.start());
            if (!normalPart.isEmpty()) {
                Text t = new Text(normalPart);
                t.getStyleClass().add("normal-text");
                flow.getChildren().add(t);
            }
            
            // Check which group was matched
            if (matcher.group(1) != null) { // Double asterisk bold
                String boldPart = matcher.group(2);
                Text t = new Text(boldPart);
                t.setStyle("-fx-font-weight: bold;");
                t.getStyleClass().add("bold-text");
                flow.getChildren().add(t);
            } else if (matcher.group(3) != null) { // Single asterisk bold
                String boldPart = matcher.group(4);
                Text t = new Text(boldPart);
                t.setStyle("-fx-font-weight: bold;");
                t.getStyleClass().add("bold-text");
                flow.getChildren().add(t);
            } else if (matcher.group(5) != null) { // Inline code
                String codePart = matcher.group(6);
                Label codeLabel = new Label(codePart);
                codeLabel.getStyleClass().add("inline-code");
                flow.getChildren().add(codeLabel);
            }
            
            lastIndex = matcher.end();
        }
        
        // Add any remaining text
        String remaining = text.substring(lastIndex);
        if (!remaining.isEmpty()) {
            Text t = new Text(remaining);
            t.getStyleClass().add("normal-text");
            flow.getChildren().add(t);
        }
        
        return flow;
    }

    public static VBox createCodeBlock(String language, String code) {
        VBox codeBox = new VBox(2);
        codeBox.getStyleClass().add("code-box");
        
        // Header with language and copy button
        HBox header = new HBox();
        header.getStyleClass().add("code-header");
        Label langLabel = new Label(language.toUpperCase());
        langLabel.getStyleClass().add("code-language");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button copyBtn = new Button("Copy");
        copyBtn.getStyleClass().add("copy-button");
        copyBtn.setOnAction(e -> {
            Clipboard cb = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(code);
            cb.setContent(content);
            copyBtn.setText("Copied!");
            // Reset text after 2 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(2000);
                    Platform.runLater(() -> copyBtn.setText("Copy"));
                } catch (InterruptedException ex) { }
            }).start();
        });
        
        header.getChildren().addAll(langLabel, spacer, copyBtn);
        
        // Code content
        Label codeLabel = new Label(code);
        codeLabel.getStyleClass().add("code-text");
        codeLabel.setWrapText(true);

        codeBox.getChildren().addAll(header, codeLabel);
        return codeBox;
    }

    public static Node makeStreamingBubble(String text, String styleClass) {
        boolean isUser = styleClass.contains("message-user");
        
        VBox messageContainer = new VBox(4);
        messageContainer.getStyleClass().add("message-row");
        
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon avatar = new FontIcon(isUser ? "fas-user" : "fas-robot");
        avatar.setIconSize(16);
        avatar.getStyleClass().add("message-avatar");
        
        Label nameLabel = new Label(isUser ? "You" : "iTutor");
        nameLabel.getStyleClass().add("message-name");
        
        Label timestamp = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));
        timestamp.getStyleClass().add("message-timestamp");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        headerRow.getChildren().addAll(avatar, nameLabel, spacer, timestamp);
        
        VBox bubbleContainer = new VBox(5);
        bubbleContainer.getStyleClass().addAll("message-bubble", styleClass);
        
        TextFlow flow = createRichTextFlow(text);
        flow.setMaxWidth(600);
        bubbleContainer.getChildren().add(flow);
        
        messageContainer.getChildren().addAll(headerRow, bubbleContainer);
        
        HBox wrapper = new HBox();
        if (isUser) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }
        wrapper.getChildren().add(messageContainer);
        
        return wrapper;
    }

    public static Node makeBubbleFromNode(String text, String styleClass, Node contentNode) {
        boolean isUser = styleClass.contains("message-user");
        
        VBox messageContainer = new VBox(4);
        messageContainer.getStyleClass().add("message-row");
        
        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);
        
        FontIcon avatar = new FontIcon(isUser ? "fas-user" : "fas-robot");
        avatar.setIconSize(16);
        avatar.getStyleClass().add("message-avatar");
        
        Label nameLabel = new Label(isUser ? "You" : "iTutor");
        nameLabel.getStyleClass().add("message-name");
        
        Label timestamp = new Label(LocalDateTime.now().format(DateTimeFormatter.ofPattern("h:mm a")));
        timestamp.getStyleClass().add("message-timestamp");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button copyBtn = new Button();
        copyBtn.getStyleClass().add("copy-button");
        FontIcon copyIcon = new FontIcon("fas-copy");
        copyIcon.setIconSize(12);
        copyBtn.setGraphic(copyIcon);
        copyBtn.setOnAction(e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(text);
            Clipboard.getSystemClipboard().setContent(content);
            copyIcon.setIconLiteral("fas-check");
            Platform.runLater(() -> {
                try { Thread.sleep(1000); } catch (Exception ex) {}
                copyIcon.setIconLiteral("fas-copy");
            });
        });
        
        headerRow.getChildren().addAll(avatar, nameLabel, spacer, timestamp, copyBtn);
        
        VBox bubbleContainer = new VBox(5);
        bubbleContainer.getStyleClass().addAll("message-bubble", styleClass);
        
        if (contentNode instanceof VBox) {
            bubbleContainer.getChildren().addAll(((VBox) contentNode).getChildren());
        } else {
            bubbleContainer.getChildren().add(contentNode);
        }
        
        messageContainer.getChildren().addAll(headerRow, bubbleContainer);
        
        HBox wrapper = new HBox();
        if (isUser) {
            wrapper.setAlignment(Pos.CENTER_RIGHT);
        } else {
            wrapper.setAlignment(Pos.CENTER_LEFT);
        }
        wrapper.getChildren().add(messageContainer);
        
        return wrapper;
    }

    public static TextFlow createRichTextFlow(String text) {
        TextFlow flow = new TextFlow();
        
        if (text == null || text.isEmpty()) {
            return flow;
        }
        
        Pattern pattern = Pattern.compile(
            "(\\*\\*(.+?)\\*\\*)|" +
            "(--(.+?)--)|" +
            "(\\*([^*]+)\\*)|" +
            "(`([^`]+)`)|" +
            "(\\[([^\\]]+)\\]\\(([^)]+)\\))|" +
            "(\\[([^\\]]+)\\])"
        );
        
        Matcher matcher = pattern.matcher(text);
        int lastIndex = 0;
        
        while (matcher.find()) {
            String normalPart = text.substring(lastIndex, matcher.start());
            if (!normalPart.isEmpty()) {
                Text t = new Text(normalPart);
                t.getStyleClass().add("normal-text");
                flow.getChildren().add(t);
            }
            
            if (matcher.group(1) != null) {
                String boldPart = matcher.group(2);
                Text t = new Text(boldPart);
                t.setStyle("-fx-font-weight: bold;");
                t.getStyleClass().add("bold-text");
                flow.getChildren().add(t);
            } else if (matcher.group(3) != null) {
                String strikePart = matcher.group(4);
                Text t = new Text(strikePart);
                t.setStyle("-fx-strikethrough: true; -fx-fill: #6B7280;");
                t.getStyleClass().add("strike-text");
                flow.getChildren().add(t);
            } else if (matcher.group(5) != null) {
                String italicPart = matcher.group(6);
                Text t = new Text(italicPart);
                t.setStyle("-fx-font-style: italic;");
                t.getStyleClass().add("italic-text");
                flow.getChildren().add(t);
            } else if (matcher.group(7) != null) {
                String codePart = matcher.group(8);
                Label codeLabel = new Label(codePart);
                codeLabel.getStyleClass().add("inline-code");
                flow.getChildren().add(codeLabel);
            } else if (matcher.group(9) != null) {
                String linkText = matcher.group(10);
                String linkUrl = matcher.group(11);
                Label linkLabel = new Label(linkText);
                linkLabel.getStyleClass().add("link-text");
                linkLabel.setStyle("-fx-text-fill: #1B7A3E; -fx-underline: true; -fx-cursor: hand;");
                linkLabel.setTooltip(new javafx.scene.control.Tooltip("URL: " + linkUrl));
                flow.getChildren().add(linkLabel);
            } else if (matcher.group(12) != null) {
                String refText = matcher.group(13);
                Label refLabel = new Label(refText);
                refLabel.getStyleClass().add("reference-text");
                refLabel.setStyle("-fx-text-fill: #1B7A3E;");
                flow.getChildren().add(refLabel);
            }
            
            lastIndex = matcher.end();
        }
        
        String remaining = text.substring(lastIndex);
        if (!remaining.isEmpty()) {
            Text t = new Text(remaining);
            t.getStyleClass().add("normal-text");
            flow.getChildren().add(t);
        }
        
        return flow;
    }

    public static Node parseFullContent(String text) {
        VBox container = new VBox(8);
        container.getStyleClass().add("formatted-content");
        
        parseContent(container, text);
        
        return container;
    }
}
