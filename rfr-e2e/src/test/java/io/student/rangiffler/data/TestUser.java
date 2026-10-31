package io.student.rangiffler.data;

import java.util.List;

/**
 * A test user's credentials with the friends and the users who sent it an invitation.
 */
public record TestUser(String username, String password, List<TestUser> friends, List<TestUser> incomeInvitations) {

    /**
     * Returns a copy of this user with the given friends.
     */
    public TestUser withFriends(List<TestUser> friends) {
        return new TestUser(username, password, friends, incomeInvitations);
    }

    /**
     * Returns a copy of this user with the given inviters.
     */
    public TestUser withIncomeInvitations(List<TestUser> incomeInvitations) {
        return new TestUser(username, password, friends, incomeInvitations);
    }

    /**
     * Returns the username only, so the password stays out of logs.
     */
    @Override
    public String toString() {
        return "TestUser[username=" + username + "]";
    }
}
