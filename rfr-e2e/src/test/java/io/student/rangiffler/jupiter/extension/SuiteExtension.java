package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Extension of the whole test run: beforeSuite runs once before the first test class and afterSuite once after the last one, however many classes use the extension.
 */
public interface SuiteExtension extends BeforeAllCallback {

    /**
     * Registers the run in the root store before beforeSuite is called, so the first class starts it, the others only repeat its failure, and the run closes in every case.
     */
    @Override
    default void beforeAll(ExtensionContext context) {
        ExtensionContext root = context.getRoot();
        SuiteRun run = root.getStore(ExtensionContext.Namespace.GLOBAL)
                .computeIfAbsent(getClass(), key -> new SuiteRun(this, root), SuiteRun.class);
        run.start();
        run.check();
    }

    default void beforeSuite(ExtensionContext context) {
    }

    default void afterSuite(ExtensionContext context) {
    }
}
