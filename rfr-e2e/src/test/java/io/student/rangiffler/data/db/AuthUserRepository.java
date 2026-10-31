package io.student.rangiffler.data.db;

import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates and removes test users in the rfr-auth database.
 */
public final class AuthUserRepository {

    private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private AuthUserRepository() {
    }

    /**
     * Inserts a user with a hashed password and the read and write authorities in one transaction.
     */
    public static void createUser(String username, String password) {
        Jdbc.inTransaction(Databases.AUTH, () -> {
            Jdbc.client(Databases.AUTH)
                    .sql(Jdbc.sql("sql/auth/insert_user.sql"))
                    .param("username", username)
                    .param("password", PASSWORD_ENCODER.encode(password))
                    .update();
            for (String authority : new String[]{"read", "write"}) {
                Jdbc.client(Databases.AUTH)
                        .sql(Jdbc.sql("sql/auth/insert_authority.sql"))
                        .param("username", username)
                        .param("authority", authority)
                        .update();
            }
        });
    }

    /**
     * Deletes the user and its authorities in one transaction.
     */
    public static void deleteUser(String username) {
        Jdbc.inTransaction(Databases.AUTH, () -> {
            execute("sql/auth/delete_authorities.sql", username);
            execute("sql/auth/delete_user.sql", username);
        });
    }

    /**
     * Runs a SQL file from resources with the {@code username} parameter.
     */
    private static void execute(String sqlPath, String username) {
        Jdbc.client(Databases.AUTH).sql(Jdbc.sql(sqlPath)).param("username", username).update();
    }
}
