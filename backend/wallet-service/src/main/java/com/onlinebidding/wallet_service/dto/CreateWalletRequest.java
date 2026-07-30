package com.onlinebidding.wallet_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateWalletRequest {
	@NotNull(message = "User ID is required")
	private Long userId;

	@NotNull(message = "Initial balance is required")
	@PositiveOrZero(message = "Initial balance cannot be negative")
	private BigDecimal initialBalance;
}
