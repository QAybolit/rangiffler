package io.student.rangiffler.jupiter.extension;

import org.junit.jupiter.api.extension.ExtensionContext;

import static org.junit.platform.commons.util.ExceptionUtils.throwAsUncheckedException;

/**
 * State of one suite extension within one test run: it is stored before beforeSuite is called, so a failure of beforeSuite is kept, repeated for every class and reported again when the run closes.
 */
public final class SuiteRun implements AutoCloseable {

    private final SuiteExtension extension;

    private final ExtensionContext root;

    private boolean started;

    private Throwable failure;

    public SuiteRun(SuiteExtension extension, ExtensionContext root) {
        this.extension = extension;
        this.root = root;
    }

    /**
     * Calls beforeSuite on the first call only and keeps its failure, so that later calls never repeat it.
     */
    public synchronized void start() {
        if (!started) {
            started = true;
            try {
                extension.beforeSuite(root);
            } catch (Throwable thrown) {
                failure = thrown;
            }
        }
    }

    /**
     * Throws the failure of beforeSuite, if there was one, so that every class of the run fails with the same cause.
     */
    public synchronized void check() {
        if (failure != null) {
            throwAsUncheckedException(failure);
        }
    }

    /**
     * Calls afterSuite when beforeSuite succeeded, and throws the failure of beforeSuite otherwise, because nothing was taken that afterSuite could give back.
     */
    @Override
    public synchronized void close() {
        if (started) {
            if (failure != null) {
                throwAsUncheckedException(failure);
            }
            extension.afterSuite(root);
        }
    }
}
