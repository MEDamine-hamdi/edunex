package com.edunex.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Sliding one-minute windows per user (or IP when not signed in): all requests, and uploads separately. */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000;

    private final int generalLimit;
    private final int uploadLimit;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    public RateLimitFilter(int generalLimit, int uploadLimit) {
        this.generalLimit = generalLimit;
        this.uploadLimit = uploadLimit;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean signedIn = auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
        String who = signedIn ? "u:" + auth.getName() : "ip:" + request.getRemoteAddr();
        String type = request.getContentType() == null ? "" : request.getContentType().toLowerCase(Locale.ROOT);

        long wait = check(who + "|all", generalLimit);
        if (wait == 0 && type.startsWith("multipart/")) {
            wait = check(who + "|upload", uploadLimit);
        }
        if (wait > 0) {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(wait));
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Too many requests, please slow down\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    /** Records the hit and returns 0, or returns the seconds to wait when the limit is reached. */
    private long check(String key, int limit) {
        long now = System.currentTimeMillis();
        if (hits.size() > 20_000) {
            hits.entrySet().removeIf(e -> e.getValue().isEmpty() || e.getValue().peekLast() < now - WINDOW_MS);
        }
        Deque<Long> queue = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        synchronized (queue) {
            while (!queue.isEmpty() && queue.peekFirst() < now - WINDOW_MS) {
                queue.pollFirst();
            }
            if (queue.size() >= limit) {
                return Math.max(1, (queue.peekFirst() + WINDOW_MS - now + 999) / 1000);
            }
            queue.addLast(now);
            return 0;
        }
    }
}