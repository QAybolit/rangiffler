package io.student.rangiffler.data.db;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.output.ValidateOutput;

import javax.sql.DataSource;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Compares the migrations applied to one database schema with the migration files of its backend, without changing the schema.
 */
public final class SchemaMigrations {

    private final DataSource dataSource;
    private final String schema;
    private final Path location;

    public SchemaMigrations(DataSource dataSource, String schema, Path location) {
        this.dataSource = dataSource;
        this.schema = schema;
        this.location = location;
    }

    /**
     * Returns one sentence per difference between the schema and the migration files, and an empty list when they match.
     */
    public List<String> problems() {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schema)
                .locations("filesystem:" + location.toAbsolutePath().normalize())
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .ignoreMigrationPatterns("*:pending", "*:future", "*:missing", "*:ignored")
                .load();
        List<String> problems = new ArrayList<>();
        for (MigrationInfo info : flyway.info().all()) {
            switch (info.getState()) {
                case PENDING -> problems.add(label(info) + " is in the backend but is not applied to the database");
                case FUTURE_SUCCESS, FUTURE_FAILED, MISSING_SUCCESS, MISSING_FAILED ->
                        problems.add(label(info) + " is applied to the database but is absent from the backend");
                case IGNORED, OUT_OF_ORDER -> problems.add(label(info) + " is in the backend but was skipped by the database");
                case FAILED -> problems.add(label(info) + " failed in the database");
                default -> {
                }
            }
        }
        for (ValidateOutput invalid : flyway.validateWithResult().invalidMigrations) {
            problems.add(invalid.errorDetails.errorMessage);
        }
        return problems;
    }

    private static String label(MigrationInfo info) {
        return info.getVersion() == null
                ? "Repeatable migration '" + info.getDescription() + "'"
                : "Migration V" + info.getVersion() + " '" + info.getDescription() + "'";
    }
}
