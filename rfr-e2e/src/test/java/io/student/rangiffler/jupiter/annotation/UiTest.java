package io.student.rangiffler.jupiter.annotation;

import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.BrowserTraceExtension;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import io.student.rangiffler.jupiter.extension.SessionExtension;
import io.student.rangiffler.jupiter.extension.UserExtension;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class that needs the browser: it carries the tag {@code ui}, which the {@code e2e} task selects and the stand wait reacts to, it lets Playwright create the browser, context and page with {@link PlaywrightOptions}, {@link UserExtension} creates the {@code @User} users, {@link SessionExtension} opens the {@code UserSession} and {@code GuestSession} contexts, and {@link BrowserTraceExtension} keeps the traces of all contexts of a failed test.
 * The order of the extensions matters: {@code afterEach} runs in reverse order, so the sessions are closed before the users are removed.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("ui")
@UsePlaywright(PlaywrightOptions.class)
@ExtendWith({UserExtension.class, SessionExtension.class, BrowserTraceExtension.class})
public @interface UiTest {
}
