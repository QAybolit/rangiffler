package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.GuestSession;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.component.PeopleButton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Friendship")
@UiTest
class DeclineFriendRequestTest extends BasePeopleTest {

    @Test
    @DisplayName("Declined friend request leaves both users able to invite each other again")
    void shouldDeclineFriendRequest(@User(incomeInvitations = 1) TestUser receiver, GuestSession inviterSession) {
        TestUser inviter = receiver.incomeInvitations().getFirst();
        openPeople(receiver)
                .openIncomeInvitations()
                .clickDecline(inviter.username())
                .shouldHaveNotification(Notification.INVITATION_DECLINED)
                .openAllPeople()
                .search(inviter.username())
                .shouldHaveButtons(inviter.username(), PeopleButton.ADD);
        openPeople(inviterSession.page(), inviter)
                .openAllPeople()
                .search(receiver.username())
                .shouldHaveButtons(receiver.username(), PeopleButton.ADD);
    }
}
