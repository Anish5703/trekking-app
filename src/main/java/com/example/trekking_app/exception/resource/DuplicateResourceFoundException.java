package com.example.trekking_app.exception.resource;

import lombok.Getter;

@Getter
public class DuplicateResourceFoundException extends RuntimeException {

    private final String resourceName;
    private final String fieldName;
    private final Object filedValue;
    public DuplicateResourceFoundException(String resourceName,String fieldName , Object fieldValue) {

        super(String.format("duplicate  %s found with %s : %s",resourceName,fieldName,fieldValue));
        this.resourceName = resourceName;
        this.fieldName = fieldName;
        this.filedValue = fieldValue;
    }
}
