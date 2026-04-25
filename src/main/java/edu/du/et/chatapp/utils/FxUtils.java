/*
 * Copyright (c) 2024 Academia ChatAI. All rights reserved.
 */

package edu.du.et.chatapp.utils;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
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
 * Utility class for JavaFX UI operations and Markdown parsing.
 *
 * <p>Provides methods for animations, creating message bubbles, and rendering
 * formatted content (Markdown) into JavaFX nodes.
 */
public class FxUtils {

  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a");

  /**
   * Applies a fade-in animation to a node.
   *
   * @param node The node to animate.
   */
  public static void fadeIn(Node node) {
    node.setOpacity(0);
    FadeTransition ft = new FadeTransition(Duration.millis(220), node);
    ft.setToValue(1.0);
    ft.play();
  }

  /**
   * Creates a standard message bubble.
   *
   * @param text The raw message text.
   * @param styleClass The CSS style class for the bubble.
   * @return A JavaFX Node representing the message.
   */
  public static Node makeBubble(String text, String styleClass) {
    return createBubble(text, styleClass, null);
  }

  /**
   * Creates a message bubble from a pre-rendered node.
   *
   * @param text The raw source text (for copying).
   * @param styleClass The CSS style class.
   * @param contentNode The pre-rendered content.
   * @return A JavaFX Node representing the message.
   */
  public static Node makeBubbleFromNode(String text, String styleClass, Node contentNode) {
    return createBubble(text, styleClass, contentNode);
  }

  /**
   * Creates a simplified streaming message bubble.
   *
   * @param text The current partial text.
   * @param styleClass The CSS style class.
   * @return A JavaFX Node representing the streaming message.
   */
  public static Node makeStreamingBubble(String text, String styleClass) {
    TextFlow flow = new TextFlow(new Text(text));
    flow.getChildren().forEach(n -> n.getStyleClass().add("normal-text"));
    return createBubble(text, styleClass, flow);
  }

  /**
   * Internal helper to construct the bubble layout.
   */
  private static Node createBubble(String rawText, String styleClass, Node contentNode) {
    boolean isUser = styleClass.contains("message-user");

    VBox messageContainer = new VBox(4);
    messageContainer.getStyleClass().add("message-row");

    // Header Construction
    HBox headerRow = createHeader(isUser, rawText);

    // Content Construction
    VBox bubbleContainer = new VBox(5);
    bubbleContainer.getStyleClass().addAll("message-bubble", styleClass);

    if (contentNode != null) {
      bubbleContainer.getChildren().add(contentNode);
    } else {
      if (isUser) {
        Label lbl = new Label(rawText);
        lbl.setWrapText(true);
        lbl.getStyleClass().add("message-text");
        lbl.setStyle("-fx-text-fill: inherit;");
        bubbleContainer.getChildren().add(lbl);
      } else {
        parseContent(bubbleContainer, rawText);
      }
    }

    messageContainer.getChildren().addAll(headerRow, bubbleContainer);

    HBox wrapper = new HBox();
    wrapper.setAlignment(isUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
    wrapper.getChildren().add(messageContainer);

    return wrapper;
  }

  /**
   * Creates the header row for a message bubble.
   */
  private static HBox createHeader(boolean isUser, String text) {
    HBox header = new HBox(8);
    header.setAlignment(Pos.CENTER_LEFT);

    FontIcon avatar = new FontIcon(isUser ? "fas-user" : "fas-robot");
    avatar.setIconSize(16);
    avatar.getStyleClass().add("message-avatar");

    Label nameLabel = new Label(isUser ? "You" : "Academia ChatAI");
    nameLabel.getStyleClass().add("message-name");

    Label timestamp = new Label(LocalDateTime.now().format(TIME_FORMATTER));
    timestamp.getStyleClass().add("message-timestamp");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Button copyBtn = createCopyButton(text);

    header.getChildren().addAll(avatar, nameLabel, spacer, timestamp, copyBtn);
    return header;
  }

  /**
   * Creates a functional copy-to-clipboard button.
   */
  private static Button createCopyButton(String text) {
    Button btn = new Button();
    btn.getStyleClass().add("copy-button");
    FontIcon icon = new FontIcon("fas-copy");
    icon.setIconSize(12);
    btn.setGraphic(icon);
    btn.setTooltip(new Tooltip("Copy to clipboard"));

    btn.setOnAction(e -> {
      ClipboardContent content = new ClipboardContent();
      content.putString(text);
      Clipboard.getSystemClipboard().setContent(content);
      icon.setIconLiteral("fas-check");

      new Thread(() -> {
        try {
          Thread.sleep(1500);
          Platform.runLater(() -> icon.setIconLiteral("fas-copy"));
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }).start();
    });

    return btn;
  }

  /**
   * Parses Markdown-like content into structured JavaFX nodes.
   */
  private static void parseContent(VBox container, String text) {
    Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n?(.*?)```", Pattern.DOTALL);
    Matcher matcher = codeBlockPattern.matcher(text);

    int lastIndex = 0;
    while (matcher.find()) {
      String preText = text.substring(lastIndex, matcher.start());
      if (!preText.isEmpty()) {
        parseTextAndListsAndTables(container, preText);
      }

      String language = matcher.group(1);
      String code = matcher.group(2);
      container.getChildren().add(createCodeBlock(language, code));

      lastIndex = matcher.end();
    }

    String remaining = text.substring(lastIndex);
    if (!remaining.isEmpty()) {
      parseTextAndListsAndTables(container, remaining);
    }
  }

  private static void parseTextAndListsAndTables(VBox container, String text) {
    String[] lines = text.split("\\r?\\n");
    List<String> tableBuffer = new ArrayList<>();
    boolean inTable = false;

    for (String line : lines) {
      boolean isTableRow = line.trim().startsWith("|") && line.trim().endsWith("|");

      if (isTableRow) {
        if (!inTable) {
          inTable = true;
        }
        tableBuffer.add(line);
      } else {
        if (inTable) {
          container.getChildren().add(createTable(tableBuffer));
          tableBuffer.clear();
          inTable = false;
        }
        processLine(container, line);
      }
    }

    if (inTable) {
      container.getChildren().add(createTable(tableBuffer));
    }
  }

  private static void processLine(VBox container, String line) {
    Matcher listMatcher = Pattern.compile("^\\s*[*-]\\s+(.*)").matcher(line);
    Matcher headerMatcher = Pattern.compile("^\\s*(#+)\\s+(.*)").matcher(line);
    Matcher hrMatcher = Pattern.compile("^\\s*---+\\s*$").matcher(line);
    Matcher blockquoteMatcher = Pattern.compile("^>\\s+(.*)").matcher(line);

    if (hrMatcher.matches()) {
      container.getChildren().add(new Separator());
    } else if (listMatcher.find()) {
      container.getChildren().add(createListItem(listMatcher.group(1)));
    } else if (headerMatcher.find()) {
      container.getChildren().add(createHeaderLabel(headerMatcher.group(1), headerMatcher.group(2)));
    } else if (blockquoteMatcher.find()) {
      container.getChildren().add(createBlockquote(blockquoteMatcher.group(1)));
    } else if (!line.trim().isEmpty()) {
      container.getChildren().add(createRichText(line));
    }
  }

  private static Node createListItem(String content) {
    HBox item = new HBox(8);
    item.getStyleClass().add("list-item");
    Label bullet = new Label("•");
    bullet.getStyleClass().add("list-bullet");
    TextFlow contentFlow = createRichText(content);
    HBox.setHgrow(contentFlow, Priority.ALWAYS);
    item.getChildren().addAll(bullet, contentFlow);
    return item;
  }

  private static Node createHeaderLabel(String hashes, String content) {
    TextFlow flow = createRichText(content);
    double size = 1.4 - (hashes.length() * 0.1);
    flow.getChildren().forEach(n -> {
      n.getStyleClass().add("header-text");
      n.setStyle("-fx-font-size: " + Math.max(1.0, size) + "em; -fx-font-weight: bold;");
    });
    return flow;
  }

  private static Node createBlockquote(String content) {
    VBox box = new VBox();
    box.getStyleClass().add("blockquote");
    box.getChildren().add(createRichText(content));
    return box;
  }

  public static Node createTable(List<String> tableLines) {
    GridPane grid = new GridPane();
    grid.getStyleClass().add("markdown-table");
    grid.setHgap(0);
    grid.setVgap(0);

    int row = 0;
    int maxCols = 0;

    // Scan for col count
    for (String line : tableLines) {
      if (line.matches(".*\\|\\s*:?-+:?\\s*\\|.*")) {
        continue;
      }
      String[] cells = line.trim().substring(1, line.trim().length() - 1).split("\\|");
      maxCols = Math.max(maxCols, cells.length);
    }

    for (int i = 0; i < maxCols; i++) {
      ColumnConstraints cc = new ColumnConstraints();
      cc.setHgrow(Priority.ALWAYS);
      cc.setPercentWidth(100.0 / maxCols);
      grid.getColumnConstraints().add(cc);
    }

    for (String line : tableLines) {
      if (line.matches(".*\\|\\s*:?-+:?\\s*\\|.*")) {
        continue;
      }
      String trimmed = line.trim();
      String[] cells = trimmed.substring(1, trimmed.length() - 1).split("\\|");
      int col = 0;
      for (String cellText : cells) {
        VBox cellBox = new VBox();
        cellBox.getStyleClass().add(row == 0 ? "table-header-cell" : "table-cell");
        cellBox.getChildren().add(createRichText(cellText.trim()));
        grid.add(cellBox, col, row);
        col++;
      }
      row++;
    }
    return grid;
  }

  private static TextFlow createRichText(String text) {
    TextFlow flow = new TextFlow();
    Pattern pattern = Pattern.compile("(\\*\\*(.*?)\\*\\*)|(\\*([^*]+?)\\*)|(`(.*?)`)|(\\[(.*?)\\]\\((.*?)\\))");
    Matcher matcher = pattern.matcher(text);

    int lastIndex = 0;
    while (matcher.find()) {
      String normalPart = text.substring(lastIndex, matcher.start());
      if (!normalPart.isEmpty()) {
        flow.getChildren().add(new Text(normalPart));
      }

      if (matcher.group(1) != null) { // Bold
        Text t = new Text(matcher.group(2));
        t.getStyleClass().add("bold-text");
        flow.getChildren().add(t);
      } else if (matcher.group(3) != null) { // Italic
        Text t = new Text(matcher.group(4));
        t.getStyleClass().add("italic-text");
        flow.getChildren().add(t);
      } else if (matcher.group(5) != null) { // Inline Code
        Label l = new Label(matcher.group(6));
        l.getStyleClass().add("inline-code");
        flow.getChildren().add(l);
      } else if (matcher.group(7) != null) { // Link
        Label l = new Label(matcher.group(8));
        l.getStyleClass().add("link-text");
        l.setTooltip(new Tooltip(matcher.group(9)));
        flow.getChildren().add(l);
      }
      lastIndex = matcher.end();
    }

    String remaining = text.substring(lastIndex);
    if (!remaining.isEmpty()) {
      flow.getChildren().add(new Text(remaining));
    }
    return flow;
  }

  public static VBox createCodeBlock(String language, String code) {
    VBox codeBox = new VBox(0);
    codeBox.getStyleClass().add("code-box");

    HBox header = new HBox();
    header.getStyleClass().add("code-header");
    Label langLabel = new Label(language.toUpperCase());
    langLabel.getStyleClass().add("code-language");

    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);

    Button copyBtn = new Button("Copy");
    copyBtn.getStyleClass().add("copy-button");
    copyBtn.setOnAction(e -> {
      ClipboardContent cc = new ClipboardContent();
      cc.putString(code);
      Clipboard.getSystemClipboard().setContent(cc);
      copyBtn.setText("Copied!");
    });

    header.getChildren().addAll(langLabel, spacer, copyBtn);
    Label codeLabel = new Label(code);
    codeLabel.getStyleClass().add("code-text");
    codeLabel.setWrapText(true);

    VBox contentBox = new VBox(codeLabel);
    contentBox.getStyleClass().add("code-content");
    contentBox.setPadding(new Insets(12));

    codeBox.getChildren().addAll(header, contentBox);
    return codeBox;
  }

  /**
   * Parses Markdown text into a formatted JavaFX Node container.
   *
   * @param text The Markdown source text.
   * @return A container Node with formatted children.
   */
  public static Node parseFullContent(String text) {
    VBox container = new VBox(8);
    container.getStyleClass().add("formatted-content");
    if (text != null && !text.isEmpty()) {
      parseContent(container, text);
    }
    return container;
  }
}
