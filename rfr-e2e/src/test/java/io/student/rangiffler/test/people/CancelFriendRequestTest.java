package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Friendship")
@UiTest
class CancelFriendRequestTest extends BasePeopleTest {

    @Test
    @DisplayName("Cancelled outgoing friend request disappears from the invitations of the invited user")
    void shouldCancelOutgoingFriendRequest(@User(outcomeInvitations = 1) UserSession alice) {
        TestUser invited = alice.user().outcomeInvitations().getFirst();
        new MyTravelsPage(alice.page())
                .sidebar()
                .openPeople()
                .openOutcomeInvitations()
                .clickCancel(invited.username())
                .shouldHaveNotification(Notification.INVITATION_CANCELLED)
                .shouldNotHaveUsers(invited.username());
        openPeople(invited)
                .openIncomeInvitations()
                .shouldHaveUsers(alice.user().username());
    }
}
