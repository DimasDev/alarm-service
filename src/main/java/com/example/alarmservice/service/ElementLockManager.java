package com.example.alarmservice.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

@Component
public class ElementLockManager {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public <T> T execute(String elementId, Supplier<T> action) {
        ReentrantLock lock = locks.computeIfAbsent(elementId, id -> new ReentrantLock());

        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
