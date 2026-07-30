package com.onlinebidding.user_service.exception;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class GlobalExceptionHandler {
	private ResponseEntity<ErrorResponse> buildResponse(HttpStatus status, String message, HttpServletRequest request) {
	    ErrorResponse response = ErrorResponse.builder()
	            .timestamp(LocalDateTime.now())
	            .status(status.value())
	            .error(status.getReasonPhrase())
	            .message(message)
	            .path(request.getRequestURI())
	            .build();

	    return ResponseEntity.status(status).body(response);
	}
	
	@ExceptionHandler(EmailAlreadyExistsException.class)
	public ResponseEntity<ErrorResponse> handleEmailExists(EmailAlreadyExistsException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request);
	}
	
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.UNAUTHORIZED, "Invalid email or password", request);
	}
	
	@ExceptionHandler(InvalidRefreshTokenException.class)
	public ResponseEntity<ErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}
	
	@ExceptionHandler(RefreshTokenExpiredException.class)
	public ResponseEntity<ErrorResponse> handleExpiredRefreshToken(RefreshTokenExpiredException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}
	
	@ExceptionHandler(RefreshTokenRevokedException.class)
	public ResponseEntity<ErrorResponse> handleRevokedRefreshToken(RefreshTokenRevokedException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}
	
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
	    ex.printStackTrace();
	    return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong", request);
	}
	
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
	    Map<String, String> errors = new LinkedHashMap<>();

	    ex.getBindingResult()
	            .getFieldErrors()
	            .forEach(error ->
	                    errors.put(
	                            error.getField(),
	                            error.getDefaultMessage()
	                    ));

	    ErrorResponse response = ErrorResponse.builder()
	            .timestamp(LocalDateTime.now())
	            .status(HttpStatus.BAD_REQUEST.value())
	            .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
	            .message("Validation failed")
	            .path(request.getRequestURI())
	            .errors(errors)
	            .build();

	    return ResponseEntity.badRequest().body(response);
	}
	
	@ExceptionHandler(JwtAuthenticationException.class)
	public ResponseEntity<ErrorResponse> handleJwtAuthenticationException(JwtAuthenticationException ex, HttpServletRequest request) {
	    return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
	}
}