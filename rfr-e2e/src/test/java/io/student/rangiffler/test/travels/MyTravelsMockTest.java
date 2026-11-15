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
@Feature("Feed on mock")
@UiTest
class MyTravelsMockTest extends BaseWebTest {

    @Test
    @DisplayName("On the mock feed the switch changes the cards and the visited countries")
    void shouldSwitchBetweenOwnAndFriendsTravels(@User TestUser user) {
        openFeed(user)
                .shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!")
                .shouldHaveCountriesVisited(4)
                .showWithFriends()
                .shouldHavePhotos(
                        "Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!",
                        "Старая Прага завораживает!", "Мюнхен - мой любимый город!", "Обожаю Таиланд!"
                )
                .shouldHaveCountriesVisited(7)
                .showOnlyMyTravels()
                .shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!")
                .shouldHaveCountriesVisited(4);
    }

    @Test
    @DisplayName("On the mock feed the country filter leaves one card and the reset brings the four back")
    void shouldFilterByCountryAndReset(@User TestUser user) {
        openFeed(user)
                .openCountries()
                .filterByCountry("Netherlands")
                .shouldHaveCountryFilter("Netherlands")
                .shouldHavePhotos("Амстердам!")
                .resetCountryFilter()
                .shouldHavePhotos("Амстердам!", "Париж!", "Петербург!", "Нью-Йорк!");
    }

    @Test
    @DisplayName("On the mock feed the Photo actions menu is only on own photos")
    void shouldOfferActionsOnlyOnOwnPhotos(@User TestUser user) {
        MyTravelsPage feed = openFeed(user).showWithFriends();
        feed.photo("Амстердам!").shouldHaveCountry("Netherlands").shouldHaveActions();
        feed.photo("Париж!").shouldHaveCountry("France").shouldHaveActions();
        feed.photo("Петербург!").shouldHaveCountry("Russian Federation").shouldHaveActions();
        feed.photo("Нью-Йорк!").shouldHaveCountry("United States").shouldHaveActions();
        feed.photo("Старая Прага завораживает!").shouldHaveCountry("Czech Republic").shouldNotHaveActions();
        feed.photo("Мюнхен - мой любимый город!").shouldHaveCountry("Germany").shouldNotHaveActions();
        feed.photo("Обожаю Таиланд!").shouldHaveCountry("Thailand").shouldNotHaveActions();
    }

    private MyTravelsPage openFeed(TestUser user) {
        return new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password());
    }
}
