package com.chess.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    // ip/user id - [tokens, lastRefillNanos]
    private final Map<String, long[]> buckets = new ConcurrentHashMap<>();

    // clean up every 10 mins
    private volatile long lastCleanup = System.nanoTime();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.equals("/api/health") || path.startsWith("/ws")) {
            chain.doFilter(request, response);
            return;
        }

        String key = resolveKey(request);

        if (!tryConsume(key)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Try again shortly.\"}");
            return;
        }

        if (System.nanoTime() - lastCleanup > TimeUnit.MINUTES.toNanos(10)) {
            lastCleanup = System.nanoTime();
            long cutoff = System.nanoTime() - TimeUnit.MINUTES.toNanos(5);
            buckets.entrySet().removeIf(e -> e.getValue()[1] < cutoff);
        }

        chain.doFilter(request, response);
    }

    private boolean tryConsume(String key) {
        long maxTokens = requestsPerMinute;
        long refillRateNanos = TimeUnit.MINUTES.toNanos(1) / requestsPerMinute;
        long now = System.nanoTime();

        long[] bucket = buckets.computeIfAbsent(key, k -> new long[]{maxTokens, now});

        synchronized (bucket) {
            // refill tokens based on elapsed time
            long elapsed = now - bucket[1];
            long newTokens = elapsed / refillRateNanos;
            if (newTokens > 0) {
                bucket[0] = Math.min(maxTokens, bucket[0] + newTokens);
                bucket[1] = now;
            }

            if (bucket[0] <= 0) {
                return false;
            }

            bucket[0]--;
            return true;
        }
    }

    private String resolveKey(HttpServletRequest request) {
        // try to use authenticated user ID first, fall back to IP
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return "user:" + auth.substring(7, Math.min(auth.length(), 47)); // prefix of token
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        return "ip:" + (forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr());
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
