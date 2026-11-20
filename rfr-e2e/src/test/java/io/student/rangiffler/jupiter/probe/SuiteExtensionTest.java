package io.student.rangiffler.jupiter.probe;

import io.student.rangiffler.jupiter.extension.SuiteExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.engine.DiscoverySelector;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample tests through the JUnit engine to check, treating {@link SuiteExtension} as a black box, that a suite extension acts once per test run, survives failures of its own hooks and always closes what it opened.
 */
@DisplayName("SuiteExtension acts once per test run and always closes")
@Tag("extension")
class SuiteExtensionTest {

    private static final List<String> EVENTS = new CopyOnWriteArrayList<>();

    private static final String BEFORE_FAILURE = "Sample failure of beforeSuite";

    private static final String AFTER_FAILURE = "Sample failure of afterSuite";

    @Test
    @DisplayName("beforeSuite runs once for three test classes")
    void shouldCallBeforeSuiteOnceForManyClasses() {
        assertEquals(
                1,
                Collections.frequency(eventsOf(First.class, Second.class, Third.class), "before"),
                "beforeSuite was not called exactly once for three test classes"
        );
    }

    @Test
    @DisplayName("beforeSuite runs before the first test and afterSuite after the last one")
    void shouldWrapAllTestsOfTheRun() {
        List<String> events = eventsOf(First.class, Second.class, Third.class);

        assertEquals(
                List.of("before", "after", 5),
                List.of(events.getFirst(), events.getLast(), events.size()),
                "Suite hooks did not surround the tests of all three classes"
        );
    }

    @Test
    @DisplayName("afterSuite runs once for three test classes")
    void shouldCallAfterSuiteOnceForManyClasses() {
        assertEquals(
                1,
                Collections.frequency(eventsOf(First.class, Second.class, Third.class), "after"),
                "afterSuite was not called exactly once for three test classes"
        );
    }

    @Test
    @DisplayName("afterSuite runs after failed tests")
    void shouldCallAfterSuiteAfterFailedTests() {
        assertEquals(
                "after",
                eventsOf(First.class, Failing.class).getLast(),
                "afterSuite was skipped because a test failed"
        );
    }

    @Test
    @DisplayName("Nothing runs when no test class starts")
    void shouldDoNothingWhenNoClassRuns() {
        assertEquals(
                List.of(),
                eventsOf(Skipped.class),
                "Suite hooks ran although no test class started"
        );
    }

    @Test
    @DisplayName("Both hooks receive the root context")
    void shouldPassRootContextToBothHooks() {
        assertEquals(
                List.of("before.root", "test", "after.root"),
                eventsOf(RootContext.class),
                "A hook did not receive the root context"
        );
    }

    @Test
    @DisplayName("A class the extension does not apply to never starts it")
    void shouldNotStartExtensionForInapplicableClass() {
        assertEquals(
                List.of("unmarked"),
                eventsOf(Unmarked.class),
                "The extension started or closed for a class it does not apply to"
        );
    }

    @Test
    @DisplayName("The extension starts once on the first class it applies to")
    void shouldStartExtensionOnceOnApplicableClass() {
        List<String> events = eventsOf(Unmarked.class, MarkedFirst.class, MarkedSecond.class);

        assertEquals(
                List.of(1, 1, "tagged.after"),
                List.of(Collections.frequency(events, "tagged.before"), Collections.frequency(events, "tagged.after"), events.getLast()),
                "The extension did not start and close once for the classes it applies to"
        );
    }

    @Test
    @DisplayName("Each test run calls the hooks once on its own")
    void shouldStartEachRunFromScratch() {
        assertEquals(
                List.of(List.of("before", "first", "after"), List.of("before", "first", "after")),
                List.of(eventsOf(First.class), eventsOf(First.class)),
                "The second run did not call the hooks on its own"
        );
    }

    @Test
    @DisplayName("A nested test class does not call beforeSuite again")
    void shouldCallBeforeSuiteOnceWithNestedClass() {
        assertEquals(
                List.of("before", "outer", "nested", "after"),
                eventsOf(WithNested.class),
                "Nested class called the hooks again or its tests were lost"
        );
    }

    @Test
    @DisplayName("Parallel test classes call beforeSuite once")
    void shouldCallBeforeSuiteOnceInParallel() {
        run(true, First.class, Second.class, Third.class);

        assertEquals(
                1,
                Collections.frequency(EVENTS, "before"),
                "Parallel classes did not call beforeSuite exactly once"
        );
    }

    @Test
    @DisplayName("Different suite extensions act once each")
    void shouldTreatDifferentExtensionsSeparately() {
        List<String> events = eventsOf(First.class, OtherOnly.class);

        assertEquals(
                List.of(1, 1, 1, 1),
                List.of(
                        Collections.frequency(events, "before"),
                        Collections.frequency(events, "after"),
                        Collections.frequency(events, "other.before"),
                        Collections.frequency(events, "other.after")
                ),
                "Two suite extensions did not act once each"
        );
    }

    @Test
    @DisplayName("Two suite extensions start in registration order and close in reverse order")
    void shouldCloseExtensionsInReverseOrder() {
        assertEquals(
                List.of("before", "other.before", "test", "other.after", "after"),
                eventsOf(OrderedPair.class),
                "Two suite extensions did not start in order and close in reverse order"
        );
    }

    @Test
    @DisplayName("A suite extension after a failed one never starts and so never closes")
    void shouldNotStartExtensionAfterFailedOne() {
        assertEquals(
                List.of("before"),
                eventsOf(FailedThenOther.class),
                "The extension registered after the failed one started or closed"
        );
    }

    @Test
    @DisplayName("A suite extension before a failed one still closes")
    void shouldCloseExtensionBeforeFailedOne() {
        assertEquals(
                List.of("other.before", "before", "other.after"),
                eventsOf(OtherThenFailed.class),
                "The extension registered before the failed one did not close"
        );
    }

    @Test
    @DisplayName("A suite extension still closes when another one fails to close")
    void shouldCloseOtherExtensionWhenAfterSuiteFails() {
        assertEquals(
                List.of("other.before", "before", "test", "after", "other.after"),
                eventsOf(OtherThenFailingEnd.class),
                "The extension registered before the one failing to close was not closed"
        );
    }

    @Test
    @DisplayName("beforeSuite is not repeated for the next class after it failed")
    void shouldCallBeforeSuiteOnceAfterItFailed() {
        assertEquals(
                1,
                Collections.frequency(eventsOf(FailingStartFirst.class, FailingStartSecond.class), "before"),
                "beforeSuite was repeated after it failed"
        );
    }

    @Test
    @DisplayName("afterSuite is not called when beforeSuite failed")
    void shouldNotCallAfterSuiteWhenBeforeSuiteFailed() {
        assertEquals(
                0,
                Collections.frequency(eventsOf(FailingStartFirst.class, FailingStartSecond.class), "after"),
                "afterSuite was called although beforeSuite failed and took nothing"
        );
    }

    @Test
    @DisplayName("No test runs when beforeSuite failed")
    void shouldNotRunTestsWhenBeforeSuiteFailed() {
        assertEquals(
                0,
                Collections.frequency(eventsOf(FailingStartFirst.class, FailingStartSecond.class), "test"),
                "A test ran although beforeSuite failed"
        );
    }

    @Test
    @DisplayName("Every class and the run itself fail with the cause of beforeSuite")
    void shouldFailEveryClassAndRunWithCauseOfBeforeSuite() {
        assertEquals(
                List.of(BEFORE_FAILURE, BEFORE_FAILURE, BEFORE_FAILURE),
                failures(run(false, FailingStartFirst.class, FailingStartSecond.class)),
                "Not every class or the run failed with the cause of beforeSuite"
        );
    }

    @Test
    @DisplayName("afterSuite runs once even when it fails")
    void shouldCallAfterSuiteOnceWhenItFails() {
        assertEquals(
                1,
                Collections.frequency(eventsOf(FailingEndFirst.class, FailingEndSecond.class), "after"),
                "afterSuite was not called exactly once when it fails"
        );
    }

    @Test
    @DisplayName("A failure of afterSuite keeps the test results")
    void shouldKeepTestResultsWhenAfterSuiteFails() {
        assertEquals(
                2L,
                run(false, FailingEndFirst.class, FailingEndSecond.class).testEvents().succeeded().count(),
                "Failure of afterSuite changed the results of the tests"
        );
    }

    @Test
    @DisplayName("A failure of afterSuite is reported by the run")
    void shouldReportAfterSuiteFailure() {
        assertEquals(
                List.of(AFTER_FAILURE),
                failures(run(false, FailingEndFirst.class, FailingEndSecond.class)),
                "Failure of afterSuite was lost"
        );
    }

    private static List<String> eventsOf(Class<?>... samples) {
        run(false, samples);
        return List.copyOf(EVENTS);
    }

    private static List<String> failures(EngineExecutionResults results) {
        return results.containerEvents().failed().stream()
                .map(event -> rootCause(event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow()).getMessage())
                .toList();
    }

    private static Throwable rootCause(Throwable thrown) {
        return thrown.getCause() == null ? thrown : rootCause(thrown.getCause());
    }

    private static EngineExecutionResults run(boolean parallel, Class<?>... samples) {
        EVENTS.clear();
        return EngineTestKit.engine("junit-jupiter")
                .selectors(Arrays.stream(samples).map(sample -> selectClass(sample)).toArray(DiscoverySelector[]::new))
                .configurationParameter("rfr.testkit", "true")
                .configurationParameter("junit.jupiter.execution.parallel.enabled", String.valueOf(parallel))
                .configurationParameter("junit.jupiter.execution.parallel.mode.default", "concurrent")
                .configurationParameter("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
                .execute();
    }

    /**
     * Suite extension that only records its hooks.
     */
    static final class Recording implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add("before");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add("after");
        }
    }

    /**
     * Second suite extension, so that two implementations can run in one test run.
     */
    static final class Other implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add("other.before");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add("other.after");
        }
    }

    /**
     * Suite extension that applies only to classes tagged marked.
     */
    static final class OnlyMarked implements SuiteExtension {

        @Override
        public boolean appliesTo(ExtensionContext context) {
            return context.getTags().contains("marked");
        }

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add("tagged.before");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add("tagged.after");
        }
    }

    /**
     * Suite extension that records whether each hook received the root context.
     */
    static final class RootProbe implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add(context.getParent().isEmpty() ? "before.root" : "before.other");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add(context.getParent().isEmpty() ? "after.root" : "after.other");
        }
    }

    /**
     * Suite extension whose beforeSuite fails after it has recorded itself.
     */
    static final class FailingStart implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add("before");
            throw new IllegalStateException(BEFORE_FAILURE);
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add("after");
        }
    }

    /**
     * Suite extension whose afterSuite fails after it has recorded itself.
     */
    static final class FailingEnd implements SuiteExtension {

        @Override
        public void beforeSuite(ExtensionContext context) {
            EVENTS.add("before");
        }

        @Override
        public void afterSuite(ExtensionContext context) {
            EVENTS.add("after");
            throw new IllegalStateException(AFTER_FAILURE);
        }
    }

    /**
     * Sample class with one passing test.
     */
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class First {

        @Test
        void body() {
            EVENTS.add("first");
        }
    }

    /**
     * Second sample class with one passing test.
     */
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class Second {

        @Test
        void body() {
            EVENTS.add("second");
        }
    }

    /**
     * Third sample class with one passing test.
     */
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class Third {

        @Test
        void body() {
            EVENTS.add("third");
        }
    }

    /**
     * Sample class with a failing test.
     */
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class Failing {

        @Test
        void body() {
            EVENTS.add("failing");
            throw new IllegalStateException("Sample failure of a test");
        }
    }

    /**
     * Sample class that never starts, because it is disabled.
     */
    @Disabled
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class Skipped {

        @Test
        void body() {
            EVENTS.add("skipped");
        }
    }

    /**
     * Sample class with a nested class.
     */
    @ProbeTarget
    @ExtendWith(Recording.class)
    static class WithNested {

        @Test
        void body() {
            EVENTS.add("outer");
        }

        @Nested
        class Inner {

            @Test
            void body() {
                EVENTS.add("nested");
            }
        }
    }

    /**
     * Sample class the tagged-only suite extension does not apply to.
     */
    @ProbeTarget
    @ExtendWith(OnlyMarked.class)
    static class Unmarked {

        @Test
        void body() {
            EVENTS.add("unmarked");
        }
    }

    /**
     * First sample class the tagged-only suite extension applies to.
     */
    @Tag("marked")
    @ProbeTarget
    @ExtendWith(OnlyMarked.class)
    static class MarkedFirst {

        @Test
        void body() {
            EVENTS.add("marked.first");
        }
    }

    /**
     * Second sample class the tagged-only suite extension applies to.
     */
    @Tag("marked")
    @ProbeTarget
    @ExtendWith(OnlyMarked.class)
    static class MarkedSecond {

        @Test
        void body() {
            EVENTS.add("marked.second");
        }
    }

    /**
     * Sample class served by the suite extension that checks the context.
     */
    @ProbeTarget
    @ExtendWith(RootProbe.class)
    static class RootContext {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Sample class served by two suite extensions.
     */
    @ProbeTarget
    @ExtendWith({Recording.class, Other.class})
    static class OrderedPair {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Sample class whose first suite extension fails to start.
     */
    @ProbeTarget
    @ExtendWith({FailingStart.class, Other.class})
    static class FailedThenOther {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Sample class whose second suite extension fails to start.
     */
    @ProbeTarget
    @ExtendWith({Other.class, FailingStart.class})
    static class OtherThenFailed {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Sample class whose second suite extension fails to finish.
     */
    @ProbeTarget
    @ExtendWith({Other.class, FailingEnd.class})
    static class OtherThenFailingEnd {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Sample class served by the second suite extension only.
     */
    @ProbeTarget
    @ExtendWith(Other.class)
    static class OtherOnly {

        @Test
        void body() {
            EVENTS.add("other");
        }
    }

    /**
     * First sample class whose suite extension fails to start.
     */
    @ProbeTarget
    @ExtendWith(FailingStart.class)
    static class FailingStartFirst {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Second sample class whose suite extension fails to start.
     */
    @ProbeTarget
    @ExtendWith(FailingStart.class)
    static class FailingStartSecond {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * First sample class whose suite extension fails to finish.
     */
    @ProbeTarget
    @ExtendWith(FailingEnd.class)
    static class FailingEndFirst {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }

    /**
     * Second sample class whose suite extension fails to finish.
     */
    @ProbeTarget
    @ExtendWith(FailingEnd.class)
    static class FailingEndSecond {

        @Test
        void body() {
            EVENTS.add("test");
        }
    }
}
