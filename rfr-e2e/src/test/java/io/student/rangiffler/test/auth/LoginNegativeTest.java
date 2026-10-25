package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.BrowserLocale;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

/**
 * Negative login scenarios.
 */
@Epic("Auth")
@Feature("Login")
class LoginNegativeTest extends BaseWebTest {

    private static final String BAD_CREDENTIALS_EN = "Bad credentials";
    private static final String BAD_CREDENTIALS_RU = "Неверные учетные данные пользователя";

    /**
     * Checks that a wrong password shows the English bad credentials error.
     */
    @Test
    @BrowserLocale("en-US")
    @DisplayName("Login with a wrong password shows 'Bad credentials'")
    void shouldShowBadCredentialsForWrongPassword(@User TestUser user) {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .loginWithInvalidCredentials(user.username(), randomPassword())
                .shouldHaveError(BAD_CREDENTIALS_EN);
    }

    /**
     * Checks that a non-existing user shows the same English error as a wrong password.
     */
    @Test
    @BrowserLocale("en-US")
    @DisplayName("Login with a non-existing user shows 'Bad credentials'")
    void shouldShowBadCredentialsForNonExistingUser() {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .loginWithInvalidCredentials(randomUsername(), randomPassword())
                .shouldHaveError(BAD_CREDENTIALS_EN);
    }

    /**
     * Checks that the bad credentials error is shown in Russian for the Russian locale.
     */
    @Test
    @BrowserLocale("ru-RU")
    @DisplayName("Login with a wrong password shows the error in Russian")
    void shouldShowBadCredentialsInRussian(@User TestUser user) {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .loginWithInvalidCredentials(user.username(), randomPassword())
                .shouldHaveError(BAD_CREDENTIALS_RU);
    }
}
