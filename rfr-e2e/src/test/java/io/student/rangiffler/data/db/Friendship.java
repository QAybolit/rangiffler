package io.student.rangiffler.data.db;

/**
 * A friendship row of the rfr-api database: who asked, who was asked and in which status.
 */
public record Friendship(String requester, String addressee, FriendshipStatus status) {
}
