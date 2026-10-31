package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Databases;
import io.student.rangiffler.data.db.Jdbc;

/**
 * Creates and removes test users in the rfr-api database.
 */
public final class ApiUserRepository {

    private ApiUserRepository() {
    }

    /**
     * Creates the user profile with the default country, as rfr-api does lazily on first login.
     */
    public static void createProfile(String username) {
        Jdbc.inTransaction(Databases.API, () -> Jdbc.requireOneRow(
                Jdbc.client(Databases.API)
                        .sql(Jdbc.sql("sql/api/create_profile.sql"))
                        .param("username", username)
                        .update(),
                "create profile '" + username + "'"
        ));
    }

    /**
     * Creates a friendship between two existing profiles; the status is {@code ACCEPTED} or {@code PENDING}.
     */
    public static void createFriendship(String requester, String addressee, String status) {
        Jdbc.inTransaction(Databases.API, () -> Jdbc.requireOneRow(
                Jdbc.client(Databases.API)
                        .sql(Jdbc.sql("sql/api/create_friendship.sql"))
                        .param("requester", requester)
                        .param("addressee", addressee)
                        .param("status", status)
                        .update(),
                "create friendship '" + requester + "' -> '" + addressee + "'"
        ));
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
