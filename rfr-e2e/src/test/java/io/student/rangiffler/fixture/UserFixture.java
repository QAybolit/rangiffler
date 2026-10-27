package io.student.rangiffler.fixture;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.ApiUserRepository;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.model.TestUser;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

/**
 * Owns a test user's lifecycle: creates it in the database and removes it on close.
 */
public final class UserFixture implements AutoCloseable {

    private final TestUser user = new TestUser(randomUsername(), randomPassword());

    private UserFixture() {
    }

    /**
     * Creates a random user in the auth database and cleans up after itself if that fails.
     */
    public static UserFixture create() {
        UserFixture fixture = new UserFixture();
        try {
            Allure.step("Create user '" + fixture.user.username() + "' in auth DB",
                    () -> AuthUserRepository.createUser(fixture.user.username(), fixture.user.password()));
        } catch (RuntimeException | Error e) {
            try {
                fixture.close();
            } catch (RuntimeException | Error closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
        return fixture;
    }

    public TestUser user() {
        return user;
    }

    /**
     * Removes the user from the api and auth databases.
     */
    @Override
    public void close() {
        try {
            Allure.step("Delete user '" + user.username() + "' from api DB",
                    () -> ApiUserRepository.deleteUser(user.username()));
        } finally {
            Allure.step("Delete user '" + user.username() + "' from auth DB",
                    () -> AuthUserRepository.deleteUser(user.username()));
        }
    }
}
