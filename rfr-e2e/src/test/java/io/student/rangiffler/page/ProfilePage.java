package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.page.component.Header;

import java.nio.file.Path;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class ProfilePage {

    public static final String URL = TestConfig.frontUrl() + "/profile";

    private final Page page;
    private final Header header;
    private final Locator avatarInput;
    private final Locator avatarImage;
    private final Locator firstNameInput;
    private final Locator surnameInput;
    private final Locator usernameInput;
    private final Locator locationSelect;
    private final Locator resetButton;
    private final Locator saveButton;
    private final Locator successMessage;

    public ProfilePage(Page page) {
        this.page = page;
        this.header = new Header(page);
        this.avatarInput = page.getByLabel("Update avatar");
        this.avatarImage = page.getByRole(AriaRole.MAIN).locator("img[src^='data:image/']:not([alt])");
        this.firstNameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("First Name"));
        this.surnameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Surname"));
        this.usernameInput = page.getByRole(AriaRole.TEXTBOX, new Page.GetByRoleOptions().setName("Username"));
        this.locationSelect = page.getByRole(AriaRole.COMBOBOX, new Page.GetByRoleOptions().setName("Location"));
        this.resetButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Reset"));
        this.saveButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Save"));
        this.successMessage = page.getByText("Your profile is successfully updated");
    }

    public Header header() {
        return header;
    }

    @Step("Fill profile form: first name '{firstName}', surname '{surname}', location '{country}'")
    public ProfilePage fillForm(String firstName, String surname, String country) {
        firstNameInput.fill(firstName);
        surnameInput.fill(surname);
        locationSelect.click();
        page.getByRole(AriaRole.OPTION)
                .filter(new Locator.FilterOptions().setHas(
                        page.getByRole(AriaRole.IMG, new Page.GetByRoleOptions().setName(country).setExact(true))))
                .click();
        return this;
    }

    @Step("Upload avatar '{file}'")
    public ProfilePage uploadAvatar(Path file) {
        avatarInput.setInputFiles(file);
        return this;
    }

    @Step("Click 'Save' and wait for UpdateUser mutation")
    public ProfilePage clickSave() {
        page.waitForResponse(
                response -> response.url().endsWith("/graphql")
                        && response.request().postData() != null
                        && response.request().postData().contains("UpdateUser"),
                saveButton::click
        );
        return this;
    }

    @Step("Reload profile page")
    public ProfilePage reload() {
        page.reload();
        return this;
    }

    @Step("Click 'Reset'")
    public ProfilePage reset() {
        resetButton.click();
        return this;
    }

    @Step("Check that username is '{username}'")
    public ProfilePage shouldHaveUsername(String username) {
        assertThat(usernameInput).hasValue(username);
        return this;
    }

    @Step("Check that first name is '{firstName}'")
    public ProfilePage shouldHaveFirstName(String firstName) {
        assertThat(firstNameInput).hasValue(firstName);
        return this;
    }

    @Step("Check that surname is '{surname}'")
    public ProfilePage shouldHaveSurname(String surname) {
        assertThat(surnameInput).hasValue(surname);
        return this;
    }

    @Step("Check that location is '{country}'")
    public ProfilePage shouldHaveLocation(String country) {
        assertThat(locationSelect).hasText(country);
        return this;
    }

    @Step("Check that avatar is displayed")
    public ProfilePage shouldHaveAvatar() {
        assertThat(avatarImage).isVisible();
        return this;
    }

    @Step("Check profile success message")
    public ProfilePage shouldHaveSuccessMessage() {
        assertThat(successMessage).isVisible();
        return this;
    }
}
