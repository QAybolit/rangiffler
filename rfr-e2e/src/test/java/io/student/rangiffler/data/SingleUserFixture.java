package io.student.rangiffler.data;

import io.qameta.allure.Allure;

import java.util.List;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

/**
 * Owns one test user's lifecycle: creates it with a profile in the databases and removes it on close.
 */
public final class SingleUserFixture implements UserFixture {

    private final TestUser user;

    private SingleUserFixture(TestUser user) {
        this.user = user;
    }

    /**
     * Creates a random user in the auth database and its profile in the api database, and cleans up if that fails.
     */
    public static SingleUserFixture create() {
        SingleUserFixture fixture = new SingleUserFixture(new TestUser(randomUsername(), randomPassword(), List.of(), List.of()));
        try {
            Allure.step("Create user '" + fixture.user.username() + "' in auth DB",
                    () -> AuthUserRepository.createUser(fixture.user.username(), fixture.user.password()));
            Allure.step("Create profile '" + fixture.user.username() + "' in api DB",
                    () -> ApiUserRepository.createProfile(fixture.user.username()));
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

    @Override
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
