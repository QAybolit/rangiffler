package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.StaticUserFixture;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.jupiter.annotation.StaticUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    @DisplayName("Every user is back in the pool after failed tests")
    void shouldReturnUserAfterFailedTest() {
        long failed = IntStream.range(0, 3)
                .mapToLong(run -> run(FailingTest.class).testEvents().failed().count())
                .sum();

        assertEquals(
                List.of(3L, 1L),
                List.of(failed, run(HoldsWholePool.class).testEvents().succeeded().count()),
                "Sample tests did not fail or a user of the failed tests was not given back to the pool"
        );
    }

    @Test
    @DisplayName("A test with two @StaticUser parameters fails instead of sharing one user")
    void shouldFailTestWithTwoStaticUserParameters() {
        assertEquals(
                "Test method has 2 @StaticUser parameters, only one is supported",
                failure(run(TwoStaticUsers.class)).getMessage(),
                "Test with two @StaticUser parameters did not fail for that reason"
        );
    }

    @Test
    @DisplayName("@StaticUser outside the test method fails with a parameter resolution error")
    void shouldFailWhenStaticUserIsNotOnTestMethod() {
        assertEquals(
                ParameterResolutionException.class,
                failure(run(OnLifecycleMethod.class)).getClass(),
                "@StaticUser on a lifecycle method did not fail with a parameter resolution error"
        );
    }

    @Test
    @DisplayName("Giving a user back twice fails instead of putting it in the pool twice")
    void shouldFailWhenUserIsGivenBackTwice() {
        StaticUserFixture fixture = StaticUserFixture.create();
        fixture.close();

        assertThrows(IllegalStateException.class, fixture::close, "Second return of one user did not fail");
    }

    @Test
    @DisplayName("A closed fixture cannot give back a user that another test already holds")
    void shouldNotLetClosedFixtureTakeUserFromAnotherTest() {
        StaticUserFixture closed = StaticUserFixture.create();
        closed.close();
        List<StaticUserFixture> others = IntStream.range(0, 3).mapToObj(number -> StaticUserFixture.create()).toList();

        try {
            assertThrows(IllegalStateException.class, closed::close, "Closed fixture gave back a user that was lent again");
        } finally {
            others.forEach(StaticUserFixture::close);
        }
    }

    private static Throwable failure(EngineExecutionResults results) {
        return results.testEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
                .findFirst()
                .orElseThrow();
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
     * Sample test that takes every user of the pool at once, so it passes only if none was lost earlier.
     */
    @ExtendWith(UserExtensionTest.TestkitOnly.class)
    static class HoldsWholePool {

        @Test
        void body() {
            try (
                    StaticUserFixture first = StaticUserFixture.create();
                    StaticUserFixture second = StaticUserFixture.create();
                    StaticUserFixture third = StaticUserFixture.create()
            ) {
                assertEquals(3, Set.of(first.user(), second.user(), third.user()).size(), "Pool lent one user twice");
            }
        }
    }

    /**
     * Sample test that asks for two pooled users.
     */
    @ExtendWith({UserExtensionTest.TestkitOnly.class, UsersQueueExtension.class})
    static class TwoStaticUsers {

        @Test
        void body(@StaticUser TestUser first, @StaticUser TestUser second) {
        }
    }

    /**
     * Sample test that asks for a pooled user on a lifecycle method instead of the test method.
     */
    @ExtendWith({UserExtensionTest.TestkitOnly.class, UsersQueueExtension.class})
    static class OnLifecycleMethod {

        @BeforeEach
        void setUp(@StaticUser TestUser user) {
        }

        @Test
        void body() {
        }
    }
}
