package io.student.rangiffler.data.db;

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
     * Creates the friendship between two existing profiles.
     */
    public static void createFriendship(Friendship friendship) {
        Jdbc.inTransaction(Databases.API, () -> Jdbc.requireOneRow(
                Jdbc.client(Databases.API)
                        .sql(Jdbc.sql("sql/api/create_friendship.sql"))
                        .param("requester", friendship.requester())
                        .param("addressee", friendship.addressee())
                        .param("status", friendship.status().name())
                        .update(),
                "create friendship '" + friendship.requester() + "' -> '" + friendship.addressee() + "'"
        ));
    }

    /**
     * Tells whether a profile with the username exists.
     */
    public static boolean userExists(String username) {
        return Jdbc.client(Databases.API)
                .sql(Jdbc.sql("sql/api/exists_user.sql"))
                .param("username", username)
                .query(Integer.class)
                .single() > 0;
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
