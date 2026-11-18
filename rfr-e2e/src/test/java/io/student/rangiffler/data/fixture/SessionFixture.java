package io.student.rangiffler.data.fixture;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;

/**
 * Owns an additional browser context of a test and its page, and closes the context with everything in it.
 */
public final class SessionFixture implements AutoCloseable {

    private final BrowserContext context;
    private final Page page;

    private SessionFixture(BrowserContext context, Page page) {
        this.context = context;
        this.page = page;
    }

    /**
     * Opens a page in the context and closes the context when the page cannot be opened.
     */
    public static SessionFixture create(BrowserContext context) {
        try {
            return new SessionFixture(context, context.newPage());
        } catch (RuntimeException | Error e) {
            try {
                context.close();
            } catch (RuntimeException | Error closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    public Page page() {
        return page;
    }

    /**
     * Closes the context, which closes its page.
     */
    @Override
    public void close() {
        context.close();
    }
}
