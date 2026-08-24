package com.example.trekking_app.service.security;

import com.example.trekking_app.model.RateLimitResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SlidingWindowRateLimiter {

    private final StringRedisTemplate redisTemplate;

    private static final String RATE_LIMIT_SCRIPT = """
        local currKey = KEYS[1]
        local prevKey = KEYS[2]
        local limit = tonumber(ARGV[1])
        local windowSeconds = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])

        local currCount = tonumber(redis.call('GET', currKey) or '0')
        local prevCount = tonumber(redis.call('GET', prevKey) or '0')

        local elapsed = now % windowSeconds
        local weight = 1 - (elapsed / windowSeconds)
        local estimate = (prevCount * weight) + currCount

        if estimate >= limit then
            local currWindow = math.floor(now / windowSeconds)
            local retryAfter = ((currWindow + 1) * windowSeconds) - now
            return { 0, math.max(0, math.floor(limit - estimate)), retryAfter }
        end

        local newCount = redis.call('INCR', currKey)
        if newCount == 1 then
            redis.call('EXPIRE', currKey, windowSeconds * 2)
        end

        local remaining = math.max(0, math.floor(limit - estimate - 1))
        return { 1, remaining, 0 }
        """;

    private final DefaultRedisScript<List> rateLimitScript =
            new DefaultRedisScript<>(RATE_LIMIT_SCRIPT, List.class);

    public RateLimitResult isAllowed(String identifier, int limit, long windowSeconds) {
        long now = Instant.now().getEpochSecond();
        long currentWindow = now / windowSeconds;
        long previousWindow = currentWindow - 1;

        String currentKey = "rate:%s:%d:%d".formatted(identifier, windowSeconds, currentWindow);
        String previousKey = "rate:%s:%d:%d".formatted(identifier, windowSeconds, previousWindow);

        List<Object> result = redisTemplate.execute(
                rateLimitScript,
                List.of(currentKey, previousKey),
                String.valueOf(limit),
                String.valueOf(windowSeconds),
                String.valueOf(now)
        );

        if (result == null || result.size() < 3) {
            throw new IllegalStateException("Rate limiter returned an invalid Redis response");
        }

        long allowed = ((Number) result.get(0)).longValue();
        long remaining = ((Number) result.get(1)).longValue();
        long retryAfter = ((Number) result.get(2)).longValue();

        return allowed == 1
                ? RateLimitResult.allowed(remaining)
                : RateLimitResult.blocked(remaining, retryAfter);
    }
}