package com.example.trekking_app.exception.contour;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class ElevationUnavailableException extends RuntimeException {
    public ElevationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}