package ru.smirnov;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class SettingsLoader {
    private Properties properties;

    public SettingsLoader(String filename) {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("settings.properties")) {
            if (input == null) {
                System.out.println("Не удалось найти settings.properties");
                return;
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public int getPort() {
        return Integer.parseInt(properties.getProperty("server.port", "8080"));
    }

    public String getLogFile() {
        return properties.getProperty("logfile", "file.log");
    }
}
