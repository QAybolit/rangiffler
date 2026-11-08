package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Income invitations tab")
@Tag("ui")
class IncomeInvitationsTabTest extends BasePeopleTest {

    @Test
    @DisplayName("Income invitations tab lists the inviter and not the friend")
    void shouldListOnlyInviters(@User(friends = 1, incomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openIncomeInvitations()
                .shouldHaveUsers(user.incomeInvitations().getFirst().username())
                .shouldNotHaveUsers(user.friends().getFirst().username());
    }

    @Test
    @DisplayName("Inviter row offers the Accept and Decline buttons")
    void shouldOfferAcceptAndDeclineForInviter(@User(friends = 1, incomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openIncomeInvitations()
                .shouldHaveButtons(user.incomeInvitations().getFirst().username(), "Accept", "Decline");
    }

    @Test
    @DisplayName("Income invitations tab is empty when the user has only a friend")
    void shouldBeEmptyWithoutInvitations(@User(friends = 1) TestUser user) {
        openPeople(user)
                .openIncomeInvitations()
                .shouldBeEmpty();
    }
}
