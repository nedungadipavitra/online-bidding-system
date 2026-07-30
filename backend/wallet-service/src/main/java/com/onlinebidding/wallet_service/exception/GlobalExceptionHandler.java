package com.onlinebidding.wallet_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResourceNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
				"timestamp", LocalDateTime.now(),
				"status", HttpStatus.NOT_FOUND.value(),
				"message", ex.getMessage()
		));
	}

	@ExceptionHandler(InsufficientBalanceException.class)
	public ResponseEntity<Map<String, Object>> handleInsufficientBalance(InsufficientBalanceException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"timestamp", LocalDateTime.now(),
				"status", HttpStatus.BAD_REQUEST.value(),
				"message", ex.getMessage()
		));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
				"timestamp", LocalDateTime.now(),
				"status", HttpStatus.BAD_REQUEST.value(),
				"message", ex.getMessage()
		));
	}
}
