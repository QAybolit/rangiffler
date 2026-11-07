package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.BaseUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Outcome invitations tab")
@Tag("ui")
class OutcomeInvitationsTabTest extends BasePeopleTest {

    @Test
    @DisplayName("Outcome invitations tab lists the invited user and not the friend")
    void shouldListOnlyInvitedUsers(@User(friends = 1, outcomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openOutcomeInvitations()
                .shouldHaveUsers(user.outcomeInvitations().getFirst().username())
                .shouldNotHaveUsers(user.friends().getFirst().username());
    }

    @Test
    @DisplayName("Invited user row shows Waiting... and offers the Cancel button")
    void shouldShowWaitingAndOfferCancel(@User(friends = 1, outcomeInvitations = 1) TestUser user) {
        openPeople(user)
                .openOutcomeInvitations()
                .shouldHaveInvitationSent(user.outcomeInvitations().getFirst().username())
                .shouldHaveButtons(user.outcomeInvitations().getFirst().username(), "Cancel");
    }

    @Test
    @DisplayName("Outcome invitations tab is empty when the user has only a friend")
    void shouldBeEmptyWithoutInvitations(@User(friends = 1) TestUser user) {
        openPeople(user)
                .openOutcomeInvitations()
                .shouldBeEmpty();
    }

    @Test
    @DisplayName("Invitation sent in the browser is shown as waiting")
    void shouldShowInvitationSentInBrowserAsWaiting() {
        try (UserFixture sender = BaseUserFixture.create(); UserFixture addressee = BaseUserFixture.create()) {
            openPeople(sender.user())
                    .openAllPeople()
                    .search(addressee.user().username())
                    .clickAdd(addressee.user().username())
                    .shouldHaveInvitationSent(addressee.user().username());
        }
    }
}
