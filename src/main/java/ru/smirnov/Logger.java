package ru.smirnov;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    private static volatile Logger instance;
    private final String logFilePath;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private Logger(String logFilePath) {
        this.logFilePath = logFilePath;
    }

    public static Logger getInstance() {
        if (instance == null) {
            synchronized (Logger.class) {
                if (instance == null) {
                    SettingsLoader settings = new SettingsLoader("settings.properties");
                    instance = new Logger(settings.getLogFile());
                }
            }
        }
        return instance;
    }

    public void logAndPrint(String from, String data) {
        System.out.println(data);
        printToFile(from, data);
    }

    public void log(String from, String data) {
        printToFile(from, data);
    }

    private synchronized void printToFile(String from, String data) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logEntry = timestamp + " - " + from + " - " + data;

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFilePath, true))) {
            writer.write(logEntry);
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
