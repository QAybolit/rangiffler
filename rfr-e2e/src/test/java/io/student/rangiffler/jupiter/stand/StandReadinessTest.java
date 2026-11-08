package io.student.rangiffler.jupiter.stand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks with fake services that {@link StandReadiness} waits for every service, never waits past its limit and names only the silent services.
 */
@DisplayName("StandReadiness waits for the stand within its limit")
class StandReadinessTest {

    @Test
    @DisplayName("Returns at once when every service answers")
    void shouldReturnWhenEveryServiceAnswers() {
        AtomicInteger calls = new AtomicInteger();
        new StandReadiness(
                List.of(new Fake("a", "start a", 1, calls), new Fake("b", "start b", 1, calls)),
                Duration.ofSeconds(5), Duration.ofMillis(10), Duration.ofMillis(100)
        ).await();

        assertEquals(2, calls.get(), "A service that answered was asked again or a service was skipped");
    }

    @Test
    @DisplayName("Waits for a service that answers only on the fourth try")
    void shouldWaitForServiceThatComesUpLate() {
        AtomicInteger calls = new AtomicInteger();
        new StandReadiness(
                List.of(new Fake("late", "start late", 4, calls)),
                Duration.ofSeconds(5), Duration.ofMillis(10), Duration.ofMillis(100)
        ).await();

        assertEquals(4, calls.get(), "The wait did not poll until the service answered");
    }

    @Test
    @DisplayName("Does not ask a service again after it answered")
    void shouldNotAskAnsweredServiceAgain() {
        AtomicInteger early = new AtomicInteger();
        new StandReadiness(
                List.of(new Fake("early", "start early", 1, early), new Fake("late", "start late", 4, new AtomicInteger())),
                Duration.ofSeconds(5), Duration.ofMillis(10), Duration.ofMillis(100)
        ).await();

        assertEquals(1, early.get(), "A service that had answered was asked again");
    }

    @Test
    @DisplayName("Names only the silent service and its command when time is up")
    void shouldNameOnlySilentServices() {
        String message = assertThrows(
                IllegalStateException.class,
                () -> new StandReadiness(
                        List.of(new Fake("alive", "start alive", 1, new AtomicInteger()), new Fake("silent", "start silent", Integer.MAX_VALUE, new AtomicInteger())),
                        Duration.ofMillis(300), Duration.ofMillis(20), Duration.ofMillis(100)
                ).await(),
                "The wait did not fail when a service stayed silent"
        ).getMessage();

        assertEquals(
                List.of(true, false, true, false),
                List.of(message.contains("silent"), message.contains("alive"), message.contains("start silent"), message.contains("start alive")),
                "The message did not name exactly the silent service and its command"
        );
    }

    @Test
    @DisplayName("Lists a command shared by two services once")
    void shouldListSharedCommandOnce() {
        String message = assertThrows(
                IllegalStateException.class,
                () -> new StandReadiness(
                        List.of(new Fake("one", "start both", Integer.MAX_VALUE, new AtomicInteger()), new Fake("two", "start both", Integer.MAX_VALUE, new AtomicInteger())),
                        Duration.ofMillis(200), Duration.ofMillis(20), Duration.ofMillis(100)
                ).await(),
                "The wait did not fail when services stayed silent"
        ).getMessage();

        assertEquals(1, message.split("start both", -1).length - 1, "A shared command was listed more than once");
    }

    @Test
    @DisplayName("Stops soon after the limit even when a request would take longer")
    void shouldStopWithinLimit() {
        long started = System.nanoTime();
        assertThrows(
                IllegalStateException.class,
                () -> new StandReadiness(
                        List.of(new Slow("slow", "start slow", Duration.ofSeconds(1))),
                        Duration.ofMillis(300), Duration.ofMillis(20), Duration.ofSeconds(60)
                ).await(),
                "The wait did not fail when a service answered too slowly"
        );

        assertTrue(Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(3)) < 0, "The wait went on long after its limit");
    }

    private record Fake(String name, String command, int answersOn, AtomicInteger calls) implements StandService {

        @Override
        public boolean answers(Duration timeout) {
            return calls.incrementAndGet() >= answersOn;
        }
    }

    private record Slow(String name, String command, Duration delay) implements StandService {

        @Override
        public boolean answers(Duration timeout) {
            try {
                Thread.sleep(timeout.compareTo(delay) < 0 ? timeout : delay);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return false;
        }
    }
}
