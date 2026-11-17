package org.springbozo.meditracker.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

/**
 * Per-IP limits on the unauthenticated auth endpoints (login, register, email links).
 * The client IP comes from X-Real-IP, which nginx sets; the backend has no published port.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Limit(int max, Duration window) {}

    private static final Map<String, Limit> LIMITS = Map.of(
            "/api/auth/login", new Limit(20, Duration.ofMinutes(15)),
            "/api/auth/register", new Limit(5, Duration.ofHours(1)),
            "/api/auth/resend-verification", new Limit(5, Duration.ofMinutes(15)),
            "/api/auth/forgot-password", new Limit(5, Duration.ofMinutes(15)),
            "/api/auth/verify-email", new Limit(20, Duration.ofMinutes(15)),
            "/api/auth/reset-password", new Limit(20, Duration.ofMinutes(15)),
            "/api/doses/quick", new Limit(60, Duration.ofMinutes(15))
    );

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Limit limit = "POST".equals(request.getMethod()) ? LIMITS.get(request.getRequestURI()) : null;
        if (limit != null && !rateLimiter.tryAcquire(request.getRequestURI() + "|" + clientIp(request), limit.max(), limit.window())) {
            tooManyRequests(response, limit.window());
            return;
        }
        chain.doFilter(request, response);
    }

    private static void tooManyRequests(HttpServletResponse response, Duration window) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(window.toSeconds()));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"Too many attempts. Please wait a few minutes and try again.\"}");
    }

    private static String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        return ip != null && !ip.isBlank() ? ip.trim() : request.getRemoteAddr();
    }
}
