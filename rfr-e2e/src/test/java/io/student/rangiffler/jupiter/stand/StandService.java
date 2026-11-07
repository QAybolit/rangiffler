package io.student.rangiffler.jupiter.stand;

import java.time.Duration;

/**
 * One part of the local stand that tests need, with the command that starts it.
 */
public interface StandService {

    String name();

    String command();

    /**
     * Tells whether the service answered within the timeout; any failure to answer is false, never an exception.
     */
    boolean answers(Duration timeout);
}
