package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;

/**
 * Removes test users from the rfr-api database.
 */
public final class ApiUserRepository {

    private ApiUserRepository() {
    }

    /**
     * Deletes the user profile (created lazily by rfr-api on first login) together with its dependent rows.
     */
    public static void deleteUser(String username) {
        Jdbc.inTransaction(Databases.API, () -> {
            execute("sql/api/delete_photo_likes.sql", username);
            execute("sql/api/delete_likes.sql", username);
            execute("sql/api/delete_photos.sql", username);
            execute("sql/api/delete_statistics.sql", username);
            execute("sql/api/delete_friendships.sql", username);
            execute("sql/api/delete_user.sql", username);
        });
    }

    /**
     * Runs a SQL file from resources with the {@code username} parameter.
     */
    private static void execute(String sqlPath, String username) {
        Jdbc.client(Databases.API).sql(Jdbc.sql(sqlPath)).param("username", username).update();
    }
}
