package com.example.trekking_app.service.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class BruteForceProtectionService {

    private final StringRedisTemplate stringRedisTemplate;

    private static final int    MAX_ATTEMPTS    = 5;
    private static final long   LOCKOUT_SECONDS = 900L;
    private static final String ATTEMPT_PRE     = "bf:attempt:";
    private static final String LOCKOUT_PRE     = "bf:lockout:";

    public void recordFailedAttempt(String ip) {
        Long count = stringRedisTemplate.opsForValue().increment(ATTEMPT_PRE + ip);
        if (count != null && count == 1)
            stringRedisTemplate.expire(ATTEMPT_PRE + ip, Duration.ofSeconds(LOCKOUT_SECONDS));
        if (count != null && count >= MAX_ATTEMPTS) {
            stringRedisTemplate.opsForValue().set(LOCKOUT_PRE + ip, "locked", Duration.ofSeconds(LOCKOUT_SECONDS));
            log.warn("IP locked out: {}", ip);
        }
    }

    public void resetAttempts(String ip) {
        stringRedisTemplate.delete(List.of(ATTEMPT_PRE + ip, LOCKOUT_PRE + ip));
    }

    public boolean isBlocked(String ip) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(LOCKOUT_PRE + ip));
    }

    public long getLockoutRemaining(String ip) {
        Long ttl = stringRedisTemplate.getExpire(LOCKOUT_PRE + ip, TimeUnit.SECONDS);
        return ttl != null ? ttl : 0L;
    }

    private void increment(String key) {
        Long count = stringRedisTemplate.opsForValue().increment(key);
        if (count != null && count == 1)
            stringRedisTemplate.expire(key, Duration.ofSeconds(LOCKOUT_SECONDS));
        if (count != null && count >= MAX_ATTEMPTS) {
            String lockKey = key.replace(ATTEMPT_PRE, LOCKOUT_PRE);
            stringRedisTemplate.opsForValue().set(lockKey, "locked", Duration.ofSeconds(LOCKOUT_SECONDS));
            log.warn("Brute force lockout triggered: {}", key);
        }
    }
}