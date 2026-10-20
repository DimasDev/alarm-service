package com.example.alarmservice.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.example.alarmservice.BaseIntegrationTest.ELEMENT_ID_1;
import static org.junit.jupiter.api.Assertions.*;

public class NotificationDispatcherTest {

    public static final String URL = "http://localhost:8081/external/notifications";

    @Test
    void shouldProcessTasksForSameElementSequentially() throws Exception {
        NotificationProperties properties = new NotificationProperties(true, 2, 10,
                URL, 2000, 5000);
        NotificationDispatcher dispatcher = new NotificationDispatcher(properties);

        try {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(3);

            for (int i = 1; i <= 3; i++) {
                int value = i;
                try (var reservation = dispatcher.tryReserve(ELEMENT_ID_1)) {
                    assertNotNull(reservation);

                    reservation.submit(() -> {
                        results.add(value);
                        latch.countDown();
                    });
                }
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(1, 2, 3), results);
        } finally {
            dispatcher.shutdown();
        }
    }

    @Test
    void shouldRejectTaskWhenQueueIsFull() throws Exception {

        NotificationProperties properties = new NotificationProperties(
                true, 1, 2,
                URL,
                2000, 5000
        );

        NotificationDispatcher dispatcher = new NotificationDispatcher(properties);

        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        try {
            try (var reservation = dispatcher.tryReserve(ELEMENT_ID_1)) {
                assertNotNull(reservation);

                reservation.submit(() -> {
                    started.countDown();

                    try {
                        release.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                });
            }

            assertTrue(started.await(5, TimeUnit.SECONDS));

            try (var reservation = dispatcher.tryReserve(ELEMENT_ID_1)) {
                assertNotNull(reservation);
                reservation.submit(() -> {
                });
            }

            assertNull(dispatcher.tryReserve(ELEMENT_ID_1));

        } finally {
            release.countDown();
            dispatcher.shutdown();
        }
    }
}
