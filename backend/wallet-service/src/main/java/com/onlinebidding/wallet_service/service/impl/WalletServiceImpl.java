package com.onlinebidding.wallet_service.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinebidding.wallet_service.dto.CreateWalletRequest;
import com.onlinebidding.wallet_service.dto.BidSettlementRequest;
import com.onlinebidding.wallet_service.dto.WalletDto;
import com.onlinebidding.wallet_service.entity.Transaction;
import com.onlinebidding.wallet_service.entity.TransactionType;
import com.onlinebidding.wallet_service.entity.Wallet;
import com.onlinebidding.wallet_service.entity.WalletStatus;
import com.onlinebidding.wallet_service.exception.InsufficientBalanceException;
import com.onlinebidding.wallet_service.exception.ResourceNotFoundException;
import com.onlinebidding.wallet_service.mapper.WalletMapper;
import com.onlinebidding.wallet_service.repository.TransactionRepository;
import com.onlinebidding.wallet_service.repository.WalletRepository;
import com.onlinebidding.wallet_service.service.WalletService;

@Service
@Transactional
public class WalletServiceImpl implements WalletService {

	private final WalletRepository walletRepo;
	private final WalletMapper mapper;
	private final TransactionRepository transactionRepo;

	@Autowired
	public WalletServiceImpl(WalletRepository walletRepo, WalletMapper mapper, TransactionRepository transactionRepo) {
		this.walletRepo = walletRepo;
		this.mapper = mapper;
		this.transactionRepo = transactionRepo;
	}

	@Override
	public WalletDto createWallet(CreateWalletRequest dto) {
		Wallet newWallet = Wallet.builder()
				.userId(dto.getUserId())
				.balance(dto.getInitialBalance() != null ? dto.getInitialBalance() : BigDecimal.ZERO)
				.status(WalletStatus.ACTIVE)
				.build();
		Wallet savedWallet = walletRepo.save(newWallet);
		return mapper.toDto(savedWallet);
	}

	@Override
	public WalletDto updateWallet(WalletDto updatedto) {
		Wallet updatedWallet = walletRepo.findById(updatedto.getWalletId())
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + updatedto.getWalletId()));
		mapper.updateWallet(updatedto, updatedWallet);
		return mapper.toDto(walletRepo.save(updatedWallet));
	}

	@Override
	public void deleteWalletById(Long walletId) {
		Wallet wallet = walletRepo.findById(walletId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + walletId));
		walletRepo.delete(wallet);
	}

	@Override
	@Transactional(readOnly = true)
	public List<WalletDto> getAllWallets() {
		return walletRepo.findAll().stream().map(mapper::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public WalletDto getWalletById(Long id) {
		Wallet wallet = walletRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found with id: " + id));
		return mapper.toDto(wallet);
	}

	@Override
	@Transactional(readOnly = true)
	public WalletDto getWalletByUserId(Long userId) {
		Wallet wallet = walletRepo.findByUserId(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
		return mapper.toDto(wallet);
	}

	@Override
	public WalletDto deposit(Long userId, BigDecimal amount) {
		Wallet wallet = walletRepo.findByUserIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
		wallet.setBalance(wallet.getBalance().add(amount));

		Transaction transaction = Transaction.builder()
				.amount(amount)
				.type(TransactionType.DEPOSIT)
				.description("Deposit to wallet")
				.timestamp(LocalDateTime.now())
				.userId(userId)
				.build();
		transactionRepo.save(transaction);

		return mapper.toDto(walletRepo.save(wallet));
	}

	@Override
	public WalletDto withdraw(Long userId, BigDecimal amount) {
		Wallet wallet = walletRepo.findByUserIdForUpdate(userId)
				.orElseThrow(() -> new ResourceNotFoundException("Wallet not found for user: " + userId));
		if (wallet.getBalance().compareTo(amount) < 0) {
			throw new InsufficientBalanceException("Insufficient wallet balance");
		}
		wallet.setBalance(wallet.getBalance().subtract(amount));

		Transaction transaction = Transaction.builder()
				.amount(amount)
				.type(TransactionType.WITHDRAW)
				.description("Withdrawal from wallet")
				.timestamp(LocalDateTime.now())
				.userId(userId)
				.build();
		transactionRepo.save(transaction);

		return mapper.toDto(walletRepo.save(wallet));
	}

	@Override
	public WalletDto settleBid(BidSettlementRequest request) {
		if (request.getBidderId().equals(request.getSellerId())) {
			throw new IllegalArgumentException("A seller cannot bid on their own auction");
		}

		BigDecimal previousAmount = request.getPreviousAmount() == null
				? BigDecimal.ZERO
				: request.getPreviousAmount();
		Map<Long, Wallet> wallets = lockWallets(request, previousAmount);
		Wallet bidderWallet = wallets.get(request.getBidderId());
		Wallet sellerWallet = wallets.get(request.getSellerId());

		if (bidderWallet.getBalance().compareTo(request.getNewAmount()) < 0) {
			throw new InsufficientBalanceException("Insufficient wallet balance for this bid");
		}

		bidderWallet.setBalance(bidderWallet.getBalance().subtract(request.getNewAmount()));
		sellerWallet.setBalance(sellerWallet.getBalance().add(request.getNewAmount()));
		saveTransaction(request.getBidderId(), request.getNewAmount(), TransactionType.WITHDRAW,
				"Bid amount reserved");
		saveTransaction(request.getSellerId(), request.getNewAmount(), TransactionType.DEPOSIT,
				"Bid received");

		if (request.getPreviousBidderId() != null && previousAmount.compareTo(BigDecimal.ZERO) > 0) {
			Wallet previousBidderWallet = wallets.get(request.getPreviousBidderId());
			previousBidderWallet.setBalance(previousBidderWallet.getBalance().add(previousAmount));
			sellerWallet.setBalance(sellerWallet.getBalance().subtract(previousAmount));
			saveTransaction(request.getPreviousBidderId(), previousAmount, TransactionType.REFUND,
					"Previous highest bid refunded");
			saveTransaction(request.getSellerId(), previousAmount, TransactionType.WITHDRAW,
					"Previous bid replaced");
		}

		walletRepo.saveAll(wallets.values());
		return mapper.toDto(bidderWallet);
	}

	private Map<Long, Wallet> lockWallets(BidSettlementRequest request, BigDecimal previousAmount) {
		TreeSet<Long> userIds = new TreeSet<>();
		userIds.add(request.getBidderId());
		userIds.add(request.getSellerId());
		if (request.getPreviousBidderId() != null && previousAmount.compareTo(BigDecimal.ZERO) > 0) {
			userIds.add(request.getPreviousBidderId());
		}

		Map<Long, Wallet> wallets = new HashMap<>();
		for (Long userId : userIds) {
			Wallet wallet = walletRepo.findByUserIdForUpdate(userId).orElse(null);
			if (wallet == null && userId.equals(request.getSellerId())) {
				wallet = Wallet.builder()
						.userId(userId)
						.balance(BigDecimal.ZERO)
						.status(WalletStatus.ACTIVE)
						.build();
				wallet = walletRepo.save(wallet);
			}
			if (wallet == null) {
				throw new ResourceNotFoundException("Wallet not found for user: " + userId);
			}
			wallets.put(userId, wallet);
		}
		return wallets;
	}

	private void saveTransaction(Long userId, BigDecimal amount, TransactionType type, String description) {
		transactionRepo.save(Transaction.builder()
				.amount(amount)
				.type(type)
				.description(description)
				.timestamp(LocalDateTime.now())
				.userId(userId)
				.build());
	}
}
