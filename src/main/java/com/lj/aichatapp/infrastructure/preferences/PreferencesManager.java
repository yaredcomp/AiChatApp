package com.lj.aichatapp.infrastructure.preferences;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.lj.aichatapp.models.UserPreferences;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PreferencesManager {

    private static final String APP_DIR_NAME = ".aichatapp";
    private static final String PREFS_FILE_NAME = "user_prefs.json";
    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.registerModule(new JavaTimeModule());
    }

    public static Path getAppDirectory() {
        return Paths.get(System.getProperty("user.home"), APP_DIR_NAME);
    }

    public static void ensureAppDirectory() {
        File dir = getAppDirectory().toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public static void savePreferences(UserPreferences prefs) {
        try {
            ensureAppDirectory();
            mapper.writeValue(getAppDirectory().resolve(PREFS_FILE_NAME).toFile(), prefs);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static UserPreferences loadPreferences() {
        File prefsFile = getAppDirectory().resolve(PREFS_FILE_NAME).toFile();
        if (prefsFile.exists()) {
            try {
                return mapper.readValue(prefsFile, UserPreferences.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new UserPreferences();
    }
}