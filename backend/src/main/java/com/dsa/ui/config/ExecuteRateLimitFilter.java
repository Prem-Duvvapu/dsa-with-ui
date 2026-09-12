package com.dsa.ui.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounds how often one client may run an algorithm.
 *
 * <p>{@code InputValidator} bounds a SINGLE request very carefully - per-field size caps and
 * a 5000-step budget, both mandatory. Nothing bounded MANY requests. {@code /execute} runs
 * real algorithms on caller-supplied input, so a loop over an expensive problem was a free
 * way to spend the server's CPU, and the per-request discipline did nothing to stop it.
 *
 * <p>Only {@code /execute} is guarded. The catalogue is a cached, compressed projection and
 * costs almost nothing to serve; rate-limiting it would only punish a real user loading the
 * page. Charging for the expensive call and not the cheap one is the whole point.
 *
 * <p>Written by hand rather than with Bucket4j or Resilience4j because this application has
 * exactly two dependencies and that is deliberate. A token bucket is twenty lines.
 *
 * <p><strong>The client map is bounded.</strong> An unbounded map keyed by client address is
 * itself a memory exhaustion vector - the attacker rotates addresses and the defence becomes
 * the outage. Past {@code MAX_TRACKED_CLIENTS} the map is cleared wholesale, which briefly
 * forgives everyone and is strictly better than running out of heap. That trade is the
 * reason the cap is high enough that ordinary traffic never reaches it.
 */
@Component
public class ExecuteRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final int capacity;
    private final long refillIntervalNanos;
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public ExecuteRateLimitFilter(
            @Value("${dsa.rate-limit.executes-per-minute:60}") int executesPerMinute) {
        this.capacity = Math.max(1, executesPerMinute);
        // One token every (60s / rate). A burst up to `capacity` is allowed and then the
        // client is metered, which matches how someone actually uses this: a handful of
        // runs while exploring a problem, not sixty evenly spaced ones.
        this.refillIntervalNanos = 60_000_000_000L / this.capacity;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().endsWith("/execute");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String client = clientKey(request);

        if (buckets.size() > MAX_TRACKED_CLIENTS) {
            buckets.clear();
        }

        Bucket bucket = buckets.computeIfAbsent(client, k -> new Bucket(capacity));
        if (!bucket.tryConsume(refillIntervalNanos, capacity)) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", "60");
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"rate_limited\",\"message\":\"Too many runs. "
                  + "Each client may execute " + capacity + " traces per minute.\"}");
            return;
        }

        chain.doFilter(request, response);
    }

    /**
     * X-Forwarded-For first, because this runs behind a PaaS proxy in production and every
     * request would otherwise share the proxy's address - one bucket for the whole internet,
     * which is worse than no limit at all. Only the first hop is read; the rest of the chain
     * is client-supplied and cannot be trusted.
     */
    private static String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        String remote = request.getRemoteAddr();
        return remote == null ? "unknown" : remote;
    }

    /** A token bucket holding nanosecond-resolution credit, refilled lazily on read. */
    private static final class Bucket {
        private final AtomicLong tokens;
        private final AtomicLong lastRefillNanos = new AtomicLong(System.nanoTime());

        Bucket(int capacity) {
            this.tokens = new AtomicLong(capacity);
        }

        synchronized boolean tryConsume(long refillIntervalNanos, int capacity) {
            long now = System.nanoTime();
            long elapsed = now - lastRefillNanos.get();
            long earned = elapsed / refillIntervalNanos;
            if (earned > 0) {
                tokens.set(Math.min(capacity, tokens.get() + earned));
                lastRefillNanos.addAndGet(earned * refillIntervalNanos);
            }
            if (tokens.get() <= 0) {
                return false;
            }
            tokens.decrementAndGet();
            return true;
        }
    }
}
