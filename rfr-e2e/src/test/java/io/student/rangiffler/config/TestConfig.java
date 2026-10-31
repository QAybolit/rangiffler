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
    private static final Locale BROWSER_LOCALE = Locale.forLanguageTag(value("browserLocale", "en-US"));

    private static final Duration STATIC_USER_TIMEOUT = Duration.ofSeconds(Long.parseLong(value("staticUserTimeout", "30")));

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

    public static Locale browserLocale() {
        return BROWSER_LOCALE;
    }

    public static Duration staticUserTimeout() {
        return STATIC_USER_TIMEOUT;
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
