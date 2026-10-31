package io.student.rangiffler.test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.junit.Options;
import com.microsoft.playwright.junit.OptionsFactory;
import io.student.rangiffler.config.TestConfig;

import java.util.Set;

/**
 * Playwright options of the UI tests, taken from {@code TestConfig}, failing fast on an unknown browser.
 */
public final class PlaywrightOptions implements OptionsFactory {

    private static final Set<String> BROWSERS = Set.of("chromium", "firefox", "webkit");

    @Override
    public Options getOptions() {
        if (!BROWSERS.contains(TestConfig.browser())) {
            throw new IllegalArgumentException("Unknown browser: " + TestConfig.browser());
        }
        return new Options()
                .setBrowserName(TestConfig.browser())
                .setHeadless(TestConfig.headless())
                .setContextOptions(new Browser.NewContextOptions().setLocale(TestConfig.browserLocale().toLanguageTag()));
    }
}
