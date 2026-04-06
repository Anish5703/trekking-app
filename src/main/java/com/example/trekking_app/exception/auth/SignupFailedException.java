package com.example.trekking_app.exception.auth;

public class SignupFailedException extends RuntimeException {
    public SignupFailedException(String message) {
        super(message);
    }
}
