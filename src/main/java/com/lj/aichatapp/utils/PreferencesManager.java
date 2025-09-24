package com.lj.aichatapp.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lj.aichatapp.models.UserPreferences;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple JSON persistence for preferences & conversation.
 */
public class PreferencesManager {

    private static final ObjectMapper M = new ObjectMapper();
    private static final Path APP_DIR = Path.of(System.getProperty("user.home"), ".javafx_ai_chat");
    private static final Path PREFS_FILE = APP_DIR.resolve("preferences.json");
    private static final Path CONV_FILE = APP_DIR.resolve("conversation.json");

    public static Path getAppDirectory() {
        return APP_DIR;
    }

    public static void ensureAppDirectory() {
        try {
            if (!Files.exists(APP_DIR)) {
                Files.createDirectories(APP_DIR);
            }
        } catch (IOException e) {
            throw new RuntimeException("Unable to create app directory: " + APP_DIR, e);
        }
    }

    public static UserPreferences loadPreferences() {
        try {
            if (!Files.exists(PREFS_FILE)) {
                UserPreferences prefs = new UserPreferences();
                savePreferences(prefs);
                return prefs;
            }
            return M.readValue(PREFS_FILE.toFile(), UserPreferences.class);
        } catch (IOException e) {
            return new UserPreferences();
        }
    }

    public static void savePreferences(UserPreferences p) {
        try {
            ensureAppDirectory();
            M.writerWithDefaultPrettyPrinter().writeValue(PREFS_FILE.toFile(), p);
        } catch (IOException e) {
        }
    }

    public static void saveConversation(Object obj) {
        try {
            ensureAppDirectory();
            M.writerWithDefaultPrettyPrinter().writeValue(CONV_FILE.toFile(), obj);
        } catch (IOException e) {
        }
    }

    public static <T> T loadConversation(Class<T> cls, T fallback) {
        try {
            if (!Files.exists(CONV_FILE)) {
                return fallback;
            }
            return M.readValue(CONV_FILE.toFile(), cls);
        } catch (IOException e) {
            return fallback;
        }
    }
}
