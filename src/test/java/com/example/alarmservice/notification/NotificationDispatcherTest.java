package com.example.alarmservice.notification;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class NotificationDispatcherTest {

    @Test
    void shouldProcessTasksForSameElementSequentially() throws Exception {
        NotificationProperties properties = new NotificationProperties(true, 2, 10, 10, 1000);
        NotificationDispatcher dispatcher = new NotificationDispatcher(properties);

        try {
            List<Integer> results = Collections.synchronizedList(new ArrayList<>());
            CountDownLatch latch = new CountDownLatch(3);

            for (int i = 1; i <= 3; i++) {
                int value = i;
                assertTrue(dispatcher.dispatch("BTS-100", () -> {
                    results.add(value);
                    latch.countDown();
                }));
            }

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertEquals(List.of(1, 2, 3), results);
        } finally {
            dispatcher.shutdown();
        }
    }
}
