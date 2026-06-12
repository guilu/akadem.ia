package com.akdemya.adapter.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

public class RateLimitFilter extends OncePerRequestFilter {

    // Hard cap on tracked IPs so an attacker rotating spoofed addresses
    // cannot grow the bucket maps without bound.
    private static final int MAX_TRACKED_IPS = 10_000;

    private final int loginRequestsPerMinute;
    private final int registerRequestsPerMinute;

    private final ConcurrentHashMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    public RateLimitFilter(int loginRequestsPerMinute, int registerRequestsPerMinute) {
        this.loginRequestsPerMinute = loginRequestsPerMinute;
        this.registerRequestsPerMinute = registerRequestsPerMinute;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = resolveClientIp(request);

        if (path.equals("/api/auth/login")) {
            if (!tryConsume(loginBuckets, ip, loginRequestsPerMinute, response)) return;
        } else if (path.equals("/api/auth/register")) {
            if (!tryConsume(registerBuckets, ip, registerRequestsPerMinute, response)) return;
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(ConcurrentHashMap<String, Bucket> buckets, String ip, int limit,
                                HttpServletResponse response) throws IOException {
        if (buckets.size() >= MAX_TRACKED_IPS) {
            // Crude but bounded: dropping all buckets briefly resets limits,
            // which is preferable to unbounded memory growth.
            buckets.clear();
        }
        Bucket bucket = buckets.computeIfAbsent(ip, k -> buildBucket(limit));
        if (bucket.tryConsume(1)) {
            return true;
        }
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"too_many_requests\"}");
        return false;
    }

    private Bucket buildBucket(int requestsPerMinute) {
        return Bucket.builder()
            .addLimit(Bandwidth.builder()
                .capacity(requestsPerMinute)
                .refillGreedy(requestsPerMinute, Duration.ofMinutes(1))
                .build())
            .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // The rightmost entry is the one appended by our own reverse
            // proxy (nginx with proxy_add_x_forwarded_for) and is the only
            // value the client cannot forge. Leftmost entries are
            // client-controlled and would let an attacker bypass the limit
            // by sending a different fake IP on every request.
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
