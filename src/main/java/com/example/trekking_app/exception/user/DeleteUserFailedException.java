package com.example.trekking_app.exception.user;

public class DeleteUserFailedException extends RuntimeException {
    public DeleteUserFailedException(String message) {
        super(message);
    }
}
