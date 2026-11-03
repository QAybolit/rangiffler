package io.student.rangiffler.jupiter.probe;

import com.sun.net.httpserver.HttpServer;
import io.student.rangiffler.jupiter.extension.StandReadinessExtension;
import io.student.rangiffler.stand.HttpService;
import io.student.rangiffler.stand.StandReadiness;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Runs sample UI classes through the JUnit engine against a fake HTTP stand on an ephemeral port, to check that {@link StandReadinessExtension} waits for a late service, fails with its name when it stays silent and sends no request when the check is off.
 */
@DisplayName("StandReadinessExtension waits for the stand before UI tests")
@Tag("extension")
class StandReadinessExtensionTest {

    @Test
    @DisplayName("A service that starts answering after 2 s lets the tests run")
    void shouldRunTestsWhenServiceAnswersLate() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = server(requests, Duration.ofSeconds(2));
        try {
            assertEquals(
                    List.of(1L, true),
                    List.of(run(server, 10, AnswersLate.class).testEvents().succeeded().count(), requests.get() > 1),
                    "The tests did not run after the service started answering or it was not polled"
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("A service that does not answer in time stops the tests and is named in the error")
    void shouldFailWithServiceNameWhenServiceStaysSilent() throws IOException {
        HttpServer server = server(new AtomicInteger(), Duration.ofDays(1));
        try {
            EngineExecutionResults results = run(server, 1, Silent.class);

            assertEquals(
                    List.of(0L, true),
                    List.of(results.testEvents().started().count(), failures(results).stream().anyMatch(message -> message.contains("fake-api"))),
                    "A test ran or the error did not name the silent service"
            );
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("Switching the check off sends no request to the service")
    void shouldSendNoRequestWhenCheckIsOff() throws IOException {
        AtomicInteger requests = new AtomicInteger();
        HttpServer server = server(requests, Duration.ofDays(1));
        try {
            EngineExecutionResults results = run(server, 1, Switched.class);

            assertEquals(
                    List.of(0, 1L),
                    List.of(requests.get(), results.testEvents().succeeded().count()),
                    "The service got a request or the test did not run with the check off"
            );
        } finally {
            server.stop(0);
        }
    }

    private static HttpServer server(AtomicInteger requests, Duration delay) throws IOException {
        long readyAt = System.nanoTime() + delay.toNanos();
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(System.nanoTime() >= readyAt ? 200 : 503, -1);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static EngineExecutionResults run(HttpServer server, int limit, Class<?> sample) {
        return EngineTestKit.engine("junit-jupiter")
                .selectors(selectClass(sample))
                .configurationParameter("rfr.testkit", "true")
                .configurationParameter("junit.jupiter.extensions.autodetection.enabled", "false")
                .configurationParameter("probe.port", String.valueOf(server.getAddress().getPort()))
                .configurationParameter("probe.limit", String.valueOf(limit))
                .execute();
    }

    private static List<String> failures(EngineExecutionResults results) {
        return results.containerEvents().failed().stream()
                .map(event -> event.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage())
                .toList();
    }

    private static StandReadiness stand(ExtensionContext context) {
        return new StandReadiness(
                List.of(new HttpService(
                        "fake-api",
                        "start fake-api",
                        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + parameter(context, "probe.port") + "/")).GET().build(),
                        false
                )),
                Duration.ofSeconds(Long.parseLong(parameter(context, "probe.limit"))),
                Duration.ofMillis(200),
                Duration.ofSeconds(1)
        );
    }

    private static String parameter(ExtensionContext context, String name) {
        return context.getConfigurationParameter(name).orElseThrow();
    }

    /**
     * Sample UI class whose stand answers only after a delay.
     */
    @ProbeTarget
    @Tag("ui")
    static class AnswersLate {

        @RegisterExtension
        static final StandReadinessExtension READINESS = new StandReadinessExtension(context -> true, StandReadinessExtensionTest::stand);

        @Test
        void body() {
        }
    }

    /**
     * Sample UI class whose stand never answers.
     */
    @ProbeTarget
    @Tag("ui")
    static class Silent {

        @RegisterExtension
        static final StandReadinessExtension READINESS = new StandReadinessExtension(context -> true, StandReadinessExtensionTest::stand);

        @Test
        void body() {
        }
    }

    /**
     * Sample UI class whose check is switched off.
     */
    @ProbeTarget
    @Tag("ui")
    static class Switched {

        @RegisterExtension
        static final StandReadinessExtension READINESS = new StandReadinessExtension(context -> false, StandReadinessExtensionTest::stand);

        @Test
        void body() {
        }
    }
}
