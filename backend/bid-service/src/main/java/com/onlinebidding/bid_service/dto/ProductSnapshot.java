package com.onlinebidding.bid_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ProductSnapshot {
	private Long productId;
	private BigDecimal basePrice;
	private BigDecimal currentHighestBid;
	private LocalDateTime auctionStartTime;
	private LocalDateTime auctionEndTime;
	private String status;
	private Long sellerId;
}
