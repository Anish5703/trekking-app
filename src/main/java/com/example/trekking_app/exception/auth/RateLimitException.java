package com.example.trekking_app.exception.auth;

import lombok.Getter;

@Getter
public class RateLimitException extends RuntimeException {
    private final long retryAfter;
    public RateLimitException(long retryAfter) {
        super("Rate limit exceeded.");
        this.retryAfter = retryAfter;
    }
}