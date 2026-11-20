package io.student.rangiffler.test.demo;

import io.student.rangiffler.jupiter.extension.DemoExtension;
import io.student.rangiffler.jupiter.extension.DemoValue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DemoExtension.class)
@DisplayName("@ExtendWith on a top-level class")
public class DemoClassLevelTest {

    private static final List<String> SCOPE = List.of(
            "beforeAll", "preConstructTestInstance", "postProcessTestInstance", "beforeEach", "beforeTestExecution"
    );

    @Test
    @DisplayName("the first test is served by the class extension")
    void first(DemoValue value) {
        value.report("first", this);
        assertTrue(value.events().containsAll(SCOPE), "Callbacks of the class scope were not called for the first test");
    }

    @Test
    @DisplayName("the second test is served by the same extension instance")
    void second(DemoValue value) {
        value.report("second", this);
        assertTrue(value.events().containsAll(SCOPE), "Callbacks of the class scope were not called for the second test");
    }
}
