package io.student.rangiffler.jupiter.extension;

import java.util.List;

/**
 * Value that {@code DemoExtension} passes to a test, naming the place it was requested from, the callbacks seen so far and the extension instance that served it.
 */
public record DemoValue(String origin, List<String> events, String extension) {

    /**
     * Prints which test instance and which extension instance serve the test, so that sharing between tests is visible.
     */
    public void report(String test, Object instance) {
        System.out.println("[demo] test " + test + ": test instance " + Integer.toHexString(System.identityHashCode(instance)) + ", extension " + extension);
    }
}
