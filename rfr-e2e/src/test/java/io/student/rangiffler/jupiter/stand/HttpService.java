package io.student.rangiffler.jupiter.stand;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Stand service that is ready when it answers an HTTP request, either with a status below 400 or, for services that reject the probe itself, with any status, on any address of {@code localhost} (a dev server may listen on IPv6 only, while Java resolves {@code localhost} to IPv4 first); every other host name is requested as it is, so the {@code Host} header and the TLS certificate stay valid.
 */
public final class HttpService implements StandService {

    private final String name;
    private final String command;
    private final HttpRequest request;
    private final boolean anyResponse;

    public HttpService(String name, String command, HttpRequest request, boolean anyResponse) {
        this.name = name;
        this.command = command;
        this.request = request;
        this.anyResponse = anyResponse;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public String command() {
        return command;
    }

    @Override
    public boolean answers(Duration timeout) {
        if (!"localhost".equalsIgnoreCase(request.uri().getHost())) {
            return answers(request, timeout);
        }
        long deadline = System.nanoTime() + timeout.toNanos();
        for (InetAddress address : addresses()) {
            Duration left = Duration.ofNanos(deadline - System.nanoTime());
            if (left.isPositive() && answers(at(address), left)) {
                return true;
            }
        }
        return false;
    }

    private InetAddress[] addresses() {
        try {
            return InetAddress.getAllByName(request.uri().getHost());
        } catch (UnknownHostException exception) {
            return new InetAddress[0];
        }
    }

    private boolean answers(HttpRequest candidate, Duration timeout) {
        HttpRequest bounded = HttpRequest.newBuilder(candidate, (header, value) -> true).timeout(timeout).build();
        try (HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(timeout).build()) {
            int status = client.send(bounded, HttpResponse.BodyHandlers.discarding()).statusCode();
            return anyResponse || status < 400;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private HttpRequest at(InetAddress address) {
        URI uri = request.uri();
        try {
            return HttpRequest.newBuilder(request, (header, value) -> true)
                    .uri(
                            new URI(
                                    uri.getScheme(),
                                    uri.getUserInfo(),
                                    address.getHostAddress().split("%")[0],
                                    uri.getPort(),
                                    uri.getPath(),
                                    uri.getQuery(),
                                    uri.getFragment()
                            )
                    )
                    .build();
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Cannot address " + uri + " by " + address, exception);
        }
    }
}
