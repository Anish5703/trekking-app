package com.example.trekking_app.service.security;

import com.example.trekking_app.model.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SlidingWindowRateLimiter {

    private final StringRedisTemplate stringRedisTemplate;

    public RateLimitResult isAllowed(String identifier, int limit, long windowSeconds) {
        long now        = Instant.now().getEpochSecond();
        long currWindow = now / windowSeconds;
        long prevWindow = currWindow - 1;

        String currKey = "rate:%s:%d".formatted(identifier, currWindow);
        String prevKey = "rate:%s:%d".formatted(identifier, prevWindow);

        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<?>) conn -> {
            conn.stringCommands().get(currKey.getBytes());
            conn.stringCommands().get(prevKey.getBytes());
            return null;
        });

        long currCount = parseLong(results.get(0));
        long prevCount = parseLong(results.get(1));

        double elapsed  = now % windowSeconds;
        double weight   = 1.0 - (elapsed / windowSeconds);
        double estimate = (prevCount * weight) + currCount;
        long remaining  = Math.max(0, (long)(limit - estimate));

        if (estimate >= limit) {
            long retryAfter = ((currWindow + 1) * windowSeconds) - now;
            return RateLimitResult.blocked(remaining, retryAfter);
        }

        stringRedisTemplate.executePipelined((RedisCallback<?>) conn -> {
            byte[] key = currKey.getBytes();
            conn.stringCommands().incr(key);
            conn.keyCommands().expire(key, windowSeconds * 2);
            return null;
        });

        return RateLimitResult.allowed(remaining);
    }

    private long parseLong(Object val) {
        if (val == null) return 0L;
        return Long.parseLong(val.toString());
    }
}