package io.student.rangiffler.data;

import java.util.List;

/**
 * A test user's credentials with its friends, the users who sent it an invitation and the users it invited.
 */
public record TestUser(
        String username,
        String password,
        List<TestUser> friends,
        List<TestUser> incomeInvitations,
        List<TestUser> outcomeInvitations
) {

    /**
     * Returns a copy of this user with the given friends.
     */
    public TestUser withFriends(List<TestUser> friends) {
        return new TestUser(username, password, friends, incomeInvitations, outcomeInvitations);
    }

    /**
     * Returns a copy of this user with the given inviters.
     */
    public TestUser withIncomeInvitations(List<TestUser> incomeInvitations) {
        return new TestUser(username, password, friends, incomeInvitations, outcomeInvitations);
    }

    /**
     * Returns a copy of this user with the given invited users.
     */
    public TestUser withOutcomeInvitations(List<TestUser> outcomeInvitations) {
        return new TestUser(username, password, friends, incomeInvitations, outcomeInvitations);
    }

    /**
     * Returns the username only, so the password stays out of logs.
     */
    @Override
    public String toString() {
        return "TestUser[username=" + username + "]";
    }
}
