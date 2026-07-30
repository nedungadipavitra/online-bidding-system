package com.onlinebidding.wallet_service.service;

import java.math.BigDecimal;
import java.util.List;

import com.onlinebidding.wallet_service.dto.CreateWalletRequest;
import com.onlinebidding.wallet_service.dto.WalletDto;

public interface WalletService {
	WalletDto createWallet(CreateWalletRequest dto);
	WalletDto updateWallet(WalletDto updatedto);
	void deleteWalletById(Long walletId);
	List<WalletDto> getAllWallets();
	WalletDto getWalletById(Long id);
	WalletDto getWalletByUserId(Long userId);
	WalletDto deposit(Long userId, BigDecimal amount);
	WalletDto withdraw(Long userId, BigDecimal amount);
}
