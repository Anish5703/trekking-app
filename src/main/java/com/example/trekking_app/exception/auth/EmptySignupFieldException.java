package com.example.trekking_app.exception.auth;

public class EmptySignupFieldException extends RuntimeException {
    public EmptySignupFieldException(String message) {
        super(message);
    }
}
