package io.student.rangiffler.data;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.Friendship;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Owns the users linked to the user of another fixture by friendships and closes them together with that fixture.
 */
final class Partners {

    private final UserFixture origin;
    private final List<UserFixture> fixtures;

    private Partners(UserFixture origin, List<UserFixture> fixtures) {
        this.origin = origin;
        this.fixtures = fixtures;
    }

    /**
     * Creates the partners and the friendship that {@code link} builds for each, and closes everything created so far, the origin included, if that fails.
     */
    static Partners create(UserFixture origin, int count, Function<TestUser, Friendship> link) {
        List<UserFixture> created = new ArrayList<>();
        try {
            for (int index = 0; index < count; index++) {
                UserFixture partner = BaseUserFixture.create();
                created.add(partner);
                Friendship friendship = link.apply(partner.user());
                Allure.step("Create " + friendship, () -> ApiUserRepository.createFriendship(friendship));
            }
            return new Partners(origin, created);
        } catch (RuntimeException | Error e) {
            try {
                close(origin, created);
            } catch (RuntimeException | Error closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    List<TestUser> users() {
        return fixtures.stream().map(UserFixture::user).toList();
    }

    /**
     * Closes the partners and then the origin.
     */
    void close() {
        close(origin, fixtures);
    }

    /**
     * Closes every fixture, the origin last, keeping the first failure and suppressing the rest.
     */
    private static void close(UserFixture origin, List<UserFixture> partners) {
        RuntimeException failure = null;
        List<UserFixture> all = new ArrayList<>(partners);
        all.add(origin);
        for (UserFixture fixture : all) {
            try {
                fixture.close();
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }
}
