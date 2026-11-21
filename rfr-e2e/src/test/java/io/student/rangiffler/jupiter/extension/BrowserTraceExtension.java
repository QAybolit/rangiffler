package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Records the Playwright trace of the browser context of every UI test and keeps {@code trace.zip}, attached to the Allure report, only for the tests that failed; the context is the one that {@code BrowserContextExtension} of Playwright creates for the test.
 */
public final class BrowserTraceExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(BrowserTraceExtension.class);

    /**
     * Starts the trace before the {@code @BeforeEach} methods run, so their steps are recorded too.
     */
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        extensionContext.getStore(NAMESPACE).put(
                ContextTrace.class,
                ContextTrace.started(BrowserContextExtension.getOrCreateBrowserContext(extensionContext), "trace.zip", extensionContext.getDisplayName())
        );
    }

    /**
     * Runs after the test and its {@code @AfterEach} methods but before Playwright closes the context, so the trace can still be stopped; a test whose trace did not start, because an earlier extension failed first, has nothing to stop.
     */
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        ContextTrace trace = extensionContext.getStore(NAMESPACE).get(ContextTrace.class, ContextTrace.class);
        if (trace != null) {
            trace.finish(extensionContext);
        }
    }
}
