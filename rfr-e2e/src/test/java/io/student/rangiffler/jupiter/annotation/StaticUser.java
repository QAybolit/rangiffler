package io.student.rangiffler.jupiter.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code TestUser} test parameter that {@link io.student.rangiffler.jupiter.extension.UsersQueueExtension} takes from the pool of prepared users for the test and gives back afterwards.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaticUser {
}
