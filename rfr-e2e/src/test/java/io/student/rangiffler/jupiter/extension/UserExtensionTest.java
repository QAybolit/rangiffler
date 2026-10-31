package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.AuthUserRepository;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample tests through the JUnit engine to check when {@link UserExtension} creates, resolves and closes the user fixture.
 */
@DisplayName("UserExtension creates, resolves and closes the user fixture")
@Tag("extension")
class UserExtensionTest {

    @Test
    @DisplayName("User exists in both databases while the test body runs and is gone after the test")
    void shouldCreateUserBeforeBodyAndRemoveItAfter() {
        EngineExecutionResults results = run(CreatesUser.class);
        String username = values(results, "store.before").getFirst();

        assertEquals(
                List.of(existence(true), existence(false)),
                List.of(values(results, "inBody").getFirst(), existence(username)),
                "User was not created before the body or was not removed after the test"
        );
    }

    @Test
    @DisplayName("Fixture is not created for a test without a @User parameter")
    void shouldNotCreateFixtureWithoutUserParameter() {
        assertEquals(
                List.of("absent"),
                values(run(WithoutUser.class), "store.before"),
                "Fixture was created for a test that has no @User parameter"
        );
    }

    @Test
    @DisplayName("User is removed even when the test fails")
    void shouldRemoveUserWhenTestFails() {
        String username = values(run(FailingTest.class), "store.before").getFirst();

        assertEquals(existence(false), existence(username), "User was not removed after the failed test");
    }

    @Test
    @DisplayName("Every test gets its own user")
    void shouldGiveEachTestItsOwnUser() {
        List<String> usernames = values(run(TwoTests.class), "store.before");

        assertEquals(2, new HashSet<>(usernames).size(), "Tests shared one user instead of getting their own");
    }

    @Test
    @DisplayName("Friends and invitations from the annotation reach the resolved user")
    void shouldPassFriendsAndInvitationsFromAnnotation() {
        assertEquals(
                List.of("2,1"),
                values(run(WithRelations.class), "relations"),
                "Friends and invitations from the annotation did not reach the user"
        );
    }

    @Test
    @DisplayName("User is removed when @User sits on a parameter of another type")
    void shouldRemoveUserWhenParameterTypeIsWrong() {
        String username = values(run(WrongType.class), "store.before").getFirst();

        assertEquals(existence(false), existence(username), "User was not removed after the unresolved parameter");
    }

    @Test
    @DisplayName("@User on a parameter of another type fails the test with a parameter resolution error")
    void shouldFailTestWhenParameterTypeIsWrong() {
        assertEquals(
                ParameterResolutionException.class,
                failure(run(WrongType.class)).getClass(),
                "Test did not fail with a parameter resolution error"
        );
    }

    @Test
    @DisplayName("Outgoing invitation exists while the test runs and neither it nor the invited profile is left after")
    void shouldRemoveOutcomeInvitationAndInvitedProfileAfterTest() {
        EngineExecutionResults results = run(WithOutcomeInvitation.class);
        String user = values(results, "store.before").getFirst();
        String invitee = values(results, "invitee").getFirst();

        assertEquals(
                List.of("true", "false", existence(false)),
                List.of(
                        values(results, "friendship").getFirst(),
                        String.valueOf(ApiUserRepository.friendshipExists(new Friendship(user, invitee, FriendshipStatus.PENDING))),
                        existence(invitee)
                ),
                "Invitation was not created for the test or it or the invited profile was left after it"
        );
    }

    @Test
    @DisplayName("Negative outcomeInvitations fails with IllegalArgumentException before any user is created")
    void shouldRejectNegativeOutcomeInvitationsBeforeCreatingUser() {
        EngineExecutionResults results = run(NegativeOutcomeInvitations.class);

        assertEquals(
                List.of(IllegalArgumentException.class, List.of("absent")),
                List.of(failure(results).getClass(), values(results, "store.after")),
                "Negative count did not fail before the user was created"
        );
    }

    @Test
    @DisplayName("Two @User parameters of one test get the same user created from the first annotation")
    void shouldGiveTwoUserParametersTheSameUser() {
        assertEquals(
                List.of("true,1"),
                values(run(TwoUserParameters.class), "parameters"),
                "Two @User parameters did not share the user created from the first annotation"
        );
    }

    @Test
    @DisplayName("User is removed even when another after-each callback fails")
    void shouldRemoveUserWhenAnotherAfterEachCallbackFails() {
        String username = values(run(FailingAfterEachCallback.class), "store.before").getFirst();

        assertEquals(existence(false), existence(username), "User was not removed after another callback failed");
    }

    private static EngineExecutionResults run(Class<?> sample) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(sample))
                .configurationParameter("rfr.testkit", "true")
                .execute();
    }

    private static Throwable failure(EngineExecutionResults results) {
        return results.testEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
                .findFirst()
                .orElseThrow();
    }

    private static List<String> values(EngineExecutionResults results, String key) {
        return results.allEvents().reportingEntryPublished().stream()
                .map(event -> event.getRequiredPayload(ReportEntry.class).getKeyValuePairs())
                .filter(pairs -> pairs.containsKey(key))
                .map(pairs -> pairs.get(key))
                .toList();
    }

    private static String existence(String username) {
        return "auth=" + AuthUserRepository.userExists(username) + ",api=" + ApiUserRepository.userExists(username);
    }

    private static String existence(boolean exists) {
        return "auth=" + exists + ",api=" + exists;
    }

    /**
     * Lets sample tests run only inside {@code UserExtensionTest}, which sets the configuration parameter, so the build never runs them on its own.
     */
    static final class TestkitOnly implements ExecutionCondition {

        @Override
        public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
            return context.getConfigurationParameter("rfr.testkit").filter("true"::equals)
                    .map(value -> ConditionEvaluationResult.enabled("Run by UserExtensionTest"))
                    .orElseGet(() -> ConditionEvaluationResult.disabled("Sample test of UserExtensionTest"));
        }
    }

    /**
     * Reports the username held in the extension's store before and after each test, so the outer test can see the fixture's lifecycle.
     */
    static final class StoreProbe implements BeforeEachCallback, AfterEachCallback {

        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

        @Override
        public void beforeEach(ExtensionContext context) {
            context.publishReportEntry("store.before", held(context));
        }

        @Override
        public void afterEach(ExtensionContext context) {
            context.publishReportEntry("store.after", held(context));
        }

        private static String held(ExtensionContext context) {
            UserFixture fixture = context.getStore(NAMESPACE).get(context.getUniqueId(), UserFixture.class);
            return fixture == null ? "absent" : fixture.user().username();
        }
    }

    /**
     * Fails each test's after-each phase; registered after {@code UserExtension}, so it runs before the extension's cleanup.
     */
    static final class FailingAfterEach implements AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) {
            throw new IllegalStateException("Sample after-each failure");
        }
    }

    /**
     * Sample test that reports whether its user exists in both databases while the body runs.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class CreatesUser {

        @Test
        void body(@User TestUser user, TestReporter reporter) {
            reporter.publishEntry("inBody", existence(user.username()));
        }
    }

    /**
     * Sample test without a {@code @User} parameter.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WithoutUser {

        @Test
        void body(TestReporter reporter) {
            reporter.publishEntry("body", "ran");
        }
    }

    /**
     * Sample test that fails after its user is created.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class FailingTest {

        @Test
        void body(@User TestUser user) {
            throw new IllegalStateException("Sample failure of " + user.username());
        }
    }

    /**
     * Sample class with two tests that each ask for a user.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class TwoTests {

        @Test
        void first(@User TestUser user) {
        }

        @Test
        void second(@User TestUser user) {
        }
    }

    /**
     * Sample test that reports how many friends and inviters its user has.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WithRelations {

        @Test
        void body(@User(friends = 2, incomeInvitations = 1) TestUser user, TestReporter reporter) {
            reporter.publishEntry("relations", user.friends().size() + "," + user.incomeInvitations().size());
        }
    }

    /**
     * Sample test with two {@code @User} parameters that ask for different numbers of friends.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class TwoUserParameters {

        @Test
        void body(@User(friends = 1) TestUser first, @User(friends = 5) TestUser second, TestReporter reporter) {
            reporter.publishEntry("parameters", first.username().equals(second.username()) + "," + second.friends().size());
        }
    }

    /**
     * Sample test whose after-each callback fails before the user is cleaned up.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class, FailingAfterEach.class})
    static class FailingAfterEachCallback {

        @Test
        void body(@User TestUser user) {
        }
    }

    /**
     * Sample test that reports the invited user and whether the pending invitation to it exists while the body runs.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WithOutcomeInvitation {

        @Test
        void body(@User(outcomeInvitations = 1) TestUser user, TestReporter reporter) {
            String invitee = user.outcomeInvitations().getFirst().username();
            reporter.publishEntry("invitee", invitee);
            reporter.publishEntry("friendship", String.valueOf(
                    ApiUserRepository.friendshipExists(new Friendship(user.username(), invitee, FriendshipStatus.PENDING))
            ));
        }
    }

    /**
     * Sample test whose annotation asks for a negative number of outgoing invitations.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class NegativeOutcomeInvitations {

        @Test
        void body(@User(outcomeInvitations = -1) TestUser user) {
        }
    }

    /**
     * Sample test with {@code @User} on a parameter the extension does not support.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WrongType {

        @Test
        void body(@User String user) {
        }
    }
}
