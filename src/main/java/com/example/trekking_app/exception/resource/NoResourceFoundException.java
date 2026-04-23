package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class NoResourceFoundException extends RuntimeException {
    private final String resourceName;
    public NoResourceFoundException(String resourceName)
    {
        super(String.format("no %s found ",resourceName));
        this.resourceName = resourceName;
    }

}
