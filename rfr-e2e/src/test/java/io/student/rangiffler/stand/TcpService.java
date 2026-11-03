package io.student.rangiffler.stand;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.Duration;

/**
 * Stand service that is ready as soon as its port accepts a connection.
 */
public final class TcpService implements StandService {

    private final String name;
    private final String command;
    private final InetSocketAddress address;

    public TcpService(String name, String command, String host, int port) {
        this.name = name;
        this.command = command;
        this.address = new InetSocketAddress(host, port);
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
        try (Socket socket = new Socket()) {
            socket.connect(address, (int) timeout.toMillis());
            return true;
        } catch (IOException exception) {
            return false;
        }
    }
}
