package com.example.alarmservice.notification;

import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Component
public class NotificationDispatcher {

    private final List<ThreadPoolExecutor> workers = new ArrayList<>();

    public NotificationDispatcher(NotificationProperties properties) {
        for (int i = 0; i < properties.workers(); i++) {
            int workerId = i;
            ThreadPoolExecutor executor = new ThreadPoolExecutor(
                    1,
                    1,
                    0L,
                    TimeUnit.MILLISECONDS,
                    new ArrayBlockingQueue<>(properties.queueCapacity()),
                    task -> new Thread(task, "notification-worker-" + workerId),
                    new ThreadPoolExecutor.AbortPolicy()
            );
            workers.add(executor);
        }
    }

    public boolean dispatch(String elementId, Runnable task) {
        int partition = Math.floorMod(elementId.hashCode(), workers.size());
        try {
            workers.get(partition).execute(task);
            return true;

        } catch (RejectedExecutionException ex) {
            return false;
        }
    }

    @PreDestroy
    public void shutdown() {
        workers.forEach(ExecutorService::shutdown);
    }

}
