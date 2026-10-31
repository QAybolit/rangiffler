package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;

/**
 * Adds accepted friends, each a user of its own, to the user of another fixture.
 */
public final class UserWithFriendsFixture implements UserFixture {

    private final UserFixture origin;
    private final Partners friends;

    private UserWithFriendsFixture(UserFixture origin, Partners friends) {
        this.origin = origin;
        this.friends = friends;
    }

    /**
     * Creates the friends and their accepted friendships, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithFriendsFixture create(UserFixture origin, int friends) {
        return new UserWithFriendsFixture(
                origin,
                Partners.create(
                        origin,
                        friends,
                        friend -> new Friendship(origin.user().username(), friend.username(), FriendshipStatus.ACCEPTED)
                )
        );
    }

    @Override
    public TestUser user() {
        return origin.user().withFriends(friends.users());
    }

    /**
     * Removes the friends and then the origin user.
     */
    @Override
    public void close() {
        friends.close();
    }
}
