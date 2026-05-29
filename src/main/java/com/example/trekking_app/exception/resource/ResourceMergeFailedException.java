package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class ResourceMergeFailedException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object fieldValue;
    public ResourceMergeFailedException(String resourceName,String fieldName , Object fieldValue) {

        super(String.format("failed to merge  %s  with %s : %s",resourceName,fieldName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.fieldValue = fieldValue;
    }

    public ResourceMergeFailedException(String message)
    {
        super(message);
        resourceName = "";
        fieldName = "";
        fieldValue = "";
    }
}
