package com.onlinebidding.wallet_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
	public ResponseEntity<List<TransactionDto>> getTransactionsByUserId(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(transactionService.getTransactionsByUserId(userId));
	}

	@GetMapping
	public ResponseEntity<List<TransactionDto>> getAllTransactions() {
		return ResponseEntity.ok(transactionService.getAllTransactions());
	}
}
