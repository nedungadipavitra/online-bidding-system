package com.onlinebidding.product_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.onlinebidding.product_service.entity.ProductStatus;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductDto {
	private Long productId;

	@NotBlank(message = "Product name is required")
	private String name;

	private String description;
	private String imageUrl;

	@NotNull(message = "Base price is required")
	@Positive(message = "Base price must be positive")
	private BigDecimal basePrice;

	private BigDecimal currentHighestBid;

	private LocalDateTime auctionStartTime;

	@NotNull(message = "Auction end time is required")
	@Future(message = "Auction end time must be in the future")
	private LocalDateTime auctionEndTime;

	private ProductStatus status;
	private Long categoryId;
	private Long sellerId;
}
