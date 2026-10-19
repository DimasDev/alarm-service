package com.example.alarmservice.notification;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;

@Component
@Slf4j
public class NotificationDispatcher {

    private final ExecutorService executor;
    private final Semaphore capacity;

    private final Map<String, ElementQueue> queues = new ConcurrentHashMap<>();

    public NotificationDispatcher(NotificationProperties properties) {
        executor = Executors.newFixedThreadPool(properties.workers());
        capacity = new Semaphore(properties.queueCapacity());
    }

    public Reservation tryReserve(String elementId) {
        if (!capacity.tryAcquire()) {
            return null;
        }
        return new Reservation(elementId);
    }

    private void enqueue(String elementId, Runnable task) {
        ElementQueue elementQueue = queues.computeIfAbsent(elementId, id -> new ElementQueue());

        synchronized (elementQueue.lock) {
            elementQueue.tasks.add(task);

            if (!elementQueue.processing) {
                elementQueue.processing = true;
                try {
                    executor.execute(() -> processQueue(elementQueue));
                } catch (RejectedExecutionException exception) {
                    elementQueue.processing = false;
                    elementQueue.tasks.remove(task);
                    throw exception;
                }
            }
        }
    }

    private void processQueue(ElementQueue elementQueue) {
        while (true) {
            Runnable task;
            synchronized (elementQueue.lock) {
                task = elementQueue.tasks.poll();
                if (task == null) {
                    elementQueue.processing = false;
                    return;
                }
            }
            try {
                task.run();
            } catch (Exception exception) {
                log.error("Notification task failed", exception);
            } finally {
                capacity.release();
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }

    private static final class ElementQueue {
        private final Object lock = new Object();
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean processing;
    }

    public final class Reservation implements AutoCloseable {

        private final String elementId;
        private boolean active = true;

        private Reservation(String elementId) {
            this.elementId = elementId;
        }

        public void submit(Runnable task) {
            if (!active) {
                throw new IllegalStateException("Reservation already used");
            }
            enqueue(elementId, task);
            active = false;
        }

        @Override
        public void close() {
            if (active) {
                active = false;
                capacity.release();
            }
        }
    }
}
