package io.student.rangiffler.ui.page;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.config.TestConfig;
import io.student.rangiffler.ui.component.PeopleTable;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeoplePage {

    public static final String URL = TestConfig.frontUrl() + "/people";

    private final Page page;
    private final Locator friendsTab;
    private final Locator allPeopleTab;
    private final Locator incomeInvitationsTab;
    private final Locator outcomeInvitationsTab;
    private final Locator searchInput;
    private final PeopleTable table;

    public PeoplePage(Page page) {
        this.page = page;
        this.friendsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Friends"));
        this.allPeopleTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("All People"));
        this.incomeInvitationsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Income invitations"));
        this.outcomeInvitationsTab = page.getByRole(AriaRole.TAB, new Page.GetByRoleOptions().setName("Outcome invitations"));
        this.searchInput = page.getByPlaceholder("Search people");
        this.table = new PeopleTable(page);
    }

    @Step("Open 'Friends' tab")
    public PeoplePage openFriends() {
        open(friendsTab);
        return this;
    }

    @Step("Open 'All People' tab")
    public PeoplePage openAllPeople() {
        open(allPeopleTab);
        return this;
    }

    @Step("Open 'Income invitations' tab")
    public PeoplePage openIncomeInvitations() {
        open(incomeInvitationsTab);
        return this;
    }

    @Step("Open 'Outcome invitations' tab")
    public PeoplePage openOutcomeInvitations() {
        open(outcomeInvitationsTab);
        return this;
    }

    @Step("Search people by '{username}'")
    public PeoplePage search(String username) {
        searchInput.fill(username);
        searchInput.press("Enter");
        return this;
    }

    @Step("Click 'Add' for '{username}'")
    public PeoplePage clickAdd(String username) {
        table.row(username).clickAdd();
        return this;
    }

    @Step("Check that the table lists exactly the users {usernames}")
    public PeoplePage shouldHaveUsers(String... usernames) {
        table.shouldHaveUsers(usernames);
        return this;
    }

    @Step("Check that the table does not list the users {usernames}")
    public PeoplePage shouldNotHaveUsers(String... usernames) {
        table.shouldNotHaveUsers(usernames);
        return this;
    }

    @Step("Check that '{username}' has the buttons {buttons}")
    public PeoplePage shouldHaveButtons(String username, String... buttons) {
        table.row(username).shouldHaveButtons(buttons);
        return this;
    }

    @Step("Check that the invitation for '{username}' is waiting")
    public PeoplePage shouldHaveInvitationSent(String username) {
        table.row(username).shouldHaveInvitationSent();
        return this;
    }

    @Step("Check that the table is empty")
    public PeoplePage shouldBeEmpty() {
        table.shouldBeEmpty();
        return this;
    }

    private void open(Locator tab) {
        tab.click();
        assertThat(tab).hasAttribute("aria-selected", "true");
    }
}
