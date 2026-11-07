package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.ProfilePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class Header {

    private final Page page;
    private final Locator profileLink;
    private final Locator logoutButton;

    public Header(Page page) {
        this.page = page;
        this.profileLink = page.getByRole(AriaRole.BANNER).getByRole(AriaRole.LINK, new Locator.GetByRoleOptions().setName("Profile"));
        this.logoutButton = page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName("Logout"));
    }

    @Step("Open profile from header")
    public ProfilePage openProfile() {
        profileLink.click();
        return new ProfilePage(page);
    }

    @Step("Check that header is visible")
    public Header shouldBeVisible() {
        assertThat(profileLink).isVisible();
        assertThat(logoutButton).isVisible();
        return this;
    }
}
