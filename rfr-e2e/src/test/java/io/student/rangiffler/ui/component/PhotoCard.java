package io.student.rangiffler.ui.component;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import io.qameta.allure.Step;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class PhotoCard {

    private final Locator root;

    public PhotoCard(Locator root) {
        this.root = root;
    }

    @Step("Click 'like' and wait for LikePhoto mutation")
    public PhotoCard clickLike() {
        root.page().waitForResponse(
                response -> response.url().endsWith("/graphql")
                        && response.request().postData() != null
                        && response.request().postData().contains("LikePhoto"),
                () -> button("like").click()
        );
        return this;
    }

    @Step("Open the photo actions")
    public PhotoCard openActions() {
        button("Photo actions").click();
        return this;
    }

    @Step("Check that the photo is from {country}")
    public PhotoCard shouldHaveCountry(String country) {
        assertThat(root.getByText(country, new Locator.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Step("Check that the photo has {total} likes")
    public PhotoCard shouldHaveLikes(int total) {
        assertThat(root.getByText(total + (total == 1 ? " like" : " likes"), new Locator.GetByTextOptions().setExact(true))).isVisible();
        return this;
    }

    @Step("Check that the photo offers the actions")
    public PhotoCard shouldHaveActions() {
        assertThat(button("Photo actions")).isVisible();
        return this;
    }

    @Step("Check that the photo does not offer the actions")
    public PhotoCard shouldNotHaveActions() {
        assertThat(button("Photo actions")).hasCount(0);
        return this;
    }

    private Locator button(String name) {
        return root.page().getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(name).setExact(true));
    }
}
