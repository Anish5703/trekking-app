package com.example.trekking_app.exception;

import com.example.trekking_app.dto.global.ApiMessage;
import com.example.trekking_app.dto.global.ErrorResponse;
import com.example.trekking_app.exception.auth.DuplicateEmailFoundException;
import com.example.trekking_app.exception.auth.EmptySignupFieldException;
import com.example.trekking_app.exception.auth.SignupFailedException;
import com.example.trekking_app.model.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/*
* Handles thrown exception
* returns ErrorResponse object
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /*
    * Handles exception thrown by user signup attempt with empty non-nullable field in user entity
     */
    @ExceptionHandler(EmptySignupFieldException.class)
    public ResponseEntity<ErrorResponse> handleEmptySignupField(EmptySignupFieldException ex)
    {
        log.error("Signup Filed is empty {}",ex.getMessage());
        ApiMessage message = new ApiMessage(400,ex.getMessage());
        ErrorResponse response = new ErrorResponse(ErrorType.EMPTY_FIELD,message);
        return ResponseEntity.badRequest().body(response);

    }

    /*
    * Handles exception thrown by user signup attempt with existing email
     */
    @ExceptionHandler(DuplicateEmailFoundException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmailFound(DuplicateEmailFoundException ex)
    {
        log.error("Duplicate Email found {}",ex.getMessage());
        ApiMessage message = new ApiMessage(400,ex.getMessage());
        ErrorResponse response = new ErrorResponse(ErrorType.DUPLICATE_EMAIL,message);
        return ResponseEntity.badRequest().body(response);
    }
    /*
    * Handles exception thrown by user signup attempt failed
     */
    public ResponseEntity<ErrorResponse> handleSignupFailed(SignupFailedException ex)
    {
        log.error("Signup attempt failed {}",ex.getMessage());
        ApiMessage message = new ApiMessage(500,ex.getMessage());
        ErrorResponse response = new ErrorResponse(ErrorType.SIGNUP_FAILED,message);
        return ResponseEntity.badRequest().body(response);
    }
}
