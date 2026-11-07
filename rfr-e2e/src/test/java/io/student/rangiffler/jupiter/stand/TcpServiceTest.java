package io.student.rangiffler.jupiter.stand;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks that {@link TcpService} is ready exactly when its port accepts a connection.
 */
@DisplayName("TcpService is ready when its port accepts a connection")
class TcpServiceTest {

    @Test
    @DisplayName("Answers when something listens on the port")
    void shouldAnswerWhenPortIsOpen() throws IOException {
        try (ServerSocket server = new ServerSocket(0)) {
            assertEquals(
                    true,
                    new TcpService("db", "start db", "127.0.0.1", server.getLocalPort()).answers(Duration.ofSeconds(2)),
                    "A listening port was not taken for an answer"
            );
        }
    }

    @Test
    @DisplayName("Does not answer when nothing listens on the port")
    void shouldNotAnswerWhenPortIsClosed() throws IOException {
        int port;
        try (ServerSocket server = new ServerSocket(0)) {
            port = server.getLocalPort();
        }

        assertEquals(
                false,
                new TcpService("db", "start db", "127.0.0.1", port).answers(Duration.ofSeconds(2)),
                "A closed port was taken for an answer"
        );
    }
}
