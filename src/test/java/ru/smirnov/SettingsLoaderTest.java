package ru.smirnov;

import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class SettingsLoaderTest {

    private static final String TEMP_FILE = "settings.properties";

    @BeforeEach
    void setUp() throws IOException {
        String content = "server.port=12345\nlogfile=test.log";
        Path path = Paths.get("target/test-classes/" + TEMP_FILE); // путь в test classpath
        Files.createDirectories(path.getParent());
        Files.write(path, content.getBytes());
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(Paths.get("target/test-classes/" + TEMP_FILE));
    }

    @Test
    void testLoadsCorrectPortAndLogfile() {
        SettingsLoader loader = new SettingsLoader(TEMP_FILE);

        int expectedPort = 12345;
        String expectedLogFile = "test.log";

        assertEquals(expectedPort, loader.getPort());
        assertEquals(expectedLogFile, loader.getLogFile());
    }

    @Test
    void testDefaultValuesIfMissing() throws IOException {
        String content = "";
        Path path = Paths.get("target/test-classes/" + TEMP_FILE);
        Files.write(path, content.getBytes());

        SettingsLoader loader = new SettingsLoader(TEMP_FILE);

        int expectedPort = 8080;
        String expectedLogFile = "file.log";

        assertEquals(expectedPort, loader.getPort());
        assertEquals(expectedLogFile, loader.getLogFile());
    }

    @Test
    void testMissingFileDoesNotCrash() {
        SettingsLoader loader = new SettingsLoader("nonexistent.properties");

        int expectedPort = 8080;
        String expectedLogFile = "file.log";

        assertEquals(expectedPort, loader.getPort());
        assertEquals(expectedLogFile, loader.getLogFile());
    }
}
