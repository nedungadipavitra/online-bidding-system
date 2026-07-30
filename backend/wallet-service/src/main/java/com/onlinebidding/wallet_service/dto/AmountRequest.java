package com.onlinebidding.wallet_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AmountRequest {
	@NotNull(message = "Amount is required")
	@Positive(message = "Amount must be strictly positive")
	private BigDecimal amount;
}
