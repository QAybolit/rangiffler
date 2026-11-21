package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Auth")
@Feature("Logout")
@UiTest
class LogoutIsolationTest extends BaseWebTest {

    @Test
    @DisplayName("Logout in one browser context keeps the same user signed in in another context")
    void logoutKeepsOtherContextSignedIn(@User(friends = 1) UserSession bob) {
        TestUser friend = bob.user().friends().getFirst();
        new WelcomePage(page)
                .open()
                .clickLogin()
                .login(bob.user().username(), bob.user().password())
                .header()
                .clickLogout()
                .shouldHaveLogin();
        new MyTravelsPage(bob.page())
                .sidebar()
                .openPeople()
                .openFriends()
                .shouldHaveUsers(bob.user().username());
    }
}
