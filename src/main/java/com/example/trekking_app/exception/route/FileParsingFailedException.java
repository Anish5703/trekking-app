package com.example.trekking_app.exception.route;

public class FileParsingFailedException extends RuntimeException {
    public FileParsingFailedException(String message) {
        super(message);
    }
}
