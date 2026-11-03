package io.student.rangiffler.stand;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks with local servers that {@link HttpService} takes a status below 400 or, on request, any status for an answer, and that a silent server cannot hold it up.
 */
@DisplayName("HttpService is ready when the server answers")
class HttpServiceTest {

    @Test
    @DisplayName("Takes a successful status for an answer")
    void shouldAnswerOnSuccessStatus() throws IOException {
        assertEquals(true, answers(204, false), "A successful status was not taken for an answer");
    }

    @Test
    @DisplayName("Does not take an error status for an answer by default")
    void shouldNotAnswerOnErrorStatus() throws IOException {
        assertEquals(false, answers(500, false), "An error status was taken for an answer");
    }

    @Test
    @DisplayName("Takes an error status for an answer when any response counts")
    void shouldAnswerOnAnyStatusWhenAskedTo() throws IOException {
        assertEquals(true, answers(401, true), "An error status was not taken for an answer although any response counts");
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
                new HttpService("api", "start api", request(port), true).answers(Duration.ofSeconds(2)),
                "A closed port was taken for an answer"
        );
    }

    @Test
    @DisplayName("Gives up on a server that accepts the connection and never answers")
    void shouldGiveUpOnSilentServer() throws IOException {
        long started = System.nanoTime();
        boolean answered;
        try (ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress())) {
            answered = new HttpService("api", "start api", request(server.getLocalPort()), false).answers(Duration.ofMillis(300));
        }

        assertTrue(
                !answered && Duration.ofNanos(System.nanoTime() - started).compareTo(Duration.ofSeconds(5)) < 0,
                "A silent server held the request past its timeout"
        );
    }

    private static boolean answers(int status, boolean anyResponse) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
        try {
            return new HttpService("api", "start api", request(server.getAddress().getPort()), anyResponse).answers(Duration.ofSeconds(2));
        } finally {
            server.stop(0);
        }
    }

    private static HttpRequest request(int port) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/")).GET().build();
    }
}
