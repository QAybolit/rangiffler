package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The Playwright trace of one browser context of a test: it is recorded for every test and kept, with an Allure attachment, only when the test failed.
 */
final class ContextTrace {

    private final BrowserContext context;
    private final String file;

    private ContextTrace(BrowserContext context, String file) {
        this.context = context;
        this.file = file;
    }

    /**
     * Starts the trace of the context and returns it only when it has started, so a trace that is kept can always be stopped.
     */
    static ContextTrace started(BrowserContext context, String file, String title) {
        context.tracing().start(new Tracing.StartOptions()
                .setScreenshots(true)
                .setSnapshots(true)
                .setSources(true)
                .setTitle(title));
        return new ContextTrace(context, file);
    }

    /**
     * Stops the trace and discards it when the test passed; otherwise saves it as {@code file} in the folder of the test and attaches it to the Allure report.
     */
    void finish(ExtensionContext extensionContext) {
        if (extensionContext.getExecutionException().isEmpty()) {
            context.tracing().stop();
            return;
        }
        Path path = Path.of(
                "test-results",
                extensionContext.getRequiredTestClass().getSimpleName(),
                extensionContext.getRequiredTestMethod().getName(),
                file
        );
        try {
            Files.createDirectories(path.getParent());
            context.tracing().stop(new Tracing.StopOptions().setPath(path));
            try (InputStream content = Files.newInputStream(path)) {
                Allure.attachment(file, "application/zip", content, AttachmentOptions.withFileExtension(".zip"));
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot save the trace of the failed test to " + path, exception);
        }
    }
}
