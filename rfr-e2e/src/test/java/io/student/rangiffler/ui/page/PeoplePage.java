package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

    public static final String URL = TestConfig.frontUrl() + "/people";

    private final Page page;
    private final Locator friendsTab;
    private final Locator allPeopleTab;
    private final Locator incomeInvitationsTab;
    private final Locator outcomeInvitationsTab;
    private final Locator searchInput;
    private final Locator rows;

    public PeoplePage(Page page) {
        this.page = page;
        this.friendsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Friends"));
        this.allPeopleTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("All People"));
        this.incomeInvitationsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Income invitations"));
        this.outcomeInvitationsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Outcome invitations"));
        this.searchInput = page.getByPlaceholder("Search people");
        this.rows = page.locator("tbody tr");
    }

    @Step("Open 'Friends' tab")
    public PeoplePage openFriends() {
        friendsTab.click();
        return this;
    }

    @Step("Open 'All People' tab")
    public PeoplePage openAllPeople() {
        allPeopleTab.click();
        return this;
    }

    @Step("Open 'Income invitations' tab")
    public PeoplePage openIncomeInvitations() {
        incomeInvitationsTab.click();
        return this;
    }

    @Step("Open 'Outcome invitations' tab")
    public PeoplePage openOutcomeInvitations() {
        outcomeInvitationsTab.click();
        return this;
    }

    @Step("Search people by '{username}'")
    public PeoplePage search(String username) {
        searchInput.fill(username);
        searchInput.press("Enter");
        return this;
    }

    @Step("Click 'Add' for '{username}' and wait for FriendshipAction mutation")
    public PeoplePage clickAdd(String username) {
        Locator addButton = row(username).getByRole(AriaRole.BUTTON, new Locator.GetByRoleOptions().setName("Add"));
        page.waitForResponse(
                response -> response.url().endsWith("/graphql")
                        && response.request().postData() != null
                        && response.request().postData().contains("FriendshipAction"),
                addButton::click
        );
        return this;
    }

    @Step("Check that the table lists exactly the users {usernames}")
    public PeoplePage shouldHaveUsers(String... usernames) {
        assertThat(rows).hasCount(usernames.length);
        for (String username : usernames) {
            assertThat(row(username)).hasCount(1);
        }
        return this;
    }

    @Step("Check that the invitation for '{username}' is waiting")
    public PeoplePage shouldHaveInvitationSent(String username) {
        assertThat(row(username).getByText("Waiting...")).isVisible();
        return this;
    }

    private Locator row(String username) {
        return rows.filter(new Locator.FilterOptions().setHasText(username));
    }
}
