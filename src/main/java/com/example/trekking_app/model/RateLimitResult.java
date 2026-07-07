package com.example.trekking_app.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RateLimitResult {
    private final boolean allowed;
    private final long    remaining;
    private final long    retryAfter;

    public static RateLimitResult allowed(long remaining) {
        return new RateLimitResult(true, remaining, 0);
    }
    public static RateLimitResult blocked(long remaining, long retryAfter) {
        return new RateLimitResult(false, remaining, retryAfter);
    }
}