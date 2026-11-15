package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.AuthUserRepository;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static io.student.rangiffler.utils.RandomDataUtils.randomPassword;
import static io.student.rangiffler.utils.RandomDataUtils.randomUsername;

@Epic("Auth")
@Feature("Registration")
@Tag("ui")
class RegistrationTest extends BaseWebTest {

    private String username;

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
