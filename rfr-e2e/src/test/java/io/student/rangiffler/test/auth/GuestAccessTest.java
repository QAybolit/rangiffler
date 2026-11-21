package io.student.rangiffler.test.auth;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.GuestSession;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.ProfilePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Auth")
@Feature("Access")
@UiTest
class GuestAccessTest extends BaseWebTest {

    @Test
    @DisplayName("Guest who opens my travels is sent to the welcome page")
    void shouldRedirectGuestFromMyTravels(GuestSession guest) {
        new MyTravelsPage(guest.page()).open();
        new WelcomePage(guest.page()).shouldHaveLogin();
    }

    @Test
    @DisplayName("Guest who opens the profile is sent to the welcome page")
    void shouldRedirectGuestFromProfile(GuestSession guest) {
        new ProfilePage(guest.page()).open();
        new WelcomePage(guest.page()).shouldHaveLogin();
    }

    @Test
    @DisplayName("Guest who opens people is sent to the welcome page")
    void shouldRedirectGuestFromPeople(GuestSession guest) {
        new PeoplePage(guest.page()).open();
        new WelcomePage(guest.page()).shouldHaveLogin();
    }
}
