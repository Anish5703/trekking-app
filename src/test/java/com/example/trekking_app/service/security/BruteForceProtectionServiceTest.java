package com.example.trekking_app.service.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BruteForceProtectionServiceTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;
    @Mock
    private ValueOperations<String,String> valueOperations;

    @InjectMocks
    private BruteForceProtectionService bruteForceProtectionService;


    @Test
    void testRecordFailedAttempt_FirstFailedAttempt() {

        String ip = "127.0.0.1";
        String attemptKey= "bf:attempt:127.0.0.1";

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(attemptKey)).thenReturn(1L);
        bruteForceProtectionService.recordFailedAttempt(ip);

        verify(stringRedisTemplate).expire(eq(attemptKey), eq(Duration.ofSeconds(900)));
        verify(valueOperations).increment(attemptKey);
        verify(valueOperations,never()).set(anyString(),anyString(),eq(Duration.ofSeconds(900)));
    }

    @Test
    void testRecordFailedAttempt_MaxFailedAttempt() {
        String ip = "127.0.0.1";
        String attemptKey = "bf:attempt:127.0.0.1";
        String lockoutKey= "bf:lockout:127.0.0.1";

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(attemptKey)).thenReturn(5L);
        bruteForceProtectionService.recordFailedAttempt(ip);

        verify(valueOperations).increment(attemptKey);
        verify(valueOperations).set(lockoutKey,"locked",Duration.ofSeconds(900));
    }

    @Test
    void testIsBlocked_IpIsBlocked() {
        //should return true
        String ip = "127.0.0.1";
        String lockoutKey = "bf:lockout:127.0.0.1";

        when(stringRedisTemplate.hasKey(lockoutKey)).thenReturn(true);

        boolean  blocked = bruteForceProtectionService.isBlocked(ip);
        assertTrue(blocked);
        verify(stringRedisTemplate).hasKey(lockoutKey);
    }

    @Test
    void testIsBlocked_IpNotBlocked() {
        //returns false
        String ip = "127.0.0.1";
        String lockoutKey= "bf:lockout:127.0.0.1";

        when(stringRedisTemplate.hasKey(lockoutKey)).thenReturn(false);
        boolean blocked = bruteForceProtectionService.isBlocked(ip);

        assertFalse(blocked);
        verify(stringRedisTemplate).hasKey(lockoutKey);
    }

    @Test
    void testResetAttempts_Success() {
        String ip = "127.0.0.1";
        String attemptKey = "bf:attempt:127.0.0.1";
        String lockoutKey = "bf:lockout:127.0.0.1";

        bruteForceProtectionService.resetAttempts(ip);
        verify(stringRedisTemplate).delete(List.of(attemptKey,lockoutKey));
    }

    @Test
    void testGetLockoutRemaining_KeyExists(){
        String ip = "127.0.0.1";
        String lockoutKey = "bf:lockout:127.0.0.1";

        when(stringRedisTemplate.getExpire(lockoutKey, TimeUnit.SECONDS)).thenReturn(300L);
        Long remainingTime = bruteForceProtectionService.getLockoutRemaining(ip);

        assertEquals(300L,remainingTime);
    }

    @Test
    void testGetLockoutRemaining_KeyDoesNotExist() {

        String ip = "127.0.0.1";
        String lockoutKey = "bf:lockout:127.0.0.1";

        when(stringRedisTemplate.getExpire(lockoutKey,TimeUnit.SECONDS)).thenReturn(0L);
        Long remainingTime = bruteForceProtectionService.getLockoutRemaining(ip);

        assertEquals(0L,remainingTime);
    }
}
