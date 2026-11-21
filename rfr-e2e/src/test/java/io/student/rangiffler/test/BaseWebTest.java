package io.student.rangiffler.test;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import io.student.rangiffler.jupiter.extension.ContextTimeouts;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for UI tests; the subclass carries {@code @UiTest}, so its Playwright, browser, context and page are owned by {@code @UsePlaywright}.
 */
public abstract class BaseWebTest {

    protected Page page;

    /**
     * Keeps the page that Playwright created for the test and sets the waits of its context with {@code ContextTimeouts}.
     */
    @BeforeEach
    void init(BrowserContext context, Page page) {
        ContextTimeouts.apply(context);
        this.page = page;
    }
}
