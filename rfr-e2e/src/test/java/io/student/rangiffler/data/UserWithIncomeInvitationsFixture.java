package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;

/**
 * Adds pending invitations, each sent by a user of its own, to the user of another fixture.
 */
public final class UserWithIncomeInvitationsFixture implements UserFixture {

    private final UserFixture origin;
    private final Partners inviters;

    private UserWithIncomeInvitationsFixture(UserFixture origin, Partners inviters) {
        this.origin = origin;
        this.inviters = inviters;
    }

    /**
     * Creates the inviters and their pending invitations, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithIncomeInvitationsFixture create(UserFixture origin, int invitations) {
        return new UserWithIncomeInvitationsFixture(
                origin,
                Partners.create(
                        origin,
                        invitations,
                        inviter -> new Friendship(inviter.username(), origin.user().username(), FriendshipStatus.PENDING)
                )
        );
    }

    @Override
    public TestUser user() {
        return origin.user().withIncomeInvitations(inviters.users());
    }

    /**
     * Removes the inviters and then the origin user.
     */
    @Override
    public void close() {
        inviters.close();
    }
}
