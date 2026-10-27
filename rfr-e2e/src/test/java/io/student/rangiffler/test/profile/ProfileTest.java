package io.student.rangiffler.test.profile;

import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.ProfilePage;
import io.student.rangiffler.ui.page.WelcomePage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static io.student.rangiffler.utils.RandomDataUtils.randomFirstName;
import static io.student.rangiffler.utils.RandomDataUtils.randomSurname;

@Epic("Profile")
@Feature("Profile update")
@Tag("ui")
class ProfileTest extends BaseWebTest {

    private static final String DEFAULT_COUNTRY = "Russian Federation";
    private static final String FRANCE = "France";
    private static final String GERMANY = "Germany";

    @Test
    @DisplayName("First name is saved and shown after reload")
    void shouldSaveFirstName(@User TestUser user) {
        String firstName = randomFirstName();

        openProfile(user)
                .fillForm(firstName, "", DEFAULT_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveFirstName(firstName)
                .shouldHaveSurname("")
                .shouldHaveLocation(DEFAULT_COUNTRY);
    }

    @Test
    @DisplayName("Surname is saved and shown after reload")
    void shouldSaveSurname(@User TestUser user) {
        String surname = randomSurname();

        openProfile(user)
                .fillForm("", surname, DEFAULT_COUNTRY)
                .clickSave()
                .reload()
                .shouldHaveFirstName("")
                .shouldHaveSurname(surname)
                .shouldHaveLocation(DEFAULT_COUNTRY);
    }

    @Test
    @DisplayName("Location can be changed and the new country is shown after reload")
    void shouldChangeLocation(@User TestUser user) {
        openProfile(user)
                .shouldHaveLocation(DEFAULT_COUNTRY)
                .fillForm("", "", FRANCE)
                .clickSave()
                .reload()
                .shouldHaveLocation(FRANCE)
                .fillForm("", "", GERMANY)
                .clickSave()
                .reload()
                .shouldHaveLocation(GERMANY);
    }

    @Test
    @DisplayName("Avatar is saved and shown after reload")
    void shouldSaveAvatar(@User TestUser user) throws URISyntaxException {
        openProfile(user)
                .uploadAvatar(resource("img/avatar.png"))
                .shouldHaveAvatar()
                .clickSave()
                .reload()
                .shouldHaveAvatar();
    }

    @Test
    @DisplayName("Reset returns the last saved values")
    void shouldResetToSavedValues(@User TestUser user) {
        String firstName = randomFirstName();
        String surname = randomSurname();

        openProfile(user)
                .fillForm(firstName, surname, FRANCE)
                .clickSave()
                .reload()
                .fillForm(firstName + "x", surname + "x", GERMANY)
                .reset()
                .shouldHaveFirstName(firstName)
                .shouldHaveSurname(surname)
                .shouldHaveLocation(FRANCE);
    }

    private ProfilePage openProfile(TestUser user) {
        return new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password())
                .header()
                .openProfile();
    }

    private Path resource(String name) throws URISyntaxException {
        return Paths.get(getClass().getClassLoader().getResource(name).toURI());
    }
}
