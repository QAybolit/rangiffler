package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.ApiUserRepository;
import io.student.rangiffler.data.AuthUserRepository;
import io.student.rangiffler.page.WelcomePage;
import io.student.rangiffler.test.BaseWebTest;
import org.junit.jupiter.api.AfterEach;
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

    private String username;

    /**
     * Checks that a newly registered user can log in.
     */
    @Test
    @DisplayName("New user can register and then log in successfully")
    void shouldLoginAfterRegistration() {
        username = randomUsername();
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

    /**
     * Removes the user registered through the UI from the api and auth databases.
     */
    @AfterEach
    void deleteRegisteredUser() {
        if (username == null) {
            return;
        }
        try {
            ApiUserRepository.deleteUser(username);
        } finally {
            AuthUserRepository.deleteUser(username);
        }
    }
}
