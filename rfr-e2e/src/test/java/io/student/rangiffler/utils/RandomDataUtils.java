package io.student.rangiffler.utils;

import net.datafaker.Faker;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Random test data generators.
 */
public final class RandomDataUtils {

    private static final int MAX_PASSWORD_LENGTH = 12;
    private static final List<String> COUNTRIES =
            List.of("Japan", "France", "Germany", "Netherlands", "Italy", "Spain", "Canada", "Brazil");
    private static final Faker FAKER = new Faker();

    private RandomDataUtils() {
    }

    /**
     * Returns a unique username.
     */
    public static String randomUsername() {
        return "user" + UUID.randomUUID().toString().substring(0, 6);
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

    /**
     * Returns a random country name from a short list of countries available in the application, skipping the excluded ones.
     */
    public static String randomCountry(String... excluded) {
        List<String> candidates = COUNTRIES.stream()
                .filter(country -> !Arrays.asList(excluded).contains(country))
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No country left after excluding " + Arrays.toString(excluded));
        }
        return FAKER.options().nextElement(candidates);
    }
}
