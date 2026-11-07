package io.student.rangiffler.jupiter.allure;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

/**
 * The {@code environment.properties} file of an Allure results directory, which Allure shows as the environment of the run.
 */
public final class EnvironmentFile {

    private final Path directory;

    public EnvironmentFile(Path directory) {
        this.directory = directory;
    }

    /**
     * Writes the values as {@code key=value} lines in the order of the map, through a temporary file, so that parallel test processes never leave a half-written file.
     */
    public void write(Map<String, String> values) {
        StringBuilder text = new StringBuilder();
        values.forEach((key, value) -> text.append(key).append('=').append(value).append('\n'));
        try {
            Files.createDirectories(directory);
            Path temporary = directory.resolve("environment-" + UUID.randomUUID() + ".tmp");
            Files.writeString(temporary, text, StandardCharsets.UTF_8);
            Files.move(temporary, directory.resolve("environment.properties"), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot write environment.properties to " + directory, exception);
        }
    }
}
