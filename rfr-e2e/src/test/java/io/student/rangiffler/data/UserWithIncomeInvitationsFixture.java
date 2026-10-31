package io.student.rangiffler.data;

import io.qameta.allure.Allure;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.FriendshipStatus;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds pending invitations, each sent by a user of its own, to the user of another fixture.
 */
public final class UserWithIncomeInvitationsFixture implements UserFixture {

    private final UserFixture origin;
    private final List<UserFixture> inviters;

    private UserWithIncomeInvitationsFixture(UserFixture origin, List<UserFixture> inviters) {
        this.origin = origin;
        this.inviters = inviters;
    }

    /**
     * Creates the inviters and their pending invitations, and closes everything created so far, the origin included, if that fails.
     */
    public static UserWithIncomeInvitationsFixture create(UserFixture origin, int invitations) {
        List<UserFixture> created = new ArrayList<>();
        try {
            for (int index = 0; index < invitations; index++) {
                UserFixture inviter = BaseUserFixture.create();
                created.add(inviter);
                Allure.step("Create " + FriendshipStatus.PENDING + " friendship '" + inviter.user().username() + "' -> '" + origin.user().username() + "'",
                        () -> ApiUserRepository.createFriendship(inviter.user().username(), origin.user().username(), FriendshipStatus.PENDING));
            }
            return new UserWithIncomeInvitationsFixture(origin, created);
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
        return origin.user().withIncomeInvitations(inviters.stream().map(UserFixture::user).toList());
    }

    /**
     * Removes the inviters and then the origin user.
     */
    @Override
    public void close() {
        close(inviters, origin);
    }

    /**
     * Closes every fixture, the origin last, keeping the first failure and suppressing the rest.
     */
    private static void close(List<UserFixture> inviters, UserFixture origin) {
        RuntimeException failure = null;
        List<UserFixture> all = new ArrayList<>(inviters);
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
