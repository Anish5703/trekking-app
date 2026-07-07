package com.example.trekking_app.exception.auth;

import lombok.Getter;

@Getter
public class AccountLockedException extends RuntimeException {
    private final long retryAfter;
    public AccountLockedException(long retryAfter) {
        super("Account locked due to too many failed attempts.");
        this.retryAfter = retryAfter;
    }
}

