package com.onlinebidding.wallet_service.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BidSettlementRequest {
	@NotNull(message = "Bidder ID is required")
	private Long bidderId;

	@NotNull(message = "Seller ID is required")
	private Long sellerId;

	@NotNull(message = "Bid amount is required")
	@Positive(message = "Bid amount must be strictly positive")
	private BigDecimal newAmount;

	private Long previousBidderId;

	@PositiveOrZero(message = "Previous bid amount cannot be negative")
	private BigDecimal previousAmount = BigDecimal.ZERO;
}
