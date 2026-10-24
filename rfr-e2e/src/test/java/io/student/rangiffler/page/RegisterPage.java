package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class RegisterPage {

    public static final String URL = TestConfig.authUrl() + "/register";

    private final Page page;
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator confirmPasswordInput;
    private final Locator signUpButton;
    private final Locator successMessage;
    private final Locator signInLink;

    public RegisterPage(Page page) {
        this.page = page;
        this.usernameInput = page.getByPlaceholder("Type your username");
        this.passwordInput = page.getByPlaceholder("Type your password");
        this.confirmPasswordInput = page.getByPlaceholder("Repeat your password");
        this.signUpButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign up"));
        this.successMessage = page.getByText("Congratulations! You've registered!");
        this.signInLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Sign in"));
    }

    @Step("Fill registration form for user '{username}'")
    public RegisterPage fillForm(String username, String password, String confirmPassword) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        confirmPasswordInput.fill(confirmPassword);
        return this;
    }

    @Step("Submit registration form")
    public RegisterPage submit() {
        signUpButton.click();
        return this;
    }

    @Step("Click 'Sign in' after registration")
    public WelcomePage clickSignIn() {
        signInLink.click();
        return new WelcomePage(page);
    }

    @Step("Check registration success message")
    public RegisterPage shouldHaveSuccessMessage() {
        assertThat(successMessage).isVisible();
        return this;
    }
}
