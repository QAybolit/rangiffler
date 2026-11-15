package io.student.rangiffler.data;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks with fake fixtures that {@link Partners} closes every fixture and passes on the failure when one of them fails.
 */
@DisplayName("Partners closes every fixture it owns")
class PartnersTest {

    @Test
    @DisplayName("Fixtures before and after a failing one are still closed when it throws an Error")
    void shouldCloseRemainingFixturesWhenOneThrowsError() {
        List<String> closed = new ArrayList<>();
        Partners partners = new Partners(
                new Recording("origin", closed, () -> {
                }),
                List.of(
                        new Recording("first", closed, () -> {
                            throw new AssertionError("first failed");
                        }),
                        new Recording("second", closed, () -> {
                        })
                )
        );
        assertThrows(AssertionError.class, partners::close);

        assertEquals(
                List.of("second", "first", "origin"),
                closed,
                "Some fixtures were not closed after another one threw an Error"
        );
    }

    @Test
    @DisplayName("The Error from a fixture reaches the caller")
    void shouldPassOnErrorFromFixture() {
        Partners partners = new Partners(
                new Recording("origin", new ArrayList<>(), () -> {
                }),
                List.of(new Recording("only", new ArrayList<>(), () -> {
                    throw new AssertionError("only failed");
                }))
        );

        assertEquals(
                "only failed",
                assertThrows(AssertionError.class, partners::close).getMessage(),
                "The Error was swallowed or replaced on close"
        );
    }

    /**
     * Fake fixture that notes its closing in a shared list and then runs an action that may fail.
     */
    private static final class Recording implements UserFixture {

        private final String name;
        private final List<String> closed;
        private final Runnable after;

        private Recording(String name, List<String> closed, Runnable after) {
            this.name = name;
            this.closed = closed;
            this.after = after;
        }

        @Override
        public TestUser user() {
            throw new UnsupportedOperationException("Fake fixture has no user");
        }

        @Override
        public void close() {
            closed.add(name);
            after.run();
        }
    }
}
