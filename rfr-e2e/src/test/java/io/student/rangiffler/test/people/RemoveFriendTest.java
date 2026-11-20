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
class RemoveFriendTest extends BasePeopleTest {

    @Test
    @DisplayName("Friend removed by the user is shown to the friend as a stranger who can be invited")
    void shouldRemoveFriendWhenInitiatorRemoves(@User(friends = 1) TestUser user, GuestSession friendSession) {
        TestUser friend = user.friends().getFirst();
        openPeople(user)
                .openFriends()
                .clickRemove(friend.username())
                .shouldHaveNotification(Notification.FRIEND_DELETED);
        openPeople(friendSession.page(), friend)
                .openAllPeople()
                .search(user.username())
                .shouldHaveButtons(user.username(), PeopleButton.ADD);
    }

    @Test
    @DisplayName("User removed by the friend is shown to the user as a stranger who can be invited")
    void shouldRemoveFriendWhenFriendRemoves(@User(friends = 1) TestUser user, GuestSession friendSession) {
        TestUser friend = user.friends().getFirst();
        openPeople(friendSession.page(), friend)
                .openFriends()
                .clickRemove(user.username())
                .shouldHaveNotification(Notification.FRIEND_DELETED);
        openPeople(user)
                .openAllPeople()
                .search(friend.username())
                .shouldHaveButtons(friend.username(), PeopleButton.ADD);
    }
}
