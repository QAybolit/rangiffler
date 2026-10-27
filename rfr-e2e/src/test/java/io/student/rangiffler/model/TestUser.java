package io.student.rangiffler.model;

/**
 * A test user's credentials.
 */
public record TestUser(String username, String password) {

    /**
     * Returns the username only, so the password stays out of logs.
     */
    @Override
    public String toString() {
        return "TestUser[username=" + username + "]";
    }
}
