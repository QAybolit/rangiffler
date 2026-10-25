package io.student.rangiffler.test;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.annotation.BrowserLocale;
import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.support.AnnotationSupport;

import java.util.Locale;

/**
 * Base class for UI tests that starts a browser per test with the locale chosen by {@code @BrowserLocale}.
 */
@Tag("ui")
@ExtendWith(UserExtension.class)
public abstract class BaseWebTest {

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    protected Page page;

    /**
     * Starts Playwright, a browser, a context with the test locale and a page.
     */
    @BeforeEach
    void setUp(TestInfo testInfo) {
        playwright = Playwright.create();
        browser = browserType().launch(new BrowserType.LaunchOptions().setHeadless(TestConfig.headless()));
        Locale locale = locale(testInfo);
        context = browser.newContext(new Browser.NewContextOptions().setLocale(locale.toLanguageTag()));
        page = context.newPage();
    }

    /**
     * Closes the browser objects that {@code setUp} managed to create.
     */
    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
        if (browser != null) {
            browser.close();
        }
        if (playwright != null) {
            playwright.close();
        }
    }

    /**
     * Returns the locale from the method, then the class, then the configured default.
     */
    private static Locale locale(TestInfo testInfo) {
        return AnnotationSupport.findAnnotation(testInfo.getTestMethod(), BrowserLocale.class)
                .or(() -> AnnotationSupport.findAnnotation(testInfo.getTestClass(), BrowserLocale.class))
                .map(annotation -> Locale.forLanguageTag(annotation.value()))
                .orElseGet(TestConfig::browserLocale);
    }

    /**
     * Returns the Playwright browser type named by the configured browser, failing fast on an unknown name.
     */
    private BrowserType browserType() {
        return switch (TestConfig.browser()) {
            case "chromium" -> playwright.chromium();
            case "firefox" -> playwright.firefox();
            case "webkit" -> playwright.webkit();
            default -> throw new IllegalArgumentException("Unknown browser: " + TestConfig.browser());
        };
    }
}
