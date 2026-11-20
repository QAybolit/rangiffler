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
class AcceptFriendRequestTest extends BasePeopleTest {

    @Test
    @DisplayName("Accepted friend request makes both users friends")
    void shouldAcceptFriendRequest(@User(incomeInvitations = 1) TestUser receiver, GuestSession inviterSession) {
        TestUser inviter = receiver.incomeInvitations().getFirst();
        openPeople(receiver)
                .openIncomeInvitations()
                .clickAccept(inviter.username())
                .shouldHaveNotification(Notification.INVITATION_ACCEPTED)
                .openFriends()
                .shouldHaveUsers(inviter.username())
                .shouldHaveButtons(inviter.username(), PeopleButton.REMOVE);
        openPeople(inviterSession.page(), inviter)
                .openFriends()
                .shouldHaveUsers(receiver.username())
                .shouldHaveButtons(receiver.username(), PeopleButton.REMOVE);
    }
}
