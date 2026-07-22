package com.expensetracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Simple in-memory, per-IP sliding-window rate limiter for the auth endpoints
 * (register/login/refresh), to blunt brute-force credential guessing.
 *
 * This is intentionally dependency-free and process-local: fine for a single
 * backend instance. If you scale to multiple instances behind a load balancer,
 * move this to a shared store (e.g. Redis) so limits apply across all of them.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.auth.capacity:10}")
    private int capacity;

    @Value("${app.rate-limit.auth.window-seconds:60}")
    private int windowSeconds;

    private final ConcurrentHashMap<String, Window> buckets = new ConcurrentHashMap<>();

    private static class Window {
        volatile long windowStartEpochSec;
        final AtomicInteger count = new AtomicInteger(0);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                     @NonNull HttpServletResponse response,
                                     @NonNull FilterChain filterChain) throws ServletException, IOException {

        String clientIp = clientIp(request);
        long now = Instant.now().getEpochSecond();

        Window window = buckets.computeIfAbsent(clientIp, k -> {
            Window w = new Window();
            w.windowStartEpochSec = now;
            return w;
        });

        synchronized (window) {
            if (now - window.windowStartEpochSec >= windowSeconds) {
                window.windowStartEpochSec = now;
                window.count.set(0);
            }
            int current = window.count.incrementAndGet();
            if (current > capacity) {
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"message\":\"Too many requests. Please wait a moment and try again.\"}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
