package io.student.rangiffler.ui.component;

public enum Notification {

    INVITATION_SENT("Invitation sent"),
    INVITATION_ACCEPTED("Invitation accepted"),
    INVITATION_DECLINED("Invitation declined"),
    FRIEND_DELETED("Friend deleted"),
    INVITATION_CANCELLED("Invitation cancelled"),
    PROFILE_UPDATED("Your profile is successfully updated"),
    REGISTERED("Congratulations! You've registered!"),
    BAD_CREDENTIALS("Bad credentials");

    private final String text;

    Notification(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
