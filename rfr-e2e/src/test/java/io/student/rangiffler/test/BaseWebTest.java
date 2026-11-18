package io.student.rangiffler.test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for UI tests; the subclass carries {@code @UiTest}, so its Playwright, browser, context and page are owned by {@code @UsePlaywright}.
 */
@ExtendWith({UserExtension.class, SessionExtension.class})
public abstract class BaseWebTest {

    protected Page page;

    /**
     * Keeps the page that Playwright created for the test and sets the waits of actions and of navigations from {@code TestConfig} on every page of its context.
     */
    @BeforeEach
    void init(BrowserContext context, Page page) {
        context.setDefaultTimeout(TestConfig.actionTimeout());
        context.setDefaultNavigationTimeout(TestConfig.navigationTimeout());
        this.page = page;
    }
}
