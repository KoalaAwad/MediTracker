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
 * Per-IP limits: tight ones on the unauthenticated auth endpoints (login, register, email links),
 * a cap on the public label-image proxy, and a generous one on every API call.
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

    // Every API call, per IP: far above what one person clicking around does (Home polls once a
    // minute), low enough that a script hammering the site gets cut off.
    static final Limit ANY_API = new Limit(300, Duration.ofMinutes(1));
    // public and makes the server fetch from DailyMed, so it gets its own tighter cap
    static final Limit LABEL_IMAGES = new Limit(60, Duration.ofMinutes(1));

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        String ip = clientIp(request);

        Limit limit = "POST".equals(request.getMethod()) ? LIMITS.get(path) : null;
        if (limit != null && !rateLimiter.tryAcquire(path + "|" + ip, limit.max(), limit.window())) {
            tooManyRequests(response, limit.window());
            return;
        }
        if (path.startsWith("/api/medicine-images/")
                && !rateLimiter.tryAcquire("images|" + ip, LABEL_IMAGES.max(), LABEL_IMAGES.window())) {
            tooManyRequests(response, LABEL_IMAGES.window());
            return;
        }
        if (path.startsWith("/api/") && !rateLimiter.tryAcquire("api|" + ip, ANY_API.max(), ANY_API.window())) {
            tooManyRequests(response, ANY_API.window());
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
