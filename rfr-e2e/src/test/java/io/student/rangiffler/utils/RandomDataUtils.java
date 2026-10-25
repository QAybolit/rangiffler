package io.student.rangiffler.utils;

import net.datafaker.Faker;

import java.util.UUID;

/**
 * Random test data generators.
 */
public final class RandomDataUtils {

    private static final String USERNAME_PREFIX = "rfr_";
    private static final int MAX_PASSWORD_LENGTH = 12;
    private static final Faker FAKER = new Faker();

    private RandomDataUtils() {
    }

    /**
     * Returns a unique username.
     */
    public static String randomUsername() {
        return USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 6);
    }

    /**
     * Returns a random password within the register form's 12 character limit.
     */
    public static String randomPassword() {
        return ("Pw" + UUID.randomUUID().toString().replace("-", "")).substring(0, MAX_PASSWORD_LENGTH);
    }

    public static String randomFirstName() {
        return FAKER.name().firstName();
    }

    public static String randomSurname() {
        return FAKER.name().lastName();
    }
}
