package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.MigrationCheckExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample classes through the JUnit engine to check that {@link MigrationCheckExtension} stops the run on a broken schema, and that {@link AllureEnvironmentExtension} leaves {@code environment.properties} in front of it but not behind it.
 */
@DisplayName("MigrationCheckExtension stops the run on a broken schema")
@Tag("extension")
class MigrationCheckExtensionTest {

    private static final String BROKEN = "rangiffler-auth: the schema has migration 7 that the files lack";

    @Test
    @DisplayName("A switched off check lets the tests run even when the schema is broken")
    void shouldRunTestsWhenCheckIsOff(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, Off.class);

        assertEquals(
                List.of(1L, List.of()),
                List.of(outcome.testEvents().succeeded().count(), failures(outcome)),
                "The tests did not run with the check off or the run failed"
        );
    }

    @Test
    @DisplayName("A broken schema stops the tests and the error names the difference and the way out")
    void shouldFailNamingDifferenceWhenSchemaIsBroken(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, Broken.class);

        assertEquals(
                List.of(0L, true),
                List.of(
                        outcome.testEvents().started().count(),
                        failures(outcome).stream().anyMatch(message -> message.contains(BROKEN) && message.contains("docker compose down -v"))
                ),
                "A test ran or the error did not name the difference and the way out"
        );
    }

    @Test
    @DisplayName("The environment file is written when the extension that writes it comes before the failing check")
    void shouldWriteFileWhenEnvironmentComesBeforeBrokenCheck(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, EnvironmentFirst.class);

        assertEquals(
                List.of(true, true),
                List.of(Files.exists(results.resolve("environment.properties")), failures(outcome).stream().anyMatch(message -> message.contains(BROKEN))),
                "The file was not written before the broken check or the check did not fail"
        );
    }

    @Test
    @DisplayName("The environment file is not written when the extension that writes it comes after the failing check")
    void shouldNotWriteFileWhenEnvironmentComesAfterBrokenCheck(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, EnvironmentLast.class);

        assertEquals(
                List.of(false, true),
                List.of(Files.exists(results.resolve("environment.properties")), failures(outcome).stream().anyMatch(message -> message.contains(BROKEN))),
                "The file was written behind the broken check or the check did not fail"
        );
    }

    @Test
    @DisplayName("With MySQL down the file is written, its schema versions are unavailable and the run fails with the cause")
    void shouldWriteUnavailableVersionsWhenDatabaseIsDown(@TempDir Path results) throws IOException {
        EngineExecutionResults outcome = run(results, DatabaseDown.class);
        Properties environment = read(results);

        assertEquals(
                List.of("unavailable", "unavailable", List.of("MySQL is down")),
                List.of(environment.getProperty("authSchemaVersion"), environment.getProperty("apiSchemaVersion"), failures(outcome)),
                "The file did not mark the versions unavailable or the run did not fail with the cause"
        );
    }

    private static EngineExecutionResults run(Path results, Class<?> sample) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(sample))
                .configurationParameter("rfr.testkit", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("probe.results", results.toString())
                .execute();
    }

    private static List<String> failures(EngineExecutionResults outcome) {
        return outcome.containerEvents().failed().stream()
                .map(event -> rootCause(event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow()).getMessage())
                .distinct()
                .toList();
    }

    private static Throwable rootCause(Throwable thrown) {
        return thrown.getCause() == null ? thrown : rootCause(thrown.getCause());
    }

    private static Properties read(Path results) throws IOException {
        Properties environment = new Properties();
        try (Reader reader = Files.newBufferedReader(results.resolve("environment.properties"))) {
            environment.load(reader);
        }
        return environment;
    }

    private static MigrationCheckExtension check(boolean enabled) {
        return new MigrationCheckExtension(() -> enabled, () -> List.of(BROKEN));
    }

    private static AllureEnvironmentExtension environment() {
        return new AllureEnvironmentExtension(
                context -> Path.of(context.getConfigurationParameter("probe.results").orElseThrow()),
                () -> "1",
                () -> "2"
        );
    }

    /**
     * Sample class whose schema is broken but whose check is switched off.
     */
    @ProbeTarget
    @Tag("ui")
    static class Off {

        @RegisterExtension
        static final MigrationCheckExtension CHECK = check(false);

        @Test
        void body() {
        }
    }

    /**
     * Sample class whose schema is broken and whose check is on.
     */
    @ProbeTarget
    @Tag("ui")
    static class Broken {

        @RegisterExtension
        static final MigrationCheckExtension CHECK = check(true);

        @Test
        void body() {
        }
    }

    /**
     * Sample class where the environment extension comes before the failing check.
     */
    @ProbeTarget
    @Tag("ui")
    static class EnvironmentFirst {

        @Order(1)
        @RegisterExtension
        static final AllureEnvironmentExtension ENVIRONMENT = environment();

        @Order(2)
        @RegisterExtension
        static final MigrationCheckExtension CHECK = check(true);

        @Test
        void body() {
        }
    }

    /**
     * Sample class where the environment extension comes after the failing check.
     */
    @ProbeTarget
    @Tag("ui")
    static class EnvironmentLast {

        @Order(1)
        @RegisterExtension
        static final MigrationCheckExtension CHECK = check(true);

        @Order(2)
        @RegisterExtension
        static final AllureEnvironmentExtension ENVIRONMENT = environment();

        @Test
        void body() {
        }
    }

    /**
     * Sample class where nothing can read the database: the check fails with the connection error and the schema versions are unreadable.
     */
    @ProbeTarget
    @Tag("ui")
    static class DatabaseDown {

        @Order(1)
        @RegisterExtension
        static final AllureEnvironmentExtension ENVIRONMENT = new AllureEnvironmentExtension(
                context -> Path.of(context.getConfigurationParameter("probe.results").orElseThrow()),
                () -> {
                    throw new IllegalStateException("MySQL is down");
                },
                () -> {
                    throw new IllegalStateException("MySQL is down");
                }
        );

        @Order(2)
        @RegisterExtension
        static final MigrationCheckExtension CHECK = new MigrationCheckExtension(
                () -> true,
                () -> {
                    throw new IllegalStateException("MySQL is down");
                }
        );

        @Test
        void body() {
        }
    }
}
