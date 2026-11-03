package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.extension.AllureEnvironmentExtension;
import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
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
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample classes through the JUnit engine to check that {@link AllureEnvironmentExtension} leaves {@code environment.properties} in the results directory, also when MySQL is unreachable or another suite extension failed before it.
 */
@DisplayName("AllureEnvironmentExtension writes the environment of the run")
@Tag("extension")
class AllureEnvironmentExtensionTest {

    @Test
    @DisplayName("After the run the results directory has environment.properties with every key")
    void shouldWriteEveryKey(@TempDir Path results) throws IOException {
        run(results, Plain.class);

        assertEquals(
                Set.of("frontUrl", "authUrl", "apiUrl", "dbUrl", "browser", "headless", "commit", "authSchemaVersion", "apiSchemaVersion"),
                read(results).keySet(),
                "The file is missing or does not have every key of the environment"
        );
    }

    @Test
    @DisplayName("The file carries the schema versions of the extension")
    void shouldWriteSchemaVersions(@TempDir Path results) throws IOException {
        run(results, Plain.class);
        Properties environment = read(results);

        assertEquals(
                List.of("1", "2"),
                List.of(environment.getProperty("authSchemaVersion"), environment.getProperty("apiSchemaVersion")),
                "The schema versions of the extension are not in the file"
        );
    }

    @Test
    @DisplayName("Schema versions that cannot be read are marked unavailable and the file is still written")
    void shouldMarkUnreadableVersionsUnavailable(@TempDir Path results) throws IOException {
        run(results, Unreachable.class);
        Properties environment = read(results);

        assertEquals(
                List.of("unavailable", "unavailable"),
                List.of(environment.getProperty("authSchemaVersion"), environment.getProperty("apiSchemaVersion")),
                "A schema version that cannot be read was not marked unavailable"
        );
    }

    @Test
    @DisplayName("The file is written when a suite extension registered after it fails to start")
    void shouldWriteFileWhenLaterExtensionFails(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, FirstThenFailing.class);

        assertEquals(
                List.of(true, List.of("Sample failure of beforeSuite")),
                List.of(Files.exists(results.resolve("environment.properties")), failures(outcome)),
                "The file was not written after a later extension failed, or the sample did not fail as expected"
        );
    }

    @Test
    @DisplayName("The file is not written when a suite extension registered before it fails to start")
    void shouldNotWriteFileWhenEarlierExtensionFails(@TempDir Path results) {
        EngineExecutionResults outcome = run(results, FailingThenLast.class);

        assertEquals(
                List.of(false, List.of("Sample failure of beforeSuite")),
                List.of(Files.exists(results.resolve("environment.properties")), failures(outcome)),
                "The file was written although the extension never started, or the sample did not fail as expected"
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

    private static AllureEnvironmentExtension writing(String auth, String api) {
        return new AllureEnvironmentExtension(
                context -> Path.of(context.getConfigurationParameter("probe.results").orElseThrow()),
                () -> auth,
                () -> api
        );
    }

    /**
     * Suite extension whose beforeSuite always fails.
     */
    static final class Failing implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            throw new IllegalStateException("Sample failure of beforeSuite");
        }
    }

    /**
     * Sample class whose schema versions are known.
     */
    @ProbeTarget
    static class Plain {

        @RegisterExtension
        static final AllureEnvironmentExtension ALLURE = writing("1", "2");

        @Test
        void body() {
        }
    }

    /**
     * Sample class whose schema versions cannot be read.
     */
    @ProbeTarget
    static class Unreachable {

        @RegisterExtension
        static final AllureEnvironmentExtension ALLURE = new AllureEnvironmentExtension(
                context -> Path.of(context.getConfigurationParameter("probe.results").orElseThrow()),
                () -> {
                    throw new IllegalStateException("MySQL is down");
                },
                () -> {
                    throw new IllegalStateException("MySQL is down");
                }
        );

        @Test
        void body() {
        }
    }

    /**
     * Sample class where the environment extension comes first and another extension fails.
     */
    @ProbeTarget
    static class FirstThenFailing {

        @Order(1)
        @RegisterExtension
        static final AllureEnvironmentExtension ALLURE = writing("1", "2");

        @Order(2)
        @RegisterExtension
        static final Failing FAILING = new Failing();

        @Test
        void body() {
        }
    }

    /**
     * Sample class where another extension fails before the environment extension.
     */
    @ProbeTarget
    static class FailingThenLast {

        @Order(1)
        @RegisterExtension
        static final Failing FAILING = new Failing();

        @Order(2)
        @RegisterExtension
        static final AllureEnvironmentExtension ALLURE = writing("1", "2");

        @Test
        void body() {
        }
    }
}
