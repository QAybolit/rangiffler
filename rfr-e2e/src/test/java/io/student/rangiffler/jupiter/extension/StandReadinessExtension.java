package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.jupiter.stand.StandReadiness;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Makes the run wait for the stand before the first UI test class, so a stand that is still coming up or is switched off ends with a clear error instead of a Playwright timeout.
 */
public final class StandReadinessExtension implements SuiteExtension {

    private final Predicate<ExtensionContext> enabled;
    private final Function<ExtensionContext, StandReadiness> readiness;

    /**
     * Creates the extension that {@code ServiceLoader} registers: it takes its settings from {@code TestConfig}.
     */
    public StandReadinessExtension() {
        this(context -> TestConfig.standCheck(), context -> new StandReadiness());
    }

    /**
     * Creates the extension with its own settings, so that a probe can point it to a fake stand.
     */
    public StandReadinessExtension(Predicate<ExtensionContext> enabled, Function<ExtensionContext, StandReadiness> readiness) {
        this.enabled = enabled;
        this.readiness = readiness;
    }

    /**
     * Applies to test classes tagged {@code ui}, the only ones that use the services of the stand.
     */
    @Override
    public boolean appliesTo(ExtensionContext context) {
        return context.getTags().contains("ui");
    }

    /**
     * Waits for the stand unless the check is switched off.
     */
    @Override
    public void beforeSuite(ExtensionContext context) {
        if (enabled.test(context)) {
            readiness.apply(context).await();
        }
    }
}
