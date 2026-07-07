package com.example.trekking_app.config;

import com.example.trekking_app.model.RateLimitResult;
import com.example.trekking_app.service.security.SlidingWindowRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final SlidingWindowRateLimiter rateLimiter;
    private final IpExtractor              ipExtractor;

    private static final Map<String, int[]> PATH_LIMITS = Map.of(
            "/api/v1/auth",      new int[]{10,  900},   // 10 / 15min
            "/api/v1/admin/gpx", new int[]{10,  60},  // 10 / 1min
            "/api/v1/admin",     new int[]{500, 60},    // 500 / 1min
            "/api/v1/",          new int[]{200, 60}     // 200 / 1min default
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {

        String ip   = ipExtractor.extract(req);
        String path = req.getRequestURI();
        log.info("RateLimitFilter executing — path={} ip={}", path, ip);
        int[]  conf = resolveLimit(path);

        RateLimitResult result = rateLimiter.isAllowed(
                "filter:%s:%s".formatted(resolveIdentifier(ip), segmentOf(path)),
                conf[0], conf[1]
        );

        res.setHeader("X-RateLimit-Limit",     String.valueOf(conf[0]));
        res.setHeader("X-RateLimit-Remaining", String.valueOf(result.getRemaining()));

        if (!result.isAllowed()) {
            res.setHeader("Retry-After", String.valueOf(result.getRetryAfter()));
            res.setStatus(429);
            res.setContentType(MediaType.APPLICATION_JSON_VALUE);
            res.getWriter().write(
                    "{\"status\":429,\"message\":\"Rate limit exceeded. Retry after %ds.\"}".formatted(result.getRetryAfter())
            );
            return;
        }
        chain.doFilter(req, res);
    }

    private String resolveIdentifier(String ip) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken)
            return "ip:" + ip;
        return "user:" + auth.getName();
    }

    private int[] resolveLimit(String path) {
        return PATH_LIMITS.entrySet().stream()
                .filter(e -> path.startsWith(e.getKey()))
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(new int[]{200, 60});
    }

    private String segmentOf(String path) {
        String[] parts = path.split("/");
        return parts.length > 3 ? parts[3] : "api";
    }
}