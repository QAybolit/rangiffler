package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class LoginPage {

    public static final String URL = TestConfig.authUrl() + "/login";

    private final Page page;
    private final Locator usernameInput;
    private final Locator passwordInput;
    private final Locator signInButton;
    private final Locator error;

    public LoginPage(Page page) {
        this.page = page;
        this.usernameInput = page.getByPlaceholder("Type your username");
        this.passwordInput = page.getByPlaceholder("Type your password");
        this.signInButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Sign in"));
        this.error = page.locator(".form__error");
    }

    @Step("Login as '{username}'")
    public MyTravelsPage login(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        signInButton.click();
        return new MyTravelsPage(page);
    }

    @Step("Login as '{username}' with invalid credentials")
    public LoginPage loginWithInvalidCredentials(String username, String password) {
        usernameInput.fill(username);
        passwordInput.fill(password);
        signInButton.click();
        return this;
    }

    @Step("Check that login error is '{message}'")
    public LoginPage shouldHaveError(String message) {
        assertThat(error).hasText(message);
        return this;
    }
}
