package io.student.rangiffler.data;

import io.student.rangiffler.config.TestConfig;

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * A user borrowed from the JVM-wide pool of prepared users, for tests that cannot create a user on the fly; closing it gives the user back and deletes nothing.
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

    private final TestUser user;

    private StaticUserFixture(TestUser user) {
        this.user = user;
    }

    /**
     * Takes a user from the pool, waiting for one to be given back, and fails when none is freed within the configured timeout.
     */
    public static StaticUserFixture create() {
        try {
            TestUser user = POOL.poll(TestConfig.staticUserTimeout().toMillis(), TimeUnit.MILLISECONDS);
            if (user == null) {
                throw new IllegalStateException(
                        "No static user was given back within " + TestConfig.staticUserTimeout().toSeconds() + " seconds"
                );
            }
            return new StaticUserFixture(user);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting for a static user was interrupted", exception);
        }
    }

    @Override
    public TestUser user() {
        return user;
    }

    /**
     * Gives the borrowed user back to the pool and fails if the pool is already full, which means the user was given back twice.
     */
    @Override
    public void close() {
        if (!POOL.offer(user)) {
            throw new IllegalStateException("Static user '" + user.username() + "' was given back to a full pool");
        }
    }
}
