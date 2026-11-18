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
import io.student.rangiffler.ui.page.PeoplePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Epic("People")
@Feature("Friendship")
@UiTest
class FriendshipFlowTest extends BasePeopleTest {

    @Test
    @DisplayName("Two users become friends through an invitation and stop being friends when one removes the other")
    void shouldBuildAndBreakFriendshipBetweenTwoUsers(@User TestUser sender, @User UserSession receiver) {
        String invited = receiver.user().username();
        String inviter = sender.username();
        PeoplePage senderPeople = openPeople(sender)
                .openAllPeople()
                .search(invited)
                .clickAdd(invited)
                .shouldHaveInvitationSent(invited)
                .shouldHaveNotification(Notification.INVITATION_SENT)
                .openOutcomeInvitations()
                .shouldHaveUsers(invited);
        PeoplePage receiverPeople = new MyTravelsPage(receiver.page())
                .sidebar()
                .openPeople()
                .openIncomeInvitations()
                .shouldHaveUsers(inviter)
                .shouldHaveButtons(inviter, PeopleButton.ACCEPT, PeopleButton.DECLINE)
                .clickAccept(inviter)
                .shouldHaveNotification(Notification.INVITATION_ACCEPTED)
                .shouldNotHaveUsers(inviter)
                .openFriends()
                .shouldHaveUsers(inviter)
                .shouldHaveButtons(inviter, PeopleButton.REMOVE);
        senderPeople
                .openFriends()
                .shouldHaveUsers(invited)
                .clickRemove(invited)
                .shouldHaveNotification(Notification.FRIEND_DELETED)
                .shouldNotHaveUsers(invited);
        receiverPeople
                .openAllPeople()
                .openFriends()
                .shouldBeEmpty();
    }
}
