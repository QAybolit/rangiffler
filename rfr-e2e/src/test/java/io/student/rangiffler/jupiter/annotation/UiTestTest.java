package io.student.rangiffler.jupiter.annotation;

import io.student.rangiffler.jupiter.extension.BrowserTraceExtension;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Reads the extensions that {@code @UiTest} brings, because their order is the order in which JUnit registers them and, reversed, the order of their {@code afterEach} callbacks.
 */
@DisplayName("@UiTest brings the extensions of a UI test")
class UiTestTest {

    @Test
    @DisplayName("Users are created first, then sessions are opened, then the trace is started, so they are released in the reverse order")
    void shouldRegisterUserSessionAndTraceExtensionsInThisOrder() {
        assertEquals(
                List.of(UserExtension.class, SessionExtension.class, BrowserTraceExtension.class),
                List.of(UiTest.class.getAnnotation(ExtendWith.class).value()),
                "UiTest does not register the user, session and trace extensions in this order, so a user can be removed while its browser is still open"
        );
    }
}
