package io.student.rangiffler.jupiter.extension;

import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Tracing;
import io.qameta.allure.Allure;
import io.qameta.allure.AttachmentOptions;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Records the Playwright trace of every UI test and keeps {@code trace.zip}, attached to the Allure report, only for the tests that failed; the browser context is the one that Playwright has resolved for the {@code @BeforeEach} method of the test.
 */
public final class TraceExtension implements InvocationInterceptor, AfterEachCallback {

    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(TraceExtension.class);

    /**
     * Starts the trace before the first {@code @BeforeEach} method that takes a {@code BrowserContext}, so its steps are recorded too.
     */
    @Override
    public void interceptBeforeEachMethod(
            Invocation<Void> invocation,
            ReflectiveInvocationContext<Method> invocationContext,
            ExtensionContext extensionContext
    ) throws Throwable {
        ExtensionContext.Store store = extensionContext.getStore(NAMESPACE);
        if (store.get(BrowserContext.class) == null) {
            invocationContext.getArguments().stream()
                    .filter(BrowserContext.class::isInstance)
                    .map(BrowserContext.class::cast)
                    .findFirst()
                    .ifPresent(context -> {
                        context.tracing().start(new Tracing.StartOptions()
                                .setScreenshots(true)
                                .setSnapshots(true)
                                .setSources(true)
                                .setTitle(extensionContext.getDisplayName()));
                        store.put(BrowserContext.class, context);
                    });
        }
        invocation.proceed();
    }

    /**
     * Runs after the test and its {@code @AfterEach} methods but before Playwright closes the context, so the trace can still be stopped; a test that passed discards it. A test without a started trace fails here only if it passed, so the real cause of an earlier failure, such as a user that was not created, is not hidden.
     */
    @Override
    public void afterEach(ExtensionContext extensionContext) {
        BrowserContext context = extensionContext.getStore(NAMESPACE).get(BrowserContext.class, BrowserContext.class);
        if (context == null) {
            if (extensionContext.getExecutionException().isPresent()) {
                return;
            }
            throw new IllegalStateException(
                    "No @BeforeEach method of " + extensionContext.getRequiredTestClass().getName()
                            + " takes a BrowserContext, so the trace of the test was not recorded"
            );
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
