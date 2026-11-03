package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.SchemaMigrations;
import io.student.rangiffler.jupiter.allure.EnvironmentFile;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Writes the environment of the run into the Allure results after the last test, even when the run failed before its tests, and marks a schema version it cannot read as unavailable.
 */
public final class AllureEnvironmentExtension implements SuiteExtension {

    private final Function<ExtensionContext, Path> results;
    private final Supplier<String> authVersion;
    private final Supplier<String> apiVersion;

    /**
     * Creates the extension that {@code ServiceLoader} registers: it takes its settings from {@code TestConfig} and reads the schema versions with a short connection timeout, so a switched-off MySQL does not hold up the end of the run.
     */
    public AllureEnvironmentExtension() {
        this(
                context -> TestConfig.allureResults(),
                () -> version("rangiffler-auth", TestConfig.authMigrations()),
                () -> version("rangiffler-api", TestConfig.apiMigrations())
        );
    }

    /**
     * Creates the extension with its own results directory and schema versions, so that a probe can look at the file in a temporary directory.
     */
    public AllureEnvironmentExtension(Function<ExtensionContext, Path> results, Supplier<String> authVersion, Supplier<String> apiVersion) {
        this.results = results;
        this.authVersion = authVersion;
        this.apiVersion = apiVersion;
    }

    /**
     * Writes the file; a failure to write it is reported on the error stream and does not fail the run, because the file is only a report.
     */
    @Override
    public void afterSuite(ExtensionContext context) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("frontUrl", TestConfig.frontUrl());
        values.put("authUrl", TestConfig.authUrl());
        values.put("apiUrl", TestConfig.apiUrl());
        values.put("dbUrl", TestConfig.dbUrl());
        values.put("browser", TestConfig.browser());
        values.put("headless", String.valueOf(TestConfig.headless()));
        values.put("commit", TestConfig.commit());
        values.put("authSchemaVersion", safely(authVersion));
        values.put("apiSchemaVersion", safely(apiVersion));
        try {
            new EnvironmentFile(results.apply(context)).write(values);
        } catch (RuntimeException exception) {
            System.err.println("Allure environment was not written: " + exception.getMessage());
        }
    }

    private static String safely(Supplier<String> version) {
        try {
            return version.get();
        } catch (Throwable unreadable) {
            return "unavailable";
        }
    }

    private static String version(String schema, String migrations) {
        DriverManagerDataSource source = new DriverManagerDataSource(
                TestConfig.dbUrl() + "/" + schema + "?connectTimeout=2000&socketTimeout=5000",
                TestConfig.dbUser(),
                TestConfig.dbPassword()
        );
        return new SchemaMigrations(source, schema, Path.of(migrations)).version();
    }
}
