package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

@Epic("Auth")
@Feature("Login")
@Tag("ui")
class LoginNegativeTest extends BaseWebTest {

    private static final String BAD_CREDENTIALS_EN = "Bad credentials";

    @Test
    @DisplayName("Login with a wrong password shows 'Bad credentials'")
    void shouldShowBadCredentialsForWrongPassword(@User TestUser user) {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .loginWithInvalidCredentials(user.username(), randomPassword())
                .shouldHaveError(BAD_CREDENTIALS_EN);
    }

    @Test
    @DisplayName("Login with a non-existing user shows 'Bad credentials'")
    void shouldShowBadCredentialsForNonExistingUser() {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .loginWithInvalidCredentials(randomUsername(), randomPassword())
                .shouldHaveError(BAD_CREDENTIALS_EN);
    }

}
