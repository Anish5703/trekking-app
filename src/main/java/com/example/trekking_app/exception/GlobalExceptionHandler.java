package com.example.trekking_app.exception;

import com.example.trekking_app.dto.global.ApiResponse;
import com.example.trekking_app.dto.global.ErrorResponse;
import com.example.trekking_app.exception.auth.*;
import com.example.trekking_app.exception.resource.*;
import com.example.trekking_app.exception.route.CreateRouteFailedException;
import com.example.trekking_app.exception.route.FileParsingFailedException;
import com.example.trekking_app.exception.route.RouteNotFoundException;
import com.example.trekking_app.exception.user.DeleteUserFailedException;
import com.example.trekking_app.model.ErrorType;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.tool.schema.spi.CommandAcceptanceException;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailSendException;
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
        log.error("Signup field is empty {}",ex.getMessage());
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
        log.error("Duplicate email found {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.DUPLICATE_EMAIL_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),409);
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

    @ExceptionHandler(MailSendException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleMailSendFailed(MailSendException ex)
    {
        log.error("Failed send mail :{}",ex.getFailedMessages());
        ErrorResponse data = new ErrorResponse(ErrorType.SEND_MAIL_FAILED,"failed to send mail");
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),500);
        return ResponseEntity.badRequest().body(response);
    }
    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEmailAlreadyVerified(EmailAlreadyVerifiedException ex)
    {
        log.error("Email already verified {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.ALREADY_VERIFIED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }
    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEmailNotVerified(EmailNotVerifiedException ex)
    {
        log.error("Email not verified {}",ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.NOT_VERIFIED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),403);
        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleEmailNotFound(UsernameNotFoundException ex)
    {
        log.error("User with this email not found : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMAIL_NOT_FOUND,ex.getLocalizedMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),404);
        return ResponseEntity.status(404).body(response);

    }

    @ExceptionHandler({UserNotFoundException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleUserNotFound(UserNotFoundException ex)
    {
        log.error("Email not registered : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMAIL_NOT_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),404);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(LoginFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleLoginFailed(LoginFailedException ex)
    {
        log.error("Login failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.LOGIN_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(DeleteUserFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDeleteUserFailed(DeleteUserFailedException ex)
    {
        log.error("Delete user failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.DELETE_USER_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(CreateRouteFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleCreateRouteFailed(CreateRouteFailedException ex)
    {
        log.error("Create route Failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.CREATE_ROUTE_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler(RouteNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleRouteNotFound(RouteNotFoundException ex)
    {
        log.error("Route not found : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.ROUTE_NOT_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }



    @ExceptionHandler(FileParsingFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleFileParsingFailed(FileParsingFailedException ex)
    {
        log.error("File parsing failed : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.FILE_PARSING_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }

    @ExceptionHandler({JwtException.class,SignatureException.class , ExpiredJwtException.class, ClaimJwtException.class, MalformedJwtException.class})
    public ResponseEntity<ApiResponse<ErrorResponse>> handleJwtException(Exception ex)
    {
        log.error("JwtException : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.LOGIN_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.badRequest().body(response);
    }


    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceNotFound(ResourceNotFoundException ex)
    {
        log.error("Resource not found : {}",ex.getLocalizedMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.RESOURCE_NOT_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),404);
        return ResponseEntity.status(404).body(response);
    }
    @ExceptionHandler(ResourceCreationFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceCreationFailed(ResourceCreationFailedException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.RESOURCE_CREATE_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }
    @ExceptionHandler(ResourceUpdateFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceUpdateFailed(ResourceUpdateFailedException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.RESOURCE_UPDATE_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }
    @ExceptionHandler(ResourceDeletionFailedException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleResourceDeletionFailed(ResourceDeletionFailedException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.RESOURCE_DELETE_FAILED,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNoResourceFound(NoResourceFoundException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.NO_RESOURCE_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }
    @ExceptionHandler(DuplicateResourceFoundException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleDuplicateResourceFound(DuplicateResourceFoundException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.DUPLICATE_RESOURCE_FOUND,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNullPointerException(NullPointerException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.EMPTY_FIELD,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<ErrorResponse>> handleNullPointerException(IllegalArgumentException ex)
    {
        log.error(ex.getMessage());
        ErrorResponse data = new ErrorResponse(ErrorType.ILLEGAL_ARGUMENTS,ex.getMessage());
        ApiResponse<ErrorResponse> response = new ApiResponse<>(data,ex.getLocalizedMessage(),400);
        return ResponseEntity.status(400).body(response);
    }



    @ExceptionHandler(CommandAcceptanceException.class)
    public void handleCommandAcceptanceFailed(CommandAcceptanceException ex)
    {
        log.error(ex.getLocalizedMessage());
    }



}
