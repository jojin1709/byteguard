package sentinel;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Dual-channel logger for the Sentinel Java Agent.
 * <p>
 * Writes all log output to {@code stdout} and, if configured, to a log file simultaneously.
 * All methods are thread-safe.
 *
 * @author JOJIN JOHN
 */
public final class AgentLogger {

    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String PREFIX = "[Sentinel] ";

    private static volatile PrintWriter fileWriter = null;

    private AgentLogger() {}

    /**
     * Initializes file logging. If a log file path is set in {@link AgentConfig},
     * this method opens the file for appending.
     *
     * @param logFilePath path to the log file, or {@code null} to disable file logging
     */
    public static synchronized void init(String logFilePath) {
        if (logFilePath == null || logFilePath.isBlank()) {
            return;
        }
        try {
            fileWriter = new PrintWriter(new FileWriter(logFilePath, true), true);
            info("File logging enabled → " + logFilePath);
        } catch (IOException e) {
            System.err.println(PREFIX + "Could not open log file '" + logFilePath + "': " + e.getMessage());
        }
    }

    /**
     * Logs an informational message.
     *
     * @param message the message to log
     */
    public static void info(String message) {
        String line = PREFIX + message;
        System.out.println(line);
        writeToFile("INFO", message);
    }

    /**
     * Logs a warning message.
     *
     * @param message the message to log
     */
    public static void warn(String message) {
        String line = PREFIX + "[WARN] " + message;
        System.out.println(line);
        writeToFile("WARN", message);
    }

    /**
     * Logs an error message.
     *
     * @param message the message to log
     */
    public static void error(String message) {
        String line = PREFIX + "[ERROR] " + message;
        System.err.println(line);
        writeToFile("ERROR", message);
    }

    /**
     * Closes the file writer if open. Should be called from the JVM shutdown hook.
     */
    public static synchronized void close() {
        if (fileWriter != null) {
            fileWriter.flush();
            fileWriter.close();
            fileWriter = null;
        }
    }

    private static synchronized void writeToFile(String level, String message) {
        if (fileWriter == null) {
            return;
        }
        fileWriter.printf("%s [%s] %s%s%n",
                LocalDateTime.now().format(TIMESTAMP), level, PREFIX, message);
    }
}
