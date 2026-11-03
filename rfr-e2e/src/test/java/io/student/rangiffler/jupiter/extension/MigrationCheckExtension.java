package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.SchemaMigrations;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Stops the test run before any test when the local database schemas were migrated by other backend code than the one in this checkout.
 */
public final class MigrationCheckExtension implements SuiteExtension {

    /**
     * Checks both schemas and fails with the differences and a way out; it does nothing unless {@code rfr.migrationCheck} is true.
     */
    @Override
    public void beforeSuite(ExtensionContext context) {
        if (!TestConfig.migrationCheck()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        new SchemaMigrations(Databases.AUTH.dataSource(), "rangiffler-auth", Path.of(TestConfig.authMigrations()))
                .problems().forEach(problem -> problems.add("rangiffler-auth: " + problem));
        new SchemaMigrations(Databases.API.dataSource(), "rangiffler-api", Path.of(TestConfig.apiMigrations()))
                .problems().forEach(problem -> problems.add("rangiffler-api: " + problem));
        if (!problems.isEmpty()) {
            throw new IllegalStateException(
                    "The database schema does not match the migrations of the backends:\n  - " + String.join("\n  - ", problems)
                            + "\nSwitch rfr-auth and rfr-api to the branch that migrated the database, or recreate the database"
                            + " ('docker compose down -v' removes the volume mysql-volume, then 'bash localenv.sh'),"
                            + " and restart rfr-auth and rfr-api so that they migrate it again"
            );
        }
    }
}
