package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class ResourceDeletionFailedException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;


    public ResourceDeletionFailedException(String resourceName, String fieldName , Object fieldValue)
    {

        super(String.format("failed to delete %s wit %s : %s",resourceName,fieldName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }
}
