package io.student.rangiffler.data;

import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;

import java.util.List;

/**
 * How a partner is linked to the user of a fixture: the friendship status, who asked whom, and which list of the user holds the partner.
 */
public enum Relation {
    FRIEND,
    INCOME_INVITATION,
    OUTCOME_INVITATION;

    /**
     * Builds the friendship between the origin user and one partner in the status and direction of this relation.
     */
    public Friendship friendship(TestUser origin, TestUser partner) {
        return switch (this) {
            case FRIEND -> new Friendship(origin.username(), partner.username(), FriendshipStatus.ACCEPTED);
            case INCOME_INVITATION -> new Friendship(partner.username(), origin.username(), FriendshipStatus.PENDING);
            case OUTCOME_INVITATION -> new Friendship(origin.username(), partner.username(), FriendshipStatus.PENDING);
        };
    }

    /**
     * Returns a copy of the origin user whose list for this relation holds the given partners.
     */
    public TestUser attach(TestUser origin, List<TestUser> partners) {
        return switch (this) {
            case FRIEND -> origin.withFriends(partners);
            case INCOME_INVITATION -> origin.withIncomeInvitations(partners);
            case OUTCOME_INVITATION -> origin.withOutcomeInvitations(partners);
        };
    }
}
