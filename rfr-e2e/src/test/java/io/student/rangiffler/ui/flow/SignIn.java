package io.student.rangiffler.ui.flow;

import com.microsoft.playwright.Page;
import io.qameta.allure.Step;
import io.student.rangiffler.ui.page.MyTravelsPage;
import io.student.rangiffler.ui.page.WelcomePage;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

public final class SignIn {

    @Step("Sign in as '{username}' and wait for the main page")
    public void signIn(Page page, String username, String password) {
        new WelcomePage(page).open().clickLogin().login(username, password);
        assertThat(page).hasURL(MyTravelsPage.URL);
    }
}
