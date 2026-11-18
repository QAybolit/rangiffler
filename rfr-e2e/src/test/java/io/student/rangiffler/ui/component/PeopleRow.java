package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleRow {

    private final Locator root;

    public PeopleRow(Locator root) {
        this.root = root;
    }

    @Step("Click 'Add'")
    public PeopleRow clickAdd() {
        button(PeopleButton.ADD).click();
        assertThat(root.getByText("Waiting...")).isVisible();
        return this;
    }

    @Step("Click 'Accept'")
    public PeopleRow clickAccept() {
        button(PeopleButton.ACCEPT).click();
        assertThat(root).hasCount(0);
        return this;
    }

    @Step("Click 'Remove'")
    public PeopleRow clickRemove() {
        button(PeopleButton.REMOVE).click();
        assertThat(root).hasCount(0);
        return this;
    }

    @Step("Check that the row has the buttons {buttons}")
    public PeopleRow shouldHaveButtons(PeopleButton... buttons) {
        for (PeopleButton button : buttons) {
            assertThat(button(button)).isVisible();
        }
        return this;
    }

    @Step("Check that the invitation is waiting")
    public PeopleRow shouldHaveInvitationSent() {
        assertThat(root.getByText("Waiting...")).isVisible();
        return this;
    }

    private Locator button(PeopleButton button) {
        return root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(button.label()).setExact(true));
    }
}
