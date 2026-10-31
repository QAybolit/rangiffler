package io.student.rangiffler.data;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.FriendshipStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds accepted friends, each a user of its own, to the user of another fixture.
 */
public final class UserWithFriendsFixture implements UserFixture {

    private final UserFixture origin;
    private final List<UserFixture> friends;

    private UserWithFriendsFixture(UserFixture origin, List<UserFixture> friends) {
        this.origin = origin;
        this.friends = friends;
    }

    /**
     * Creates the friends and their accepted friendships, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithFriendsFixture create(UserFixture origin, int friends) {
        List<UserFixture> created = new ArrayList<>();
        try {
            for (int index = 0; index < friends; index++) {
                UserFixture friend = BaseUserFixture.create();
                created.add(friend);
                Allure.step("Create " + FriendshipStatus.ACCEPTED + " friendship '" + origin.user().username() + "' -> '" + friend.user().username() + "'",
                        () -> ApiUserRepository.createFriendship(origin.user().username(), friend.user().username(), FriendshipStatus.ACCEPTED));
            }
            return new UserWithFriendsFixture(origin, created);
        } catch (RuntimeException | Error e) {
            try {
                close(created, origin);
            } catch (RuntimeException | Error closeFailure) {
                e.addSuppressed(closeFailure);
            }
            throw e;
        }
    }

    @Override
    public TestUser user() {
        return origin.user().withFriends(friends.stream().map(UserFixture::user).toList());
    }

    /**
     * Removes the friends and then the origin user.
     */
    @Override
    public void close() {
        close(friends, origin);
    }

    /**
     * Closes every fixture, the origin last, keeping the first failure and suppressing the rest.
     */
    private static void close(List<UserFixture> friends, UserFixture origin) {
        RuntimeException failure = null;
        List<UserFixture> all = new ArrayList<>(friends);
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
