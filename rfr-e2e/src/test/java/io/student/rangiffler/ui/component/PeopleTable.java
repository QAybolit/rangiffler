package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PeopleTable {

    private final Page page;
    private final Locator rows;
    private final Locator emptyState;

    public PeopleTable(Page page) {
        this.page = page;
        this.rows = page.getByRole(AriaRole.TABLE).getByRole(AriaRole.ROW).filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL)));
        this.emptyState = page.getByText("There are no users yet");
    }

    public PeopleRow row(String username) {
        return new PeopleRow(rowOf(username));
    }

    @Step("Check that the table lists exactly the users {usernames}")
    public PeopleTable shouldHaveUsers(String... usernames) {
        assertThat(rows).hasCount(usernames.length);
        for (String username : usernames) {
            assertThat(rowOf(username)).hasCount(1);
        }
        return this;
    }

    @Step("Check that the table does not list the users {usernames}")
    public PeopleTable shouldNotHaveUsers(String... usernames) {
        for (String username : usernames) {
            assertThat(rowOf(username)).hasCount(0);
        }
        return this;
    }

    @Step("Check that the table is empty")
    public PeopleTable shouldBeEmpty() {
        assertThat(emptyState).isVisible();
        assertThat(rows).hasCount(0);
        return this;
    }

    private Locator rowOf(String username) {
        return rows.filter(new Locator.FilterOptions().setHas(page.getByRole(AriaRole.CELL, new Page.GetByRoleOptions().setName(username).setExact(true))));
    }
}
