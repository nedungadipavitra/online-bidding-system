package com.onlinebidding.wallet_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.onlinebidding.wallet_service.dto.TransactionDto;
import com.onlinebidding.wallet_service.service.TransactionService;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

	private final TransactionService transactionService;

	@Autowired
	public TransactionController(TransactionService transactionService) {
		this.transactionService = transactionService;
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<List<TransactionDto>> getTransactionsByUserId(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("userId") Long userId) {
        requireOwnerOrAdmin(userIdHeader, role, userId);
		return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId));
	}

	@GetMapping
	public ResponseEntity<List<TransactionDto>> getAllTransactions(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access is required.");
        }
		return ResponseEntity.ok(transactionService.getAllTransactions());
	}

    private void requireOwnerOrAdmin(String userIdHeader, String role, Long targetUserId) {
        if ("ADMIN".equalsIgnoreCase(role)) {
            return;
        }
        try {
            if (userIdHeader != null && Long.valueOf(userIdHeader).equals(targetUserId)) {
                return;
            }
        } catch (NumberFormatException ignored) {
            // Fall through to the standard forbidden response.
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "You are not allowed to access another user's transactions.");
    }
}
