package com.example.alarmservice;

import com.example.alarmservice.notification.NotificationHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "notification.enabled=true",
        "notification.workers=1",
        "notification.queue-capacity=1"
})
@AutoConfigureMockMvc
public class AlarmControllerQueueFullIntegrationTest extends BaseIntegrationTest {

    @MockitoBean
    private NotificationHttpClient httpClient;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReturn503AndNotPersistAlarmWhenNotificationQueueIsFull()
            throws Exception {

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        doAnswer(invocation -> {
            started.countDown();

            if (!release.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out");
            }

            return null;
        }).when(httpClient).send(anyString(), anyString(), any());

        String firstRequest = """
                {
                    "eventId": "QUEUE-1",
                    "elementId": "BTS-100",
                    "alarmCode": "CELL_DOWN",
                    "severity": "CRITICAL",
                    "status": "RAISED",
                    "timestamp": 1791800000000,
                    "description": "Test alarm"
                }
                """;

        String secondRequest = firstRequest.replace("QUEUE-1", "QUEUE-2");

        try {
            mockMvc.perform(post(API_ALARMS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(firstRequest))
                    .andExpect(status().isCreated());

            assertTrue(started.await(5, TimeUnit.SECONDS));

            mockMvc.perform(post(API_ALARMS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(secondRequest))
                    .andExpect(status().isServiceUnavailable());

            assertEquals(1, historyRepository.count());
            assertEquals(1, activeRepository.count());
            assertFalse(historyRepository.existsByEventId("QUEUE-2"));

        } finally {
            release.countDown();
        }
    }
}
