package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.TestConfig;

import java.util.Set;

/**
 * Playwright options of the UI tests, taken from {@code TestConfig}, failing fast on an unknown browser, with the trace mode from {@code rfr.trace} and the test id attribute fixed to {@code data-test-id}.
 */
public final class PlaywrightOptions implements OptionsFactory {

    private static final Set<String> BROWSERS = Set.of("chromium", "firefox", "webkit");

    private static final String TEST_ID_ATTRIBUTE = "data-test-id";

    @Override
    public Options getOptions() {
        if (!BROWSERS.contains(TestConfig.browser())) {
            throw new IllegalArgumentException("Unknown browser: " + TestConfig.browser());
        }
        return new Options()
                .setBrowserName(TestConfig.browser())
                .setHeadless(TestConfig.headless())
                .setTestIdAttribute(TEST_ID_ATTRIBUTE)
                .setTrace(trace())
                .setContextOptions(contextOptions());
    }

    private static Options.Trace trace() {
        return switch (TestConfig.trace()) {
            case "on" -> Options.Trace.ON;
            case "retain-on-failure" -> Options.Trace.RETAIN_ON_FAILURE;
            default -> Options.Trace.OFF;
        };
    }

    private static Browser.NewContextOptions contextOptions() {
        return new Browser.NewContextOptions()
                .setLocale(TestConfig.browserLocale().toLanguageTag())
                .setViewportSize(TestConfig.viewportWidth(), TestConfig.viewportHeight());
    }
}
