package com.onlinebidding.wallet_service.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinebidding.wallet_service.dto.TransactionDto;
import com.onlinebidding.wallet_service.entity.Transaction;
import com.onlinebidding.wallet_service.repository.TransactionRepository;
import com.onlinebidding.wallet_service.service.TransactionService;

@Service
@Transactional(readOnly = true)
public class TransactionServiceImpl implements TransactionService {

	private final TransactionRepository transactionRepo;

	@Autowired
	public TransactionServiceImpl(TransactionRepository transactionRepo) {
		this.transactionRepo = transactionRepo;
	}

	@Override
	public List<TransactionDto> getTransactionsByUserId(Long userId) {
		return transactionRepo.findByUserIdOrderByTimestampDesc(userId).stream()
				.map(this::mapToDto)
				.toList();
	}

	@Override
	public List<TransactionDto> getAllTransactions() {
		return transactionRepo.findAll().stream()
				.map(this::mapToDto)
				.toList();
	}

	private TransactionDto mapToDto(Transaction t) {
		return TransactionDto.builder()
				.id(t.getId())
				.amount(t.getAmount())
				.type(t.getType())
				.description(t.getDescription())
				.timestamp(t.getTimestamp())
				.userId(t.getUserId())
				.build();
	}
}
