package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.SchemaMigrations;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Stops the test run before any test when the local database schemas were migrated by other backend code than the one in this checkout.
 */
public final class MigrationCheckExtension implements SuiteExtension {

    private final BooleanSupplier enabled;
    private final Supplier<List<String>> problems;

    /**
     * Creates the extension that {@code ServiceLoader} registers: it checks the schemas of the local database against the migrations of this checkout.
     */
    public MigrationCheckExtension() {
        this(TestConfig::migrationCheck, MigrationCheckExtension::differences);
    }

    /**
     * Creates the extension with its own switch and its own differences, so that a probe can break the schema without a database.
     */
    public MigrationCheckExtension(BooleanSupplier enabled, Supplier<List<String>> problems) {
        this.enabled = enabled;
        this.problems = problems;
    }

    /**
     * Checks both schemas and fails with the differences and a way out; it does nothing unless the check is enabled.
     */
    @Override
    public void beforeSuite(ExtensionContext context) {
        if (!enabled.getAsBoolean()) {
            return;
        }
        List<String> found = problems.get();
        if (!found.isEmpty()) {
            throw new IllegalStateException(
                    "The database schema does not match the migrations of the backends:\n  - " + String.join("\n  - ", found)
                            + "\nSwitch rfr-auth and rfr-api to the branch that migrated the database, or recreate the database"
                            + " ('docker compose down -v' removes the volume mysql-volume, then 'bash localenv.sh'),"
                            + " and restart rfr-auth and rfr-api so that they migrate it again"
            );
        }
    }

    private static List<String> differences() {
        List<String> found = new ArrayList<>();
        new SchemaMigrations(Databases.AUTH.dataSource(), "rangiffler-auth", Path.of(TestConfig.authMigrations()))
                .problems().forEach(problem -> found.add("rangiffler-auth: " + problem));
        new SchemaMigrations(Databases.API.dataSource(), "rangiffler-api", Path.of(TestConfig.apiMigrations()))
                .problems().forEach(problem -> found.add("rangiffler-api: " + problem));
        return found;
    }
}
