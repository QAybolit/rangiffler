package io.student.rangiffler.test;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base class for UI tests whose Playwright, browser, context and page are owned by {@code @UsePlaywright}.
 */
@ExtendWith(UserExtension.class)
@UsePlaywright(PlaywrightOptions.class)
public abstract class BaseWebTest {

    protected Page page;

    /**
     * Keeps the page that Playwright created for the test.
     */
    @BeforeEach
    void init(Page page) {
        this.page = page;
    }
}
