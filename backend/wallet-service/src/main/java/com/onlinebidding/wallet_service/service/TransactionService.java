package com.onlinebidding.wallet_service.service;

import java.util.List;
import com.onlinebidding.wallet_service.dto.TransactionDto;

public interface TransactionService {
	List<TransactionDto> getTransactionsByUserId(Long userId);
	List<TransactionDto> getAllTransactions();
}
