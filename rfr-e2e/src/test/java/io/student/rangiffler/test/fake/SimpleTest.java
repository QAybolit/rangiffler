package io.student.rangiffler.test.fake;

import io.qameta.allure.Allure;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SimpleTest {

    @Test
    void simpleTest() {
        Allure.step("Simple test", () -> {
            assertEquals(2, 1 + 1);
        });
    }
}
