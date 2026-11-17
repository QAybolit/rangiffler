package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import com.microsoft.playwright.impl.junit.BrowserContextExtension;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Records the Playwright trace of every UI test and keeps {@code trace.zip}, attached to the Allure report, only for the tests that failed; the browser context is the one that {@code BrowserContextExtension} of Playwright creates for the test.
 */
public final class TraceExtension implements BeforeEachCallback, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TraceExtension.class);

    /**
     * Starts the trace before the {@code @BeforeEach} methods run, so their steps are recorded too.
     */
    @Override
    public void beforeEach(ExtensionContext extensionContext) {
        BrowserContext context = BrowserContextExtension.getOrCreateBrowserContext(extensionContext);
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true)
                .setTitle(extensionContext.getDisplayName()));
        extensionContext.getStore(NAMESPACE).put(BrowserContext.class, context);
    }

    /**
     * Runs after the test and its {@code @AfterEach} methods but before Playwright closes the context, so the trace can still be stopped; a test that passed discards it, and a test whose trace did not start, because an earlier extension failed first, has nothing to stop.
     */
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        BrowserContext context = extensionContext.getStore(NAMESPACE).get(BrowserContext.class, BrowserContext.class);
        if (context == null) {
            return;
        }
        if (extensionContext.getExecutionException().isEmpty()) {
            context.tracing().stop();
            return;
        }
        Path file = Path.of(
                "test-results",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName(),
                "trace.zip"
        );
        try {
            Files.createDirectories(file.getParent());
            context.tracing().stop(new Tracing.StopOptions().setPath(file));
            try (InputStream content = Files.newInputStream(file)) {
                Allure.attachment("trace.zip", "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save the trace of the failed test to " + file, exception);
        }
    }
}
