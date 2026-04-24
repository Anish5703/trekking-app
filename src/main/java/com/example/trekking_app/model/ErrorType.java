package com.example.trekking_app.model;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = """
        Error codes returned in the error response body when a request fails.
        Read this field to decide what message to show the user or what
        action to take on the frontend.
        
        
        GENERAL
        
        ILLEGAL_ARGUMENTS          -> one or more request fields have invalid values
        ALREADY_VERIFIED           -> the email address is already verified, no action needed
        NOT_VERIFIED               -> the user has not verified their email yet,
                                      redirect to a "check your email" screen
        
        
        FIELD VALIDATION
        
        EMPTY_FIELD                -> a required field was left blank
        
        
        AUTH / USER
        
        DUPLICATE_EMAIL_FOUND      -> an account with this email already exists,
                                      show "try logging in instead"
        EMAIL_NOT_FOUND            -> no account found with this email
        SIGNUP_FAILED              -> account could not be created, ask the user to retry
        LOGIN_FAILED               -> credentials did not match, show "invalid email or password"
        DELETE_USER_FAILED         -> user account could not be deleted
        
        
        RESOURCE (generic)
        
        RESOURCE_NOT_FOUND         -> the requested item does not exist
        DUPLICATE_RESOURCE_FOUND   -> an item with the same identifier already exists
        RESOURCE_UPDATE_FAILED     -> the item could not be updated
        RESOURCE_DELETE_FAILED     -> the item could not be deleted
        RESOURCE_CREATE_FAILED     -> the item could not be created
        NO_RESOURCE_FOUND          -> the list came back empty, show an empty state
        
        
        ROUTES
        
        CREATE_ROUTE_FAILED        -> route could not be created
        ROUTE_NOT_FOUND            -> no route exists with the given ID or name
        
        
        DESTINATIONS
        
        DESTINATION_NOT_FOUND      -> no destination exists with the given ID or name
        DUPLICATE_DESTINATION_FOUND-> a destination with this name or location already exists
        
        
        FILES
        
        FILE_PARSING_FAILED        -> the uploaded file could not be read or processed,
                                      check the file format and try again
        """,
        enumAsRef = true
)
public enum ErrorType {
    /* General Errors */
    ILLEGAL_ARGUMENTS,
    ALREADY_VERIFIED,
    NOT_VERIFIED,

    /* Signup Errors */
    EMPTY_FIELD,
    DUPLICATE_EMAIL_FOUND,
    EMAIL_NOT_FOUND,
    SIGNUP_FAILED,
    LOGIN_FAILED,
    DELETE_USER_FAILED,
    CREATE_ROUTE_FAILED,
    RESOURCE_NOT_FOUND,
    DUPLICATE_RESOURCE_FOUND,
    RESOURCE_UPDATE_FAILED,
    RESOURCE_DELETE_FAILED,
    RESOURCE_CREATE_FAILED,
    NO_RESOURCE_FOUND,
    ROUTE_NOT_FOUND,
    DESTINATION_NOT_FOUND,
    DUPLICATE_DESTINATION_FOUND,
    FILE_PARSING_FAILED
}
