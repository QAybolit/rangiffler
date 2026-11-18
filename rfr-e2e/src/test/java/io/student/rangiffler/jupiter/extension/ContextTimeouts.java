package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import io.student.rangiffler.config.TestConfig;

/**
 * The waits of actions and of navigations that every browser context of the UI tests gets from {@code TestConfig}.
 */
public final class ContextTimeouts {

    private ContextTimeouts() {
    }

    /**
     * Sets the waits on every page of the context, present and future.
     */
    public static void apply(BrowserContext context) {
        context.setDefaultTimeout(TestConfig.actionTimeout());
        context.setDefaultNavigationTimeout(TestConfig.navigationTimeout());
    }
}
