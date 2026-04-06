package com.example.trekking_app.exception;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.global.ErrorResponse;
import com.example.trekking_app.exception.auth.*;
import com.example.trekking_app.exception.user.DeleteUserFailedException;
import com.example.trekking_app.model.ErrorType;
import io.jsonwebtoken.JwtException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
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
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEmptySignupField(EmptySignupFieldException ex)
    {
        log.error("Signup Filed is empty {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMPTY_FIELD,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);

    }

    /*
    * Handles exception thrown by user signup attempt with existing email
     */
    @ExceptionHandler(DuplicateEmailFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDuplicateEmailFound(DuplicateEmailFoundException ex)
    {
        log.error("Duplicate Email found {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.DUPLICATE_EMAIL,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }
    /*
    * Handles exception thrown by user signup attempt failed
     */
    @ExceptionHandler(SignupFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleSignupFailed(SignupFailedException ex)
    {
        log.error("Signup attempt failed {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.SIGNUP_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEmailNotFound(UsernameNotFoundException ex)
    {
        log.error("User email not found : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMAIL_NOT_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);

    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserNotFound(UserNotFoundException ex)
    {
        log.error("Email not registered : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMAIL_NOT_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleLoginFailed(LoginFailedException ex)
    {
        log.error("Login Failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.LOGIN_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DeleteUserFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDeleteUserFailed(DeleteUserFailedException ex)
    {
        log.error("Delete User Failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.DELETE_USER_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJwtException(JwtException ex)
    {
        log.error("JwtException : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.JWT_EXCEPTION,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }
}
