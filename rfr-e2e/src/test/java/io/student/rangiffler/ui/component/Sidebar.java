package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.PeoplePage;

public final class Sidebar {

    private final Page page;
    private final Locator peopleLink;

    public Sidebar(Page page) {
        this.page = page;
        this.peopleLink = page.getByRole(AriaRole.LINK, new Page.GetByRoleOptions().setName("People"));
    }

    @Step("Open people from sidebar and wait for GetFriends query")
    public PeoplePage openPeople() {
        page.waitForResponse(
                response -> response.url().endsWith("/graphql")
                        && response.request().postData() != null
                        && response.request().postData().contains("GetFriends"),
                peopleLink::click
        );
        return new PeoplePage(page);
    }
}
