package com.example.alarmservice.notification;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static com.example.alarmservice.exception.ErrorMessages.NOTIFICATION_ENDPOINT_RETURNED;

@Component
public class NotificationHttpClient {

    private final HttpClient httpClient;
    private final URI endpoint;

    public NotificationHttpClient(NotificationProperties properties) {
        this.endpoint = URI.create(properties.endpoint());
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.connectTimeoutMs())).build();
    }

    public void send(String eventId, String payload, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", eventId)
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(NOTIFICATION_ENDPOINT_RETURNED.formatted(response.statusCode()));
        }
    }
}
