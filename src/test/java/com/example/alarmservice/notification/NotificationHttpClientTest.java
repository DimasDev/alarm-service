package com.example.alarmservice.notification;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationHttpClientTest {

    public static final String EVT_HTTP_TEST = "EVT-HTTP-TEST";

    @Test
    void shouldSendNotificationSuccessfully() throws Exception {

        try (MockNotificationReceiver receiver = new MockNotificationReceiver()) {
            NotificationProperties properties = new NotificationProperties(true, 4, 100,
                    receiver.getUrl(), 2000, 5000);

            NotificationHttpClient client = new NotificationHttpClient(properties);
            String payload = """
                    {
                      "eventId": "EVT-HTTP-TEST",
                      "elementId": "BTS-100",
                      "status": "RAISED"
                    }
                    """;
            client.send(EVT_HTTP_TEST, payload, Duration.ofSeconds(5));

            assertEquals(1, receiver.getReceivedEvents().size());
            assertTrue(receiver.getReceivedEvents().getFirst().contains(EVT_HTTP_TEST));
        }
    }
}
