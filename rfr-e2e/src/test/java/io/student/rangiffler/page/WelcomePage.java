package io.student.rangiffler.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

public final class WelcomePage {

    public static final String URL = TestConfig.frontUrl();

    private final Page page;
    private final Locator loginButton;
    private final Locator registerLink;

    public WelcomePage(Page page) {
        this.page = page;
        this.loginButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Login"));
        this.registerLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("Register"));
    }

    @Step("Open welcome page {URL}")
    public WelcomePage open() {
        page.navigate(URL);
        return this;
    }

    @Step("Click 'Login'")
    public LoginPage clickLogin() {
        loginButton.click();
        return new LoginPage(page);
    }

    @Step("Click 'Register'")
    public RegisterPage clickRegister() {
        registerLink.click();
        return new RegisterPage(page);
    }
}
