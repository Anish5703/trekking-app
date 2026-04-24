package com.example.trekking_app.exception.destination;

public class DuplicateDestinationFoundException extends RuntimeException {
    public DuplicateDestinationFoundException(String message) {
        super(message);
    }
}
