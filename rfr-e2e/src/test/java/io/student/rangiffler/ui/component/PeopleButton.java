package io.student.rangiffler.ui.component;

public enum PeopleButton {

    ADD("Add"),
    ACCEPT("Accept"),
    DECLINE("Decline"),
    REMOVE("Remove"),
    CANCEL("Cancel");

    private final String label;

    PeopleButton(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
