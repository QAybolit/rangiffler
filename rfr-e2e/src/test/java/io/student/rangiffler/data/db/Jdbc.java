package io.student.rangiffler.data.db;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * JDBC helpers: clients, transactions and loading SQL from resources.
 */
public final class Jdbc {

    private static final Map<Databases, JdbcClient> CLIENTS = new ConcurrentHashMap<>();

    private Jdbc() {
    }

    /**
     * Returns a cached {@code JdbcClient} for the database.
     */
    public static JdbcClient client(Databases database) {
        return CLIENTS.computeIfAbsent(database, db -> JdbcClient.create(db.dataSource()));
    }

    /**
     * Runs the action in a transaction and returns its result.
     */
    public static <T> T inTransaction(Databases database, Supplier<T> action) {
        return new TransactionTemplate(database.transactionManager()).execute(status -> action.get());
    }

    /**
     * Runs the action in a transaction without a result.
     */
    public static void inTransaction(Databases database, Runnable action) {
        new TransactionTemplate(database.transactionManager()).executeWithoutResult(status -> action.run());
    }

    /**
     * Loads SQL text from a classpath resource such as {@code sql/auth/insert_user.sql}.
     */
    public static String sql(String resourcePath) {
        try (InputStream stream = Jdbc.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalArgumentException("SQL resource not found: " + resourcePath);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read SQL resource: " + resourcePath, e);
        }
    }
}
