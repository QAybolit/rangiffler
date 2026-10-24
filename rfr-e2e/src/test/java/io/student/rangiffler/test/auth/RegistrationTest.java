package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.test.web.BaseWebTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

/**
 * Registration scenarios.
 */
@Epic("Auth")
@Feature("Registration")
class RegistrationTest extends BaseWebTest {

    /**
     * Checks that a newly registered user can log in.
     */
    @Test
    @DisplayName("New user can register and then log in successfully")
    void shouldLoginAfterRegistration() {
        String username = randomUsername();
        String password = randomPassword();

        new WelcomePage(page)
                .open()
                .clickRegister()
                .fillForm(username, password, password)
                .submit()
                .shouldHaveSuccessMessage()
                .clickSignIn()
                .clickLogin()
                .login(username, password)
                .header()
                .openProfile()
                .shouldHaveUsername(username);
    }
}
