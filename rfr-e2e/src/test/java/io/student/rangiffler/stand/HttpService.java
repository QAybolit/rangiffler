package io.student.rangiffler.stand;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Stand service that is ready when it answers an HTTP request, either with a status below 400 or, for services that reject the probe itself, with any status.
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
        HttpRequest bounded = HttpRequest.newBuilder(request, (header, value) -> true).timeout(timeout).build();
        try (HttpClient client = HttpClient.newBuilder().connectTimeout(timeout).build()) {
            int status = client.send(bounded, HttpResponse.BodyHandlers.discarding()).statusCode();
            return anyResponse || status < 400;
        } catch (IOException exception) {
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
