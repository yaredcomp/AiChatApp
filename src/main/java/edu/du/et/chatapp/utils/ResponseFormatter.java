package edu.du.et.chatapp.utils;

import javafx.scene.Node;

public class ResponseFormatter {

    /**
     * Enumeration for response types (legacy, mostly unused now as we prefer mixed parsing)
     */
    public enum ResponseType {
        PLAIN_TEXT,
        MIXED_CONTENT
    }

    /**
     * Detects the type of content. 
     * Since FxUtils handles mixed Markdown (code, tables, lists) robustly,
     * we almost always prefer MIXED_CONTENT unless it's strictly plain text.
     */
    public static ResponseType detectType(String text) {
        if (text == null || text.isEmpty()) {
            return ResponseType.PLAIN_TEXT;
        }
        // Always treat as mixed content to enable full Markdown rendering
        return ResponseType.MIXED_CONTENT;
    }

    /**
     * Formats the text into a JavaFX Node hierarchy.
     */
    public static Node format(String text, ResponseType type) {
        // We delegate to FxUtils which has the robust Markdown parser
        return FxUtils.parseFullContent(text);
    }

    /**
     * Helper to get a clean plain-text preview for the sidebar.
     */
    public static String getPreviewText(String text, int maxLength) {
        if (text == null || text.isEmpty()) return "";
        
        // Strip common Markdown symbols for a cleaner preview
        String cleaned = text.replaceAll("```[\\s\\S]*?```", "[Code]") // Replace code blocks
                            .replaceAll("`[^`]+`", "") // Remove inline code ticks
                            .replaceAll("\\*\\*", "") // Remove bold
                            .replaceAll("\\*", "") // Remove italics
                            .replaceAll("#+\\s*", "") // Remove headers
                            .replaceAll("\\|", " ") // Replace table pipes with space
                            .replaceAll("[-*+•]\\s+", "") // Remove list bullets
                            .replaceAll("\\d+\\.\\s+", "") // Remove list numbers
                            .replaceAll("\\[.*?\\]\\(.*?\\)", "") // Remove links
                            .replaceAll("\\s+", " ") // Collapse whitespace
                            .trim();
        
        if (cleaned.length() <= maxLength) return cleaned;
        return cleaned.substring(0, maxLength).trim() + "...";
    }
}
