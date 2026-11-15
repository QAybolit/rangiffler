package io.student.rangiffler.jupiter.stand;

import io.student.rangiffler.config.TestConfig;

import java.net.URI;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Waits until every service of the stand answers, and fails with the silent ones and the commands that start them when the time is up.
 */
public final class StandReadiness {

    private final List<StandService> services;
    private final Duration limit;
    private final Duration pause;
    private final Duration request;

    public StandReadiness(List<StandService> services, Duration limit, Duration pause, Duration request) {
        this.services = services;
        this.limit = limit;
        this.pause = pause;
        this.request = request;
    }

    /**
     * Creates the wait for MySQL, rfr-auth, rfr-api and the frontend at the addresses from {@code TestConfig}, with the signs of readiness of {@code scripts/wait-for-stand.sh}.
     */
    public StandReadiness() {
        this(stand(), TestConfig.standTimeout(), Duration.ofSeconds(2), Duration.ofSeconds(5));
    }

    /**
     * Polls the services that have not answered yet until all have answered or the limit is over; each request and each pause is cut to the time that is left, and no service is asked once the limit is over.
     */
    public void await() {
        long deadline = System.nanoTime() + limit.toNanos();
        List<StandService> silent = new ArrayList<>(services);
        Set<StandService> asked = new HashSet<>();
        while (true) {
            silent.removeIf(service -> {
                if (deadline - System.nanoTime() <= 0) {
                    return false;
                }
                asked.add(service);
                return service.answers(shorter(request, deadline));
            });
            if (silent.isEmpty()) {
                return;
            }
            if (deadline - System.nanoTime() <= 0) {
                throw new IllegalStateException(failure(silent, asked));
            }
            sleep(shorter(pause, deadline));
        }
    }

    private String failure(List<StandService> silent, Set<StandService> asked) {
        List<StandService> quiet = silent.stream().filter(asked::contains).toList();
        List<StandService> unasked = silent.stream().filter(service -> !asked.contains(service)).toList();
        Set<String> commands = new LinkedHashSet<>();
        quiet.forEach(service -> commands.add(service.command()));
        String message = "The stand is not ready after " + limit.toSeconds() + " s";
        if (!quiet.isEmpty()) {
            message += ", no answer from " + names(quiet);
        }
        if (!unasked.isEmpty()) {
            message += ", not asked before the limit ran out: " + names(unasked);
        }
        return quiet.isEmpty() ? message : message + "; start it with " + String.join(" and ", commands);
    }

    private static String names(List<StandService> services) {
        return String.join(", ", services.stream().map(StandService::name).toList());
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Waiting for the stand was interrupted", exception);
        }
    }

    private static Duration shorter(Duration duration, long deadline) {
        Duration left = Duration.ofNanos(Math.max(deadline - System.nanoTime(), 1_000_000L));
        return duration.compareTo(left) < 0 ? duration : left;
    }

    private static List<StandService> stand() {
        URI database = URI.create(TestConfig.dbUrl().substring("jdbc:".length()));
        return List.of(
                new TcpService("MySQL", "docker compose up -d --wait", database.getHost(), database.getPort() < 0 ? 3306 : database.getPort()),
                new HttpService(
                        "rfr-auth",
                        "./gradlew runAll",
                        HttpRequest.newBuilder(URI.create(TestConfig.authUrl() + "/.well-known/openid-configuration")).GET().build(),
                        false
                ),
                new HttpService(
                        "rfr-api",
                        "./gradlew runAll",
                        HttpRequest.newBuilder(URI.create(TestConfig.apiUrl() + "/graphql"))
                                .header("Content-Type", "application/json")
                                .POST(HttpRequest.BodyPublishers.ofString("{\"query\":\"{__typename}\"}"))
                                .build(),
                        true
                ),
                new HttpService(
                        "rfr-gql-client",
                        "cd rfr-gql-client && npm run dev",
                        HttpRequest.newBuilder(URI.create(TestConfig.frontUrl())).GET().build(),
                        false
                )
        );
    }
}
