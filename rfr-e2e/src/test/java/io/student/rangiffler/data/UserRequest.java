package io.student.rangiffler.data;

import io.student.rangiffler.jupiter.annotation.User;

/**
 * What a test asks of its user: how many friends and incoming invitations it must have.
 */
public record UserRequest(int friends, int incomeInvitations) {

    /**
     * Fails fast on a negative count, which would otherwise silently mean none.
     */
    public UserRequest {
        if (friends < 0 || incomeInvitations < 0) {
            throw new IllegalArgumentException("User request counts cannot be negative: friends=" + friends + ", incomeInvitations=" + incomeInvitations);
        }
    }

    /**
     * Reads the request from the {@code @User} annotation.
     */
    public static UserRequest from(User annotation) {
        return new UserRequest(annotation.friends(), annotation.incomeInvitations());
    }

    /**
     * Creates the fixture that satisfies this request.
     */
    public UserFixture fixture() {
        return UserWithIncomeInvitationsFixture.create(
                UserWithFriendsFixture.create(BaseUserFixture.create(), friends),
                incomeInvitations
        );
    }
}
