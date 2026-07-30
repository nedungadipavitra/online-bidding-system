package com.onlinebidding.wallet_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinebidding.wallet_service.dto.AmountRequest;
import com.onlinebidding.wallet_service.dto.CreateWalletRequest;
import com.onlinebidding.wallet_service.dto.WalletDto;
import com.onlinebidding.wallet_service.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallets")
public class WalletController {

	private final WalletService walletService;

	@Autowired
	public WalletController(WalletService walletService) {
		this.walletService = walletService;
	}

	@GetMapping
	public ResponseEntity<List<WalletDto>> getAllWallets() {
		return ResponseEntity.ok(walletService.getAllWallets());
	}

	@GetMapping("/{walletId}")
	public ResponseEntity<WalletDto> getWalletById(@PathVariable("walletId") Long id) {
		return ResponseEntity.ok(walletService.getWalletById(id));
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<WalletDto> getWalletByUserId(@PathVariable("userId") Long userId) {
		return ResponseEntity.ok(walletService.getWalletByUserId(userId));
	}

	@PostMapping("/create")
	public ResponseEntity<WalletDto> createWallet(@RequestBody @Valid CreateWalletRequest dto) {
		return new ResponseEntity<>(walletService.createWallet(dto), HttpStatus.CREATED);
	}

	@PatchMapping("/update")
	public ResponseEntity<WalletDto> updateWallet(@RequestBody @Valid WalletDto dto) {
		return ResponseEntity.ok(walletService.updateWallet(dto));
	}

	@DeleteMapping("/delete/{walletId}")
	public ResponseEntity<String> deleteWalletById(@PathVariable("walletId") Long walletId) {
		walletService.deleteWalletById(walletId);
		return ResponseEntity.ok("Wallet has been deleted successfully!");
	}

	@PostMapping("/{userId}/deposit")
	public ResponseEntity<WalletDto> deposit(@PathVariable("userId") Long userId, @Valid @RequestBody AmountRequest request) {
		return ResponseEntity.ok(walletService.deposit(userId, request.getAmount()));
	}

	@PostMapping("/{userId}/withdraw")
	public ResponseEntity<WalletDto> withdraw(@PathVariable("userId") Long userId, @Valid @RequestBody AmountRequest request) {
		return ResponseEntity.ok(walletService.withdraw(userId, request.getAmount()));
	}
}
