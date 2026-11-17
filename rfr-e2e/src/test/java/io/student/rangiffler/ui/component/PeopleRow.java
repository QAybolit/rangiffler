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
        button("Add").click();
        assertThat(root.getByText("Waiting...")).isVisible();
        return this;
    }

    @Step("Check that the row has the buttons {buttons}")
    public PeopleRow shouldHaveButtons(String... buttons) {
        for (String button : buttons) {
            assertThat(button(button)).isVisible();
        }
        return this;
    }

    @Step("Check that the invitation is waiting")
    public PeopleRow shouldHaveInvitationSent() {
        assertThat(root.getByText("Waiting...")).isVisible();
        return this;
    }

    private Locator button(String name) {
        return root.getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName(name).setExact(true));
    }
}
