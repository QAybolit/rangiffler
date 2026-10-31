package io.student.rangiffler.test.people;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.test.BaseWebTest;
import io.student.rangiffler.ui.page.PeoplePage;
import io.student.rangiffler.ui.page.WelcomePage;

/**
 * Base class for the tests of the People page tabs: logs in as the test user and opens the page.
 */
abstract class BasePeopleTest extends BaseWebTest {

    PeoplePage openPeople(TestUser user) {
        return new WelcomePage(page)
                .open()
                .clickLogin()
                .login(user.username(), user.password())
                .sidebar()
                .openPeople();
    }
}
