package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object filedValue;
    public ResourceNotFoundException(String resourceName, String fieldName, Object filedValue) {
        super(String.format("%s not found with %s : %s",resourceName, fieldName, filedValue));
        this.resourceName  = resourceName;
        this.fieldName = fieldName;
        this.filedValue = filedValue;

    }
}
