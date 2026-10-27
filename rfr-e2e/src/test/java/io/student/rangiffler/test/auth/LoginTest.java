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

@Epic("Auth")
@Feature("Login")
@Tag("ui")
class LoginTest extends BaseWebTest {

    @Test
    @DisplayName("User can log in")
    void shouldLoginWithExistingUser(@User TestUser user) {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password())
                .header()
                .openProfile()
                .shouldHaveUsername(user.username());
    }
}
