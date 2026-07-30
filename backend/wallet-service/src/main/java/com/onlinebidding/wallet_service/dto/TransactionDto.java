package com.onlinebidding.wallet_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.onlinebidding.wallet_service.entity.TransactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
	private Long id;
	private BigDecimal amount;
	private TransactionType type;
	private String description;
	private LocalDateTime timestamp;
	private Long userId;
}
