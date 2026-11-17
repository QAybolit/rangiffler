package io.student.rangiffler.data.fixture;

import io.student.rangiffler.data.TestUser;

/**
 * A test user prepared in the databases before a test and removed after it.
 */
public interface UserFixture extends AutoCloseable {

    /**
     * Returns the prepared user.
     */
    TestUser user();

    /**
     * Removes everything the fixture created.
     */
    @Override
    void close();
}
