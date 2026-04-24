package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class ResourceUpdateFailedException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    public ResourceUpdateFailedException(String resourceName, String fieldName, Object fieldValue)
    {
        super(String.format("failed to update %s with %s : %s",resourceName,fieldName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
