package io.student.rangiffler.test.people;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.ui.component.Notification;
import io.student.rangiffler.ui.component.PeopleButton;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Friendship")
@UiTest
class SendFriendRequestTest extends BasePeopleTest {

    @Test
    @DisplayName("Friend request sent by one user shows up as an income invitation of the other")
    void shouldSendFriendRequest(@User TestUser sender, @User UserSession receiver) {
        String invited = receiver.user().username();
        openPeople(sender)
                .openAllPeople()
                .search(invited)
                .clickAdd(invited)
                .shouldHaveNotification(Notification.INVITATION_SENT)
                .shouldHaveInvitationSent(invited);
        new MyTravelsPage(receiver.page())
                .sidebar()
                .openPeople()
                .openIncomeInvitations()
                .shouldHaveUsers(sender.username())
                .shouldHaveButtons(sender.username(), PeopleButton.ACCEPT, PeopleButton.DECLINE);
    }
}
