package com.lj.aichatapp.utils;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ResponseFormatter {

    public enum ResponseType {
        PLAIN_TEXT,
        CODE_WITH_EXPLANATION,
        TABLE_DATA,
        STEP_BY_STEP,
        NUMBERED_LIST,
        MIXED_CONTENT
    }

    private static final int CODE_BLOCK_THRESHOLD = 100;
    private static final int TABLE_MIN_ROWS = 2;

    public static ResponseType detectType(String text) {
        if (text == null || text.isEmpty()) {
            return ResponseType.PLAIN_TEXT;
        }

        boolean hasCodeBlocks = countOccurrences(text, "```") >= 2;
        boolean hasTable = detectTable(text);
        boolean hasSteps = detectSteps(text);
        boolean hasNumberedList = detectNumberedList(text);
        boolean hasBulletList = detectBulletList(text);
        boolean hasHeaders = detectHeaders(text);
        boolean hasMultipleFormats = countFormats(text) > 1;

        if (hasMultipleFormats || (hasCodeBlocks && (hasTable || hasSteps || hasHeaders))) {
            return ResponseType.MIXED_CONTENT;
        }
        if (hasCodeBlocks) {
            return ResponseType.CODE_WITH_EXPLANATION;
        }
        if (hasTable) {
            return ResponseType.TABLE_DATA;
        }
        if (hasSteps) {
            return ResponseType.STEP_BY_STEP;
        }
        if (hasNumberedList) {
            return ResponseType.NUMBERED_LIST;
        }
        if (hasBulletList || hasHeaders) {
            return ResponseType.MIXED_CONTENT;
        }

        return ResponseType.PLAIN_TEXT;
    }

    public static Node format(String text, ResponseType type) {
        switch (type) {
            case CODE_WITH_EXPLANATION:
                return formatCodeWithExplanation(text);
            case TABLE_DATA:
                return formatAsTable(text);
            case STEP_BY_STEP:
                return formatAsSteps(text);
            case NUMBERED_LIST:
                return formatAsNumberedList(text);
            case MIXED_CONTENT:
                return formatMixedContent(text);
            default:
                return formatDefault(text);
        }
    }

    public static Node formatDefault(String text) {
        return FxUtils.createRichTextFlow(text);
    }

    private static Node formatCodeWithExplanation(String text) {
        VBox container = new VBox(8);
        container.getStyleClass().add("formatted-content");

        Pattern codeBlockPattern = Pattern.compile("```(\\w*)\\n?(.*?)```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(text);

        int lastIndex = 0;
        while (matcher.find()) {
            String preText = text.substring(lastIndex, matcher.start());
            if (!preText.trim().isEmpty()) {
                container.getChildren().add(FxUtils.createRichTextFlow(preText));
            }

            String language = matcher.group(1);
            String code = matcher.group(2);
            container.getChildren().add(FxUtils.createCodeBlock(language, code));

            lastIndex = matcher.end();
        }

        String remaining = text.substring(lastIndex);
        if (!remaining.trim().isEmpty()) {
            container.getChildren().add(FxUtils.createRichTextFlow(remaining));
        }

        return container;
    }

    private static Node formatAsTable(String text) {
        List<String> tableLines = new ArrayList<>();
        String[] lines = text.split("\\r?\\n");

        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 1) {
                tableLines.add(line);
            }
        }

        if (tableLines.isEmpty()) {
            return formatDefault(text);
        }

        return FxUtils.createTable(tableLines);
    }

    private static Node formatAsSteps(String text) {
        VBox container = new VBox(12);
        container.getStyleClass().add("step-content");

        Pattern stepPattern = Pattern.compile(
            "(?i)(step\\s*\\d+[:\\.]?\\s*|\\d+[\\.\\)]\\s*)(.+?)(?=(?:step\\s*\\d+)|(?:\\n\\d+[\\.\\)])|$)",
            Pattern.DOTALL
        );

        Matcher matcher = stepPattern.matcher(text);
        int stepNum = 1;

        if (matcher.find()) {
            do {
                String stepContent = matcher.group(2).trim();
                HBox stepBox = createStepBox(stepNum, stepContent);
                container.getChildren().add(stepBox);
                stepNum++;
            } while (matcher.find());
        } else {
            String[] lines = text.split("\\r?\\n");
            for (String line : lines) {
                if (line.trim().isEmpty()) continue;
                container.getChildren().add(FxUtils.createRichTextFlow(line));
            }
        }

        return container;
    }

    private static HBox createStepBox(int stepNum, String content) {
        HBox stepBox = new HBox(12);
        stepBox.getStyleClass().add("step-item");

        Label stepNumber = new Label(String.valueOf(stepNum));
        stepNumber.getStyleClass().add("step-number");

        TextFlow contentFlow = FxUtils.createRichTextFlow(content);
        HBox.setHgrow(contentFlow, javafx.scene.layout.Priority.ALWAYS);

        stepBox.getChildren().addAll(stepNumber, contentFlow);
        return stepBox;
    }

    private static Node formatAsNumberedList(String text) {
        VBox container = new VBox(8);
        container.getStyleClass().add("numbered-list-content");

        Pattern listPattern = Pattern.compile("^\\s*(\\d+)[\\.\\)]\\s+(.+)$", Pattern.MULTILINE);
        Matcher matcher = listPattern.matcher(text);

        while (matcher.find()) {
            String number = matcher.group(1);
            String content = matcher.group(2);

            HBox listItem = new HBox(10);
            listItem.getStyleClass().add("numbered-list-item");

            Label numLabel = new Label(number + ".");
            numLabel.getStyleClass().add("number-label");

            TextFlow contentFlow = FxUtils.createRichTextFlow(content);
            HBox.setHgrow(contentFlow, javafx.scene.layout.Priority.ALWAYS);

            listItem.getChildren().addAll(numLabel, contentFlow);
            container.getChildren().add(listItem);
        }

        return container;
    }

    private static Node formatMixedContent(String text) {
        return FxUtils.parseFullContent(text);
    }

    private static boolean detectTable(String text) {
        String[] lines = text.split("\\r?\\n");
        int tableRows = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length() > 1) {
                if (!trimmed.matches(".*\\|\\s*:?-+:?\\s*\\|.*")) {
                    tableRows++;
                }
            }
        }
        return tableRows >= TABLE_MIN_ROWS;
    }

    private static boolean detectSteps(String text) {
        Pattern stepPattern = Pattern.compile("(?i)\\b(step\\s*\\d+|first|second|third|next|finally)\\b");
        return stepPattern.matcher(text).find();
    }

    private static boolean detectNumberedList(String text) {
        Pattern listPattern = Pattern.compile("^\\s*\\d+[\\.\\)]\\s+", Pattern.MULTILINE);
        return listPattern.matcher(text).find();
    }

    private static boolean detectBulletList(String text) {
        Pattern bulletPattern = Pattern.compile("^\\s*[-*+•]\\s+", Pattern.MULTILINE);
        return bulletPattern.matcher(text).find();
    }

    private static boolean detectHeaders(String text) {
        Pattern headerPattern = Pattern.compile("^\\s*#{1,6}\\s+", Pattern.MULTILINE);
        return headerPattern.matcher(text).find();
    }

    private static int countFormats(String text) {
        int count = 0;
        if (countOccurrences(text, "```") >= 2) count++;
        if (detectTable(text)) count++;
        if (detectSteps(text)) count++;
        if (detectNumberedList(text)) count++;
        if (detectBulletList(text)) count++;
        if (detectHeaders(text)) count++;
        return count;
    }

    private static int countOccurrences(String text, String pattern) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(pattern, index)) != -1) {
            count++;
            index += pattern.length();
        }
        return count;
    }

    public static String getPreviewText(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "";
        
        String cleaned = text.replaceAll("```[\\s\\S]*?```", "")
                            .replaceAll("`[^`]+`", "")
                            .replaceAll("\\*\\*[^*]+\\*\\*", "")
                            .replaceAll("\\*[^*]+\\*", "")
                            .replaceAll("#+\\s*", "")
                            .replaceAll("\\|", "")
                            .replaceAll("[-*+•]\\s+", "")
                            .replaceAll("\\d+\\.\\s+", "")
                            .trim();
        
        if (cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength).trim() + "...";
    }
}