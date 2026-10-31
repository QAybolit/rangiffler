package io.student.rangiffler.test.friends;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.SingleUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;

@Epic("Friends")
@Feature("Friendship fixtures")
@Tag("ui")
class FriendshipTest extends BaseWebTest {

    @Test
    @DisplayName("Friends created by the fixture are listed on the Friends tab")
    void shouldListFriendsCreatedByFixture(@User(friends = 2) TestUser user) {
        openPeople(user)
                .openFriends()
                .shouldHaveUsers(usernames(user.friends()));
    }

    @Test
    @DisplayName("Invitations created by the fixture are listed on the Income invitations tab")
    void shouldListIncomeInvitationsCreatedByFixture(@User(incomeInvitations = 2) TestUser user) {
        openPeople(user)
                .openIncomeInvitations()
                .shouldHaveUsers(usernames(user.incomeInvitations()));
    }

    @Test
    @DisplayName("Friends and invitations created together by the fixture are listed on their own tabs")
    void shouldListFriendsAndIncomeInvitationsCreatedTogether(@User(friends = 2, incomeInvitations = 1) TestUser user) {
        PeoplePage people = openPeople(user);
        people.openFriends().shouldHaveUsers(usernames(user.friends()));
        people.openIncomeInvitations().shouldHaveUsers(usernames(user.incomeInvitations()));
    }

    @Test
    @DisplayName("Invitation sent in the browser is shown as waiting")
    void shouldShowInvitationSentInBrowserAsWaiting() {
        try (UserFixture sender = SingleUserFixture.create(); UserFixture addressee = SingleUserFixture.create()) {
            openPeople(sender.user())
                    .openAllPeople()
                    .search(addressee.user().username())
                    .clickAdd(addressee.user().username())
                    .shouldHaveInvitationSent(addressee.user().username());
        }
    }

    private PeoplePage openPeople(TestUser user) {
        return new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password())
                .sidebar()
                .openPeople();
    }

    private String[] usernames(List<TestUser> users) {
        return users.stream().map(TestUser::username).toArray(String[]::new);
    }
}
