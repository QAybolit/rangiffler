package io.student.rangiffler.data;

/**
 * What a test asks of its user: how many friends, incoming and outgoing invitations it must have.
 */
public record UserRequest(int friends, int incomeInvitations, int outcomeInvitations) {

    /**
     * Fails fast on a negative count, which would otherwise silently mean none.
     */
    public UserRequest {
        if (friends < 0 || incomeInvitations < 0 || outcomeInvitations < 0) {
            throw new IllegalArgumentException(
                    "User request counts cannot be negative: friends=" + friends
                            + ", incomeInvitations=" + incomeInvitations
                            + ", outcomeInvitations=" + outcomeInvitations
            );
        }
    }

    /**
     * Creates the fixture that satisfies this request.
     */
    public UserFixture fixture() {
        return with(
                with(
                        with(BaseUserFixture.create(), Relation.FRIEND, friends),
                        Relation.INCOME_INVITATION,
                        incomeInvitations
                ),
                Relation.OUTCOME_INVITATION,
                outcomeInvitations
        );
    }

    /**
     * Wraps the origin with partners of the relation, or returns it unchanged when none are asked.
     */
    private static UserFixture with(UserFixture origin, Relation relation, int count) {
        return count == 0 ? origin : UserWithRelationsFixture.create(origin, relation, count);
    }
}
