package com.example.trekking_app.exception.auth;

public class DuplicateEmailFoundException extends RuntimeException {
    public DuplicateEmailFoundException(String message) {
        super(message);
    }
}
