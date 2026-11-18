package io.student.rangiffler.jupiter.probe;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.impl.junit.BrowserExtension;
import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.data.GuestSession;
import io.student.rangiffler.data.TestUser;
import io.student.rangiffler.data.UserSession;
import io.student.rangiffler.data.db.ApiUserRepository;
import io.student.rangiffler.data.db.AuthUserRepository;
import io.student.rangiffler.data.fixture.UserFixture;
import io.student.rangiffler.jupiter.annotation.UiTest;
import io.student.rangiffler.jupiter.annotation.User;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import io.student.rangiffler.ui.page.MyTravelsPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestReporter;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.opentest4j.AssertionFailedError;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample tests through the JUnit engine, each in a thread of its own because Playwright keeps its instance in a thread local and closes it with the engine run, to check when {@link SessionExtension} opens, signs in and closes the additional browser contexts.
 */
@DisplayName("SessionExtension opens, signs in and closes additional browser contexts")
@Tag("ui")
@UsePlaywright(PlaywrightOptions.class)
class SessionExtensionTest {

    private static final String GONE = "auth=false,api=false";

    @Test
    @DisplayName("A guest session is a separate context with nobody signed in and is closed after the test")
    void shouldOpenSeparateGuestContextAndCloseIt() {
        GuestSample.OPENED.clear();
        EngineExecutionResults results = run(GuestSample.class);

        assertEquals(
                List.of("true", "about:blank", "true"),
                List.of(values(results, "separate").getFirst(), values(results, "url").getFirst(), String.valueOf(GuestSample.OPENED.getFirst().isClosed())),
                "Guest session shared the context of the test, was not blank or was left open after the test"
        );
    }

    @Test
    @DisplayName("A user session is signed in as the @User user in a separate context, and both are cleaned up after the test")
    void shouldSignUserInAndCleanUp() {
        UserSample.OPENED.clear();
        EngineExecutionResults results = run(UserSample.class);
        String username = values(results, "username").getFirst();

        assertEquals(
                List.of(MyTravelsPage.URL, "about:blank", "auth=false,api=false", "true"),
                List.of(
                        values(results, "url").getFirst(),
                        values(results, "main").getFirst(),
                        existence(username),
                        String.valueOf(UserSample.OPENED.getFirst().isClosed())
                ),
                "User session was not signed in on its own page, touched the main page or left the context or the user after the test"
        );
    }

    @Test
    @DisplayName("A failed sign-in fails the test and still closes the opened context and removes the user")
    void shouldCloseContextWhenSignInFails() {
        EngineExecutionResults results = run(FailingSignIn.class);
        String username = values(results, "username").getFirst();

        assertEquals(
                List.of(AssertionFailedError.class, "1", GONE),
                List.of(cause(results), values(results, "contexts.after").getFirst(), existence(username)),
                "Failed sign-in did not fail with an assertion, left the session context open or left the user"
        );
    }

    @Test
    @DisplayName("Every @User parameter of a test gets its own user, each session is signed in as its own user, and all of them are cleaned up")
    void shouldGiveEachUserParameterItsOwnUserAndSession() {
        EngineExecutionResults results = run(TwoSessions.class);
        List<String> usernames = values(results, "username");

        assertEquals(
                List.of(3, List.of(MyTravelsPage.URL, MyTravelsPage.URL), List.of("1"), List.of(GONE, GONE, GONE)),
                List.of(
                        (int) usernames.stream().distinct().count(),
                        values(results, "url"),
                        values(results, "contexts.after"),
                        usernames.stream().map(username -> existence(username)).toList()
                ),
                "Parameters did not get three distinct users, were not signed in or left a context or a user after the test"
        );
    }

    @Test
    @DisplayName("A failed sign-in leaves the trace of the session context in the folder of the test")
    void shouldKeepTraceOfSessionContextWhenSignInFails() throws IOException {
        Instant started = Instant.now();
        run(FailingSignIn.class);
        Path trace = Path.of("test-results", "FailingSignIn", "body", "trace-session-1.zip");

        assertEquals(
                true,
                Files.exists(trace) && !Files.getLastModifiedTime(trace).toInstant().isBefore(started.minusSeconds(1)),
                "Failed sign-in did not leave a fresh trace of the session context"
        );
    }

    @Test
    @DisplayName("A session parameter of a class-level method fails with a parameter resolution error")
    void shouldFailSessionOnClassLevelMethod() {
        assertEquals(
                List.of(ParameterResolutionException.class, "A session is supported only on a parameter of the test method"),
                containerFailure(run(SessionOnBeforeAll.class)),
                "Session on a class-level method did not fail with the expected error"
        );
    }

    @Test
    @DisplayName("A user session without UserExtension fails with a parameter resolution error that names the extension and closes the context")
    void shouldFailUserSessionWithoutUserExtension() {
        EngineExecutionResults results = run(WithoutUserExtension.class);

        assertEquals(
                List.of(ParameterResolutionException.class, "@User needs UserExtension, which creates the user before the test", "1"),
                List.of(failure(results).get(0), failure(results).get(1), values(results, "contexts.after").getFirst()),
                "Missing UserExtension did not fail with the expected error or left the session context open"
        );
    }

    @Test
    @DisplayName("UserSession without @User fails with a parameter resolution error")
    void shouldFailUserSessionWithoutUser() {
        assertEquals(
                List.of(ParameterResolutionException.class, "UserSession needs @User, use GuestSession for a session without a user"),
                failure(run(UserSessionWithoutUser.class)),
                "UserSession without @User did not fail with the expected error"
        );
    }

    @Test
    @DisplayName("GuestSession with @User fails with a parameter resolution error and the user is removed")
    void shouldFailGuestSessionWithUser() {
        EngineExecutionResults results = run(GuestSessionWithUser.class);

        assertEquals(
                List.of(ParameterResolutionException.class, "@User is not allowed on GuestSession, a guest has no user", "auth=false,api=false"),
                List.of(failure(results).get(0), failure(results).get(1), existence(GuestSessionWithUser.USERNAMES.getFirst())),
                "GuestSession with @User did not fail with the expected error or left the user after the failure"
        );
    }

    private static EngineExecutionResults run(Class<?> sample) {
        ExecutorService thread = Executors.newSingleThreadExecutor();
        try {
            return thread.submit(() -> EngineTestKit.engine("junit-jupiter")
                    .selectors(selectClass(sample))
                    .configurationParameter("rfr.testkit", "true")
                    .execute()).get(120, TimeUnit.SECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Cannot run the sample " + sample.getSimpleName(), e);
        } finally {
            thread.shutdownNow();
        }
    }

    private static Class<?> cause(EngineExecutionResults results) {
        return results.testEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getCause().getClass())
                .findFirst()
                .orElseThrow();
    }

    private static List<Object> containerFailure(EngineExecutionResults results) {
        Throwable failure = results.containerEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
                .findFirst()
                .orElseThrow();
        return List.of(failure.getClass(), failure.getMessage());
    }

    private static List<Object> failure(EngineExecutionResults results) {
        Throwable failure = results.testEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow())
                .findFirst()
                .orElseThrow();
        return List.of(failure.getClass(), failure.getMessage());
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

    /**
     * Sample test that asks for a guest session next to the page of the test.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({UserExtension.class, SessionExtension.class})
    static class GuestSample {

        static final List<Page> OPENED = new CopyOnWriteArrayList<>();

        @Test
        void body(Page page, GuestSession guest, TestReporter reporter) {
            OPENED.add(guest.page());
            reporter.publishEntry("separate", String.valueOf(guest.page().context() != page.context()));
            reporter.publishEntry("url", guest.page().url());
        }
    }

    /**
     * Sample test that asks for a user session next to the page of the test.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({UserExtension.class, SessionExtension.class})
    static class UserSample {

        static final List<Page> OPENED = new CopyOnWriteArrayList<>();

        @Test
        void body(Page page, @User UserSession session, TestReporter reporter) {
            OPENED.add(session.page());
            reporter.publishEntry("username", session.user().username());
            reporter.publishEntry("url", session.page().url());
            reporter.publishEntry("main", page.url());
        }
    }

    /**
     * Sample test whose user is removed before the session signs in, so the sign-in fails.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({ContextsProbe.class, UserExtension.class, SessionExtension.class, RemoveUser.class})
    static class FailingSignIn {

        @Test
        void body(@User UserSession session) {
        }
    }

    /**
     * Sample test with a plain user and two user sessions.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({ContextsProbe.class, UserExtension.class, SessionExtension.class})
    static class TwoSessions {

        @Test
        void body(@User TestUser plain, @User UserSession first, @User UserSession second, TestReporter reporter) {
            reporter.publishEntry("username", plain.username());
            reporter.publishEntry("username", first.user().username());
            reporter.publishEntry("username", second.user().username());
            reporter.publishEntry("url", first.page().url());
            reporter.publishEntry("url", second.page().url());
        }
    }

    /**
     * Reports how many contexts the browser holds after the session extension has closed its own; only the context of the test is left when nothing leaked.
     */
    static final class ContextsProbe implements AfterEachCallback {

        @Override
        public void afterEach(ExtensionContext context) {
            context.publishReportEntry("contexts.after", String.valueOf(BrowserExtension.getOrCreateBrowser(context).contexts().size()));
        }
    }

    /**
     * Removes the created user from both databases, so that signing in as it fails.
     */
    static final class RemoveUser implements BeforeEachCallback {

        @Override
        public void beforeEach(ExtensionContext context) {
            List<?> fixtures = context.getStore(ExtensionContext.Namespace.create(UserExtension.class))
                    .get(context.getUniqueId(), List.class);
            String username = ((UserFixture) fixtures.getFirst()).user().username();
            context.publishReportEntry("username", username);
            AuthUserRepository.deleteUser(username);
            ApiUserRepository.deleteUser(username);
        }
    }

    /**
     * Sample class that asks for a session in a class-level method.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({UserExtension.class, SessionExtension.class})
    static class SessionOnBeforeAll {

        @BeforeAll
        static void prepare(GuestSession guest) {
        }

        @Test
        void body() {
        }
    }

    /**
     * Sample test that asks for a user session but registers no user extension.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({ContextsProbe.class, SessionExtension.class})
    static class WithoutUserExtension {

        @Test
        void body(@User UserSession session) {
        }
    }

    /**
     * Sample test that asks for a user session without {@code @User}.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({UserExtension.class, SessionExtension.class})
    static class UserSessionWithoutUser {

        @Test
        void body(UserSession session) {
        }
    }

    /**
     * Sample test that asks for a guest session with {@code @User}, remembering the user that the user extension created.
     */
    @ProbeTarget
    @UiTest
    @ExtendWith({UserExtension.class, SessionExtension.class, UsernameProbe.class})
    static class GuestSessionWithUser {

        static final List<String> USERNAMES = new CopyOnWriteArrayList<>();

        @Test
        void body(@User GuestSession guest) {
        }
    }

    /**
     * Remembers the username of the user that was created for the sample test.
     */
    static final class UsernameProbe implements BeforeEachCallback {

        @Override
        public void beforeEach(ExtensionContext context) {
            List<?> fixtures = context.getStore(ExtensionContext.Namespace.create(UserExtension.class))
                    .get(context.getUniqueId(), List.class);
            GuestSessionWithUser.USERNAMES.add(((UserFixture) fixtures.getFirst()).user().username());
        }
    }
}
