package com.edunex.backend.service.ai;

import com.edunex.backend.exception.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sliding one-minute window per user, to protect the Groq quota. */
@Component
public class ChatRateLimiter {

    private static final long WINDOW_MS = 60_000;

    private final int limit;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public ChatRateLimiter(@Value("${app.ai.rate-limit-per-minute:15}") int limit) {
        this.limit = limit;
    }

    public void check(String key) {
        long now = System.currentTimeMillis();
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < now - WINDOW_MS) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                long retry = Math.max(1, (queue.peekFirst() + WINDOW_MS - now + 999) / 1000);
                throw new TooManyRequestsException("Too many messages. Try again in a moment.", retry);
            }
            queue.addLast(now);
        }
    }
}