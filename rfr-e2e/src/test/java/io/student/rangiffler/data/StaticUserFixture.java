package io.student.rangiffler.data;

import io.student.rangiffler.config.TestConfig;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

/**
 * A user borrowed from the JVM-wide pool, for tests that cannot create a user on the fly; the pool holds agreed username and password pairs that no code creates in any database, and closing the fixture gives the user back once and deletes nothing.
 */
public final class StaticUserFixture implements UserFixture {

    private static final int POOL_SIZE = 3;

    private static final BlockingQueue<TestUser> POOL = new ArrayBlockingQueue<>(
            POOL_SIZE,
            true,
            IntStream.rangeClosed(1, POOL_SIZE)
                    .mapToObj(number -> new TestUser("static-user-" + number, "static-secret", List.of(), List.of(), List.of()))
                    .toList()
    );

    private final AtomicReference<TestUser> held;

    private StaticUserFixture(TestUser user) {
        this.held = new AtomicReference<>(user);
    }

    /**
     * Takes a user from the pool, waiting for one to be given back, and fails when none is freed within the configured timeout.
     */
    public static StaticUserFixture create() {
        Duration timeout = TestConfig.staticUserTimeout();
        try {
            TestUser user = POOL.poll(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (user == null) {
                throw new IllegalStateException(
                        "No static user was given back within " + timeout.toSeconds() + " seconds"
                );
            }
            return new StaticUserFixture(user);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting for a static user was interrupted", exception);
        }
    }

    /**
     * Returns the borrowed user and fails once the fixture is closed, because the user may already belong to another test.
     */
    @Override
    public TestUser user() {
        TestUser user = held.get();
        if (user == null) {
            throw new IllegalStateException("Static user was already given back");
        }
        return user;
    }

    /**
     * Gives the borrowed user back to the pool exactly once; a second call fails and never touches the pool.
     */
    @Override
    public void close() {
        TestUser user = held.getAndSet(null);
        if (user == null) {
            throw new IllegalStateException("Static user was already given back");
        }
        POOL.add(user);
    }
}
