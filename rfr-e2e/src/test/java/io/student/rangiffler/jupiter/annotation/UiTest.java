package io.student.rangiffler.jupiter.annotation;

import com.microsoft.playwright.junit.UsePlaywright;
import io.student.rangiffler.jupiter.extension.PlaywrightOptions;
import org.junit.jupiter.api.Tag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a test class that needs the browser: it carries the tag {@code ui}, which the {@code e2e} task selects and the stand wait reacts to, and it lets Playwright create the browser, context and page with {@link PlaywrightOptions}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("ui")
@UsePlaywright(PlaywrightOptions.class)
public @interface UiTest {
}
