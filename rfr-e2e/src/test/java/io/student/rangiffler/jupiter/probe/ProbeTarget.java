package io.student.rangiffler.jupiter.probe;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a sample class that a probe runs through EngineTestKit; the class is skipped in every other run, so it never counts as a real test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Tag("probe")
@ExtendWith(UserExtensionTest.TestkitOnly.class)
@interface ProbeTarget {
}
