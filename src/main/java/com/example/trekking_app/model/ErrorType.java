package com.example.trekking_app.model;

public enum ErrorType {
    /* General Errors */
    ILLEGAL_ARGUMENTS,

    /* Signup Errors */
    EMPTY_FIELD,
    DUPLICATE_EMAIL,
    EMAIL_NOT_FOUND,
    SIGNUP_FAILED,
    LOGIN_FAILED,
    DELETE_USER_FAILED,
    CREATE_ROUTE_FAILED,
    ROUTE_NOT_FOUND
}
