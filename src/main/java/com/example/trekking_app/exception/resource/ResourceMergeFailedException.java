package com.example.trekking_app.exception.resource;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
public class ResourceMergeFailedException extends RuntimeException {
    private final String resourceName;
    private final String fieldName;
    private final Object filedValue;
    public ResourceMergeFailedException(String resourceName,String fieldName , Object fieldValue) {

        super(String.format("failed to merge  %s  with %s : %s",resourceName,fieldName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.filedValue = fieldValue;
    }
}
