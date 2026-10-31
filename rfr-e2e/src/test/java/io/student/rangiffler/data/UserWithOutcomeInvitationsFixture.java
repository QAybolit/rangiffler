package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;

/**
 * Adds pending invitations sent by the user of another fixture, each to a user of its own.
 */
public final class UserWithOutcomeInvitationsFixture implements UserFixture {

    private final UserFixture origin;
    private final Partners invited;

    private UserWithOutcomeInvitationsFixture(UserFixture origin, Partners invited) {
        this.origin = origin;
        this.invited = invited;
    }

    /**
     * Creates the invited users and the pending invitations to them, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithOutcomeInvitationsFixture create(UserFixture origin, int invitations) {
        return new UserWithOutcomeInvitationsFixture(
                origin,
                Partners.create(
                        origin,
                        invitations,
                        invitee -> new Friendship(origin.user().username(), invitee.username(), FriendshipStatus.PENDING)
                )
        );
    }

    @Override
    public TestUser user() {
        return origin.user().withOutcomeInvitations(invited.users());
    }

    /**
     * Removes the invited users and then the origin user.
     */
    @Override
    public void close() {
        invited.close();
    }
}
