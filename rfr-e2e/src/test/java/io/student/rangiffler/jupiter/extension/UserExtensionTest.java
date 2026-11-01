package io.student.rangiffler.jupiter.extension;

import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserFixture;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.AuthUserRepository;
import io.student.rangiffler.data.db.Friendship;
import io.student.rangiffler.data.db.FriendshipStatus;
import io.student.rangiffler.jupiter.annotation.User;
import org.junit.jupiter.api.BeforeEach;
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
import java.util.Optional;

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
    @DisplayName("User exists while the test body runs and is removed even when the test fails")
    void shouldRemoveUserWhenTestFails() {
        EngineExecutionResults results = run(FailingTest.class);
        String username = values(results, "store.before").getFirst();

        assertEquals(
                List.of(existence(true), existence(false), "Sample failure of " + username),
                List.of(values(results, "inBody").getFirst(), existence(username), failure(results).getMessage()),
                "User was not created for the failing test, was not removed after it or the test did not fail as the sample does"
        );
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
    @DisplayName("User exists before the test and is removed when @User sits on a parameter of another type")
    void shouldRemoveUserWhenParameterTypeIsWrong() {
        EngineExecutionResults results = run(WrongType.class);
        String username = values(results, "store.before").getFirst();

        assertEquals(
                List.of(existence(true), existence(false)),
                List.of(values(results, "exists.before").getFirst(), existence(username)),
                "User was not created for the unresolved parameter or was not removed after the test"
        );
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
    @DisplayName("Accepted friendship exists while the test runs and neither it nor the friend profile is left after")
    void shouldRemoveFriendshipAndFriendProfileAfterTest() {
        EngineExecutionResults results = run(WithFriend.class);
        String user = values(results, "store.before").getFirst();
        String friend = values(results, "partner").getFirst();

        assertEquals(
                List.of("true", "false", existence(false)),
                List.of(
                        values(results, "friendship").getFirst(),
                        String.valueOf(ApiUserRepository.friendshipExists(new Friendship(user, friend, FriendshipStatus.ACCEPTED))),
                        existence(friend)
                ),
                "Friendship was not created for the test or it or the friend profile was left after it"
        );
    }

    @Test
    @DisplayName("Incoming invitation exists while the test runs and neither it nor the inviter profile is left after")
    void shouldRemoveIncomeInvitationAndInviterProfileAfterTest() {
        EngineExecutionResults results = run(WithIncomeInvitation.class);
        String user = values(results, "store.before").getFirst();
        String inviter = values(results, "partner").getFirst();

        assertEquals(
                List.of("true", "false", existence(false)),
                List.of(
                        values(results, "friendship").getFirst(),
                        String.valueOf(ApiUserRepository.friendshipExists(new Friendship(inviter, user, FriendshipStatus.PENDING))),
                        existence(inviter)
                ),
                "Invitation was not created for the test or it or the inviter profile was left after it"
        );
    }

    @Test
    @DisplayName("Negative outcomeInvitations fails with IllegalArgumentException before any user is created")
    void shouldRejectNegativeOutcomeInvitationsBeforeCreatingUser() {
        EngineExecutionResults results = run(NegativeOutcomeInvitations.class);

        assertEquals(
                List.of(
                        IllegalArgumentException.class,
                        "User request counts cannot be negative: friends=0, incomeInvitations=0, outcomeInvitations=-1",
                        List.of("absent")
                ),
                List.of(failure(results).getClass(), failure(results).getMessage(), values(results, "store.after")),
                "Negative count did not fail for that reason before the user was created"
        );
    }

    @Test
    @DisplayName("A test with two @User parameters fails before any user is created instead of sharing one user")
    void shouldFailTestWithTwoUserParameters() {
        EngineExecutionResults results = run(TwoUserParameters.class);

        assertEquals(
                List.of("Test method has 2 @User parameters, only one is supported", List.of("absent")),
                List.of(failure(results).getMessage(), values(results, "store.after")),
                "Test with two @User parameters did not fail for that reason before the user was created"
        );
    }

    @Test
    @DisplayName("@User outside the test method fails with a parameter resolution error")
    void shouldFailWhenUserIsNotOnTestMethod() {
        assertEquals(
                ParameterResolutionException.class,
                failure(run(OnLifecycleMethod.class)).getClass(),
                "@User on a lifecycle method did not fail with a parameter resolution error"
        );
    }

    @Test
    @DisplayName("@User on a lifecycle method fails even when the test method has its own, and that user is removed")
    void shouldFailWhenUserSitsOnLifecycleMethodToo() {
        EngineExecutionResults results = run(UserOnLifecycleAndTestMethod.class);
        String username = values(results, "store.before").getFirst();

        assertEquals(
                List.of(ParameterResolutionException.class, existence(false)),
                List.of(failure(results).getClass(), existence(username)),
                "Lifecycle parameter silently shared the user or the user was left after the failure"
        );
    }

    @Test
    @DisplayName("User exists while the test body runs and is removed even when another after-each callback fails")
    void shouldRemoveUserWhenAnotherAfterEachCallbackFails() {
        EngineExecutionResults results = run(FailingAfterEachCallback.class);
        String username = values(results, "store.before").getFirst();

        assertEquals(
                List.of(existence(true), existence(false), "Sample after-each failure"),
                List.of(values(results, "inBody").getFirst(), existence(username), failure(results).getMessage()),
                "User was not created for the test, was not removed after another callback failed or that callback did not fail"
        );
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
     * Reports the username held in the extension's store before and after each test, and whether that user exists before the test, so the outer test can see the fixture's lifecycle.
     */
    static final class StoreProbe implements BeforeEachCallback, AfterEachCallback {

        private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace.create(UserExtension.class);

        @Override
        public void beforeEach(ExtensionContext context) {
            context.publishReportEntry("store.before", held(context).orElse("absent"));
            context.publishReportEntry("exists.before", held(context).map(username -> existence(username)).orElse("absent"));
        }

        @Override
        public void afterEach(ExtensionContext context) {
            context.publishReportEntry("store.after", held(context).orElse("absent"));
        }

        private static Optional<String> held(ExtensionContext context) {
            return Optional.ofNullable(context.getStore(NAMESPACE).get(context.getUniqueId(), UserFixture.class))
                    .map(fixture -> fixture.user().username());
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
     * Sample test that reports whether its user exists and then fails.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class FailingTest {

        @Test
        void body(@User TestUser user, TestReporter reporter) {
            reporter.publishEntry("inBody", existence(user.username()));
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
        void body(@User(friends = 1) TestUser first, @User(friends = 5) TestUser second) {
        }
    }

    /**
     * Sample test that asks for a user on a lifecycle method instead of the test method.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class OnLifecycleMethod {

        @BeforeEach
        void setUp(@User TestUser user) {
        }

        @Test
        void body() {
        }
    }

    /**
     * Sample test that asks for a user both on a lifecycle method and on the test method.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class UserOnLifecycleAndTestMethod {

        @BeforeEach
        void setUp(@User TestUser user) {
        }

        @Test
        void body(@User(friends = 2) TestUser user) {
        }
    }

    /**
     * Sample test whose after-each callback fails before the user is cleaned up.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class, FailingAfterEach.class})
    static class FailingAfterEachCallback {

        @Test
        void body(@User TestUser user, TestReporter reporter) {
            reporter.publishEntry("inBody", existence(user.username()));
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
     * Sample test that reports the friend and whether the accepted friendship with it exists while the body runs.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WithFriend {

        @Test
        void body(@User(friends = 1) TestUser user, TestReporter reporter) {
            String friend = user.friends().getFirst().username();
            reporter.publishEntry("partner", friend);
            reporter.publishEntry("friendship", String.valueOf(
                    ApiUserRepository.friendshipExists(new Friendship(user.username(), friend, FriendshipStatus.ACCEPTED))
            ));
        }
    }

    /**
     * Sample test that reports the inviter and whether the pending invitation from it exists while the body runs.
     */
    @ExtendWith({TestkitOnly.class, UserExtension.class, StoreProbe.class})
    static class WithIncomeInvitation {

        @Test
        void body(@User(incomeInvitations = 1) TestUser user, TestReporter reporter) {
            String inviter = user.incomeInvitations().getFirst().username();
            reporter.publishEntry("partner", inviter);
            reporter.publishEntry("friendship", String.valueOf(
                    ApiUserRepository.friendshipExists(new Friendship(inviter, user.username(), FriendshipStatus.PENDING))
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
