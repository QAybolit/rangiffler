package io.student.rangiffler.config;

import java.time.Duration;
import java.util.Locale;

/**
 * Test settings read from {@code -Drfr.<key>}, then from the environment variable {@code RFR_<KEY>}, then from a local default.
 */
public final class TestConfig {

    private static final String FRONT_URL = value("frontUrl", "http://localhost:3001");
    private static final String AUTH_URL = value("authUrl", "http://localhost:9001");
    private static final String API_URL = value("apiUrl", "http://localhost:8081");
    private static final String DB_URL = value("dbUrl", "jdbc:mysql://localhost:3306");
    private static final String DB_USER = value("dbUser", "root");
    private static final String DB_PASSWORD = value("dbPassword", "secret");
    private static final String BROWSER = value("browser", "chromium");
    private static final boolean HEADLESS = Boolean.parseBoolean(value("headless", "true"));
    private static final boolean STAND_CHECK = Boolean.parseBoolean(value("standCheck", "true"));
    private static final boolean MIGRATION_CHECK = Boolean.parseBoolean(value("migrationCheck", "false"));
    private static final String AUTH_MIGRATIONS = value("authMigrations", "../rfr-auth/src/main/resources/db/migration/rangiffler-auth");
    private static final String API_MIGRATIONS = value("apiMigrations", "../rfr-api/src/main/resources/db/migration/rangiffler-api");
    private static final Locale BROWSER_LOCALE = Locale.forLanguageTag(value("browserLocale", "en-US"));

    private TestConfig() {
    }

    public static String frontUrl() {
        return FRONT_URL;
    }

    public static String authUrl() {
        return AUTH_URL;
    }

    public static String apiUrl() {
        return API_URL;
    }

    public static String dbUrl() {
        return DB_URL;
    }

    public static String dbUser() {
        return DB_USER;
    }

    public static String dbPassword() {
        return DB_PASSWORD;
    }

    public static String browser() {
        return BROWSER;
    }

    public static boolean headless() {
        return HEADLESS;
    }

    public static boolean migrationCheck() {
        return MIGRATION_CHECK;
    }

    public static String authMigrations() {
        return AUTH_MIGRATIONS;
    }

    public static String apiMigrations() {
        return API_MIGRATIONS;
    }

    public static Locale browserLocale() {
        return BROWSER_LOCALE;
    }

    /**
     * Returns how long to wait for a free pooled user; parsed on demand so a bad value breaks only the tests that use the pool.
     */
    public static Duration staticUserTimeout() {
        return seconds("staticUserTimeout", "30");
    }

    /**
     * Returns how long to wait for the stand to come up; parsed on demand so a bad value breaks only the runs that wait for the stand.
     */
    public static Duration standTimeout() {
        return seconds("standTimeout", "180");
    }

    public static boolean standCheck() {
        return STAND_CHECK;
    }

    private static Duration seconds(String key, String defaultValue) {
        String raw = value(key, defaultValue);
        long seconds;
        try {
            seconds = Long.parseLong(raw);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("rfr." + key + " must be a number of seconds, got '" + raw + "'", exception);
        }
        if (seconds < 1 || seconds > 3600) {
            throw new IllegalArgumentException("rfr." + key + " must be from 1 to 3600 seconds, got " + seconds);
        }
        return Duration.ofSeconds(seconds);
    }

    /**
     * Returns the system property {@code rfr.<key>}, else the variable {@code RFR_<KEY>} in upper snake case (for example {@code RFR_FRONT_URL}), else {@code defaultValue}.
     */
    private static String value(String key, String defaultValue) {
        String property = System.getProperty("rfr." + key);
        if (property != null) {
            return property;
        }
        String variable = System.getenv("RFR_" + key.replaceAll("([A-Z])", "_$1").toUpperCase(Locale.ROOT));
        return variable != null ? variable : defaultValue;
    }
}
