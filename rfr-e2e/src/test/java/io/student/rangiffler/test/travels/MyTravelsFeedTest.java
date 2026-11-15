package io.student.rangiffler.test.travels;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("Travels")
@Feature("Feed")
@UiTest
class MyTravelsFeedTest extends BaseWebTest {

    @Test
    @DisplayName("Feed lists the four photos of the mock")
    void shouldListAllPhotos(@User TestUser user) {
        openFeed(user).shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!");
    }

    @Test
    @DisplayName("Photo card shows its country and likes")
    void shouldShowCountryAndLikesOnCard(@User TestUser user) {
        openFeed(user).photo("Париж!").shouldHaveCountry("France").shouldHaveLikes(2);
    }

    @Test
    @DisplayName("Own photo offers the actions")
    void shouldOfferActionsOnOwnPhoto(@User TestUser user) {
        openFeed(user).photo("Париж!").shouldHaveActions();
    }

    @Test
    @DisplayName("Country filter leaves only the photos of the chosen country")
    void shouldFilterFeedByCountry(@User TestUser user) {
        openFeed(user)
                .shouldHaveCountriesVisited(4)
                .openCountries()
                .filterByCountry("Netherlands")
                .shouldHaveCountryFilter("Netherlands")
                .shouldHavePhotos("Амстердам!")
                .shouldNotHavePhotos("Париж!", "Петербург!", "Нью-Йорк!");
    }

    @Test
    @DisplayName("Reset of the country filter brings all photos back")
    void shouldResetCountryFilter(@User TestUser user) {
        openFeed(user)
                .shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!")
                .openCountries()
                .filterByCountry("Netherlands")
                .shouldHavePhotos("Амстердам!")
                .resetCountryFilter()
                .shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!");
    }

    private MyTravelsPage openFeed(TestUser user) {
        return new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password());
    }
}
