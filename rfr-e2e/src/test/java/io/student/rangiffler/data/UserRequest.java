package io.student.rangiffler.data;

import io.student.rangiffler.jupiter.annotation.User;

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
     * Reads the request from the {@code @User} annotation, so a new annotation attribute never touches the extension.
     */
    public UserRequest(User user) {
        this(user.friends(), user.incomeInvitations(), user.outcomeInvitations());
    }

    /**
     * Creates the fixture that satisfies this request.
     */
    public UserFixture fixture() {
        return UserWithOutcomeInvitationsFixture.create(
                UserWithIncomeInvitationsFixture.create(
                        UserWithFriendsFixture.create(BaseUserFixture.create(), friends),
                        incomeInvitations
                ),
                outcomeInvitations
        );
    }
}
