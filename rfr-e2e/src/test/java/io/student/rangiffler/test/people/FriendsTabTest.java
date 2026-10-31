package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Friends tab")
@Tag("ui")
class FriendsTabTest extends BasePeopleTest {

    @Test
    @DisplayName("Friends tab lists the friend and not the user who sent an invitation")
    void shouldListOnlyFriends(@User(friends = 1, incomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openFriends()
                .shouldHaveUsers(user.friends().getFirst().username())
                .shouldNotHaveUsers(user.incomeInvitations().getFirst().username());
    }

    @Test
    @DisplayName("Friend row offers the Remove button")
    void shouldOfferRemoveForFriend(@User(friends = 1, incomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openFriends()
                .shouldHaveButtons(user.friends().getFirst().username(), "Remove");
    }

    @Test
    @DisplayName("Friends tab is empty when the user has only an invitation")
    void shouldBeEmptyWithoutFriends(@User(incomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openFriends()
                .shouldBeEmpty();
    }
}
