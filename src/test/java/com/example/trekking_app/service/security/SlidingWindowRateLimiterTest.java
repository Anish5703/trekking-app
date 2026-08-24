package com.example.trekking_app.service.security;

import com.example.trekking_app.model.RateLimitResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlidingWindowRateLimiterTest {

    @InjectMocks
    private SlidingWindowRateLimiter slidingWindowRateLimiter;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void testIsAllowed_UnderLimit_ShouldAllow() {
        String identifier = "user123";
        int limit = 100;
        long windowSeconds = 60;

        when(stringRedisTemplate.execute(any(RedisScript.class),anyList(),eq(String.valueOf(limit)),eq(String.valueOf(windowSeconds)),anyString())).thenReturn(List.of(1L,95L,0L));

        RateLimitResult result = slidingWindowRateLimiter.isAllowed(identifier, limit, windowSeconds);

        assertTrue(result.isAllowed());
        assertEquals(95, result.getRemaining());

        verify(stringRedisTemplate, times(1)).execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString());
    }

    @Test
    void testIsAllowed_ShouldBlock() {
        String identifier = "user123";
        int limit = 10;
        long windowSeconds = 60;

        when(stringRedisTemplate.execute(any(RedisScript.class),anyList(),eq(String.valueOf(limit)),eq(String.valueOf(windowSeconds)),anyString())).thenReturn(List.of(0L,0L,45L));

        RateLimitResult result = slidingWindowRateLimiter.isAllowed(identifier, limit, windowSeconds);

        assertFalse(result.isAllowed());
        assertEquals(0, result.getRemaining());
        assertEquals(45,result.getRetryAfter());

    }

    @Test
    void testIsAllowed_NullPipelineResults() {
        String identifier = "user123";
        int limit = 10;
        long windowSeconds = 60;

        when(stringRedisTemplate.execute(any(RedisScript.class),anyList(),anyString(),anyString(),anyString())).thenReturn(Collections.emptyList());

        assertThrows(IllegalStateException.class, () -> slidingWindowRateLimiter.isAllowed(identifier, limit, windowSeconds));
    }
}