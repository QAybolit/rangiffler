package io.student.rangiffler.config;

import java.util.Locale;

/**
 * Test settings read from system properties with local defaults.
 */
public final class TestConfig {

    private static final String FRONT_URL = value("front.url", "http://localhost:3001");
    private static final String AUTH_URL = value("auth.url", "http://localhost:9001");
    private static final String AUTH_DB_URL = value("auth.db.url", "jdbc:mysql://localhost:3306/rangiffler-auth");
    private static final String API_DB_URL = value("api.db.url", "jdbc:mysql://localhost:3306/rangiffler-api");
    private static final Locale BROWSER_LOCALE = Locale.forLanguageTag(value("browser.locale", "en-US"));
    private static final String DB_USER = value("db.user", "root");
    private static final String DB_PASSWORD = value("db.password", "secret");

    private TestConfig() {
    }

    public static String frontUrl() {
        return FRONT_URL;
    }

    public static String authUrl() {
        return AUTH_URL;
    }

    public static String authDbUrl() {
        return AUTH_DB_URL;
    }

    public static String apiDbUrl() {
        return API_DB_URL;
    }

    public static Locale browserLocale() {
        return BROWSER_LOCALE;
    }

    public static String dbUser() {
        return DB_USER;
    }

    public static String dbPassword() {
        return DB_PASSWORD;
    }

    /**
     * Returns the system property {@code key}, or {@code defaultValue} when it is not set.
     */
    private static String value(String key, String defaultValue) {
        return System.getProperty(key, defaultValue);
    }
}
