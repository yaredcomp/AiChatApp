package com.lj.aichatapp.utils;

import com.lj.aichatapp.infrastructure.preferences.PreferencesManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static final String LOG_FILE_NAME = "app.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    public static void info(String message) {
        log("INFO", message, null);
    }
    
    public static void logSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Application Session Started ===").append(System.lineSeparator());
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch")).append(")").append(System.lineSeparator());
        sb.append("Java: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")").append(System.lineSeparator());
        sb.append("User Home: ").append(System.getProperty("user.home")).append(System.lineSeparator());
        sb.append("App Dir: ").append(PreferencesManager.getAppDirectory().toAbsolutePath()).append(System.lineSeparator());
        sb.append("===================================");
        
        log("INFO", sb.toString(), null);
    }

    private static void log(String level, String message, Throwable throwable) {
        try {
            Path logFile = PreferencesManager.getAppDirectory().resolve(LOG_FILE_NAME);
            PreferencesManager.ensureAppDirectory();

            StringBuilder sb = new StringBuilder();
            sb.append("[").append(LocalDateTime.now().format(DATE_FORMATTER)).append("] ");
            sb.append("[").append(level).append("] ");
            sb.append(message).append(System.lineSeparator());

            if (throwable != null) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                throwable.printStackTrace(pw);
                sb.append(sw.toString()).append(System.lineSeparator());
            }

            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // Also print to standard error/out for development
            if ("ERROR".equals(level)) {
                System.err.print(sb.toString());
            } else {
                System.out.print(sb.toString());
            }

        } catch (IOException e) {
            e.printStackTrace(); // Fallback if logging fails
        }
    }
}
