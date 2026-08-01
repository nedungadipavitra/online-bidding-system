package com.onlinebidding.wallet_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.onlinebidding.wallet_service.dto.AmountRequest;
import com.onlinebidding.wallet_service.dto.BidSettlementRequest;
import com.onlinebidding.wallet_service.dto.CreateWalletRequest;
import com.onlinebidding.wallet_service.dto.WalletDto;
import com.onlinebidding.wallet_service.service.WalletService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/wallets")
public class WalletController {

	private final WalletService walletService;
	private final String internalServiceToken;

	@Autowired
	public WalletController(WalletService walletService,
			@Value("${internal.service-token}") String internalServiceToken) {
		this.walletService = walletService;
		this.internalServiceToken = internalServiceToken;
	}

	@GetMapping
    public ResponseEntity<List<WalletDto>> getAllWallets(
            @RequestHeader(value = "X-User-Role", required = false) String role) {
        requireAdmin(role);
        return ResponseEntity.ok(walletService.getAllWallets());
    }

	@GetMapping("/{walletId}")
	public ResponseEntity<WalletDto> getWalletById(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("walletId") Long id) {
        WalletDto wallet = walletService.getWalletById(id);
        requireOwnerOrAdmin(userIdHeader, role, wallet.getUserId());
		return ResponseEntity.ok(wallet);
	}

	@GetMapping("/user/{userId}")
	public ResponseEntity<WalletDto> getWalletByUserId(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("userId") Long userId) {
        requireOwnerOrAdmin(userIdHeader, role, userId);
		return ResponseEntity.ok(walletService.getWalletByUserId(userId));
	}

	@PostMapping("/create")
	public ResponseEntity<WalletDto> createWallet(
			@RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
			@RequestBody @Valid CreateWalletRequest dto) {
		requireInternalService(serviceToken);
		return new ResponseEntity<>(walletService.createWallet(dto), HttpStatus.CREATED);
	}

	@PostMapping("/settle-bid")
	public ResponseEntity<WalletDto> settleBid(
			@RequestHeader(value = "X-Internal-Service-Token", required = false) String serviceToken,
			@RequestBody @Valid BidSettlementRequest request) {
		requireInternalService(serviceToken);
		return ResponseEntity.ok(walletService.settleBid(request));
	}

	@PatchMapping("/update")
	public ResponseEntity<WalletDto> updateWallet(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestBody @Valid WalletDto dto) {
        requireAdmin(role);
		return ResponseEntity.ok(walletService.updateWallet(dto));
	}

	@DeleteMapping("/delete/{walletId}")
	public ResponseEntity<String> deleteWalletById(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @PathVariable("walletId") Long walletId) {
        requireAdmin(role);
		walletService.deleteWalletById(walletId);
		return ResponseEntity.ok("Wallet has been deleted successfully!");
	}

	@PostMapping("/{userId}/deposit")
	public ResponseEntity<WalletDto> deposit(
			@RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable("userId") Long userId,
			@Valid @RequestBody AmountRequest request) {
		requireOwnerOrAdmin(userIdHeader, role, userId);
		return ResponseEntity.ok(walletService.deposit(userId, request.getAmount()));
	}

	@PostMapping("/{userId}/withdraw")
	public ResponseEntity<WalletDto> withdraw(
			@RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable("userId") Long userId,
			@Valid @RequestBody AmountRequest request) {
		requireOwnerOrAdmin(userIdHeader, role, userId);
		return ResponseEntity.ok(walletService.withdraw(userId, request.getAmount()));
	}

    private void requireAdmin(String role) {
        if (!"ADMIN".equalsIgnoreCase(role)) {
            throw forbidden("Administrator access is required.");
        }
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
        throw forbidden("You are not allowed to access another user's wallet.");
    }

	private ResponseStatusException forbidden(String message) {
		return new ResponseStatusException(HttpStatus.FORBIDDEN, message);
	}

	private void requireInternalService(String serviceToken) {
		if (serviceToken == null || !internalServiceToken.equals(serviceToken)) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Internal service access is required.");
		}
	}
}
