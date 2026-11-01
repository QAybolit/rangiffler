package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import io.student.rangiffler.jupiter.extension.UsersQueueExtension;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@Epic("Auth")
@Feature("Login")
@ExtendWith(UsersQueueExtension.class)
class LoginStaticUserTest extends BaseWebTest {

    @Test
    @DisplayName("Static user can log in and sees the header")
    void shouldLoginWithStaticUser(@StaticUser TestUser user) {
        new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password())
                .header()
                .shouldBeVisible();
    }
}
