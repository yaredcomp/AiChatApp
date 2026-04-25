package edu.du.et.chatapp.utils;

import edu.du.et.chatapp.infrastructure.preferences.PreferencesManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Enhanced Logger for DU Academia ChatAI.
 * Writes to both console and a local app.log file in the user's application directory.
 */
public class Logger {
    private static final String LOG_FILE_NAME = "app.log";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static void error(String message, Throwable throwable) {
        log("ERROR", message, throwable);
    }

    public static void error(String message) {
        log("ERROR", message, null);
    }

    public static void warn(String message, Throwable throwable) {
        log("WARN", message, throwable);
    }

    public static void warn(String message) {
        log("WARN", message, null);
    }

    public static void info(String message) {
        log("INFO", message, null);
    }
    
    /**
     * Captures critical system environment details to help diagnose cross-platform or 
     * deployment-specific issues (like resource loading in EXEs).
     */
    public static void logSystemInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append(System.lineSeparator());
        sb.append("===================================").append(System.lineSeparator());
        sb.append("=== Application Session Started ===").append(System.lineSeparator());
        sb.append("Timestamp: ").append(LocalDateTime.now().format(DATE_FORMATTER)).append(System.lineSeparator());
        sb.append("OS: ").append(System.getProperty("os.name")).append(" ").append(System.getProperty("os.version")).append(" (").append(System.getProperty("os.arch")).append(")").append(System.lineSeparator());
        sb.append("Java Runtime: ").append(System.getProperty("java.version")).append(" (").append(System.getProperty("java.vendor")).append(")").append(System.lineSeparator());
        sb.append("Java Home: ").append(System.getProperty("java.home")).append(System.lineSeparator());
        sb.append("User Home: ").append(System.getProperty("user.home")).append(System.lineSeparator());
        sb.append("App Data Dir: ").append(PreferencesManager.getAppDirectory().toAbsolutePath()).append(System.lineSeparator());
        sb.append("Class Path: ").append(System.getProperty("java.class.path")).append(System.lineSeparator());
        sb.append("===================================").append(System.lineSeparator());
        
        log("INFO", sb.toString(), null);
    }

    private static synchronized void log(String level, String message, Throwable throwable) {
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
                sb.append("Stack Trace: ").append(System.lineSeparator()).append(sw.toString()).append(System.lineSeparator());
            }

            Files.writeString(logFile, sb.toString(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            
            // Console output for development/terminal visibility
            if ("ERROR".equals(level)) {
                System.err.print(sb.toString());
            } else {
                System.out.print(sb.toString());
            }

        } catch (IOException e) {
            // Last resort: print to stderr if file writing fails
            System.err.println("CRITICAL: Logger failed to write to file: " + e.getMessage());
            System.err.println("[" + level + "] " + message);
            if (throwable != null) throwable.printStackTrace();
        }
    }
}
