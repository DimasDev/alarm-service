package com.example.alarmservice.notification;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MockNotificationReceiver implements AutoCloseable {
    private final HttpServer server;
    private final List<String> receivedEvents = new CopyOnWriteArrayList<>();

    public MockNotificationReceiver() throws IOException {

        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/external/notifications", exchange -> {
            if (!"POST".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                exchange.close();
                return;
            }

            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            receivedEvents.add(body);

            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });

        server.start();
    }

    public String getUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/external/notifications";
    }

    public List<String> getReceivedEvents() {
        return List.copyOf(receivedEvents);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
