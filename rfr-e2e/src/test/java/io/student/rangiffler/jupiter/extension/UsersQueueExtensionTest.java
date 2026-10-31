package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample tests through the JUnit engine to check that {@link UsersQueueExtension} lends the pooled users out and always gets them back.
 */
@DisplayName("UsersQueueExtension lends pooled users and gets them back")
@Tag("extension")
class UsersQueueExtensionTest {

    @Test
    @DisplayName("Four tests in a row pass on a pool of three users")
    void shouldServeMoreTestsThanUsersInPool() {
        assertEquals(
                4L,
                run(FourTests.class).testEvents().succeeded().count(),
                "Not every one of four tests got a user from the pool of three"
        );
    }

    @Test
    @DisplayName("A user is back in the pool after a failed test")
    void shouldReturnUserAfterFailedTest() {
        for (int run = 0; run < 3; run++) {
            run(FailingTest.class);
        }

        assertEquals(
                1L,
                run(PassingTest.class).testEvents().succeeded().count(),
                "A user of the failed tests was not given back to the pool"
        );
    }

    private static EngineExecutionResults run(Class<?> sample) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(sample))
                .configurationParameter("rfr.testkit", "true")
                .execute();
    }

    /**
     * Sample class with more tests than the pool has users.
     */
    @ExtendWith({UserExtensionTest.TestkitOnly.class, UsersQueueExtension.class})
    static class FourTests {

        @Test
        void first(@StaticUser TestUser user) {
        }

        @Test
        void second(@StaticUser TestUser user) {
        }

        @Test
        void third(@StaticUser TestUser user) {
        }

        @Test
        void fourth(@StaticUser TestUser user) {
        }
    }

    /**
     * Sample test that fails while it holds a pooled user.
     */
    @ExtendWith({UserExtensionTest.TestkitOnly.class, UsersQueueExtension.class})
    static class FailingTest {

        @Test
        void body(@StaticUser TestUser user) {
            throw new IllegalStateException("Sample failure of " + user.username());
        }
    }

    /**
     * Sample test that only needs a pooled user.
     */
    @ExtendWith({UserExtensionTest.TestkitOnly.class, UsersQueueExtension.class})
    static class PassingTest {

        @Test
        void body(@StaticUser TestUser user) {
        }
    }
}
