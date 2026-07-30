package com.onlinebidding.product_service.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", nullable = false)
	private Long productId;

	@Column(name = "name", nullable = false)
	private String name;

	private String description;

	@Column(name = "image_url", columnDefinition = "LONGTEXT")
	private String imageUrl;

	@Column(name = "base_price", nullable = false)
	private BigDecimal basePrice;

	@Column(name = "current_highest_bid")
	private BigDecimal currentHighestBid;

	@Column(name = "auction_start_time")
	private LocalDateTime auctionStartTime;

	@Column(name = "auction_end_time")
	private LocalDateTime auctionEndTime;

	@Enumerated(EnumType.STRING)
	private ProductStatus status;

	@Column(name = "category_id")
	private Long categoryId;

	@Column(name = "seller_id")
	private Long sellerId;
}
