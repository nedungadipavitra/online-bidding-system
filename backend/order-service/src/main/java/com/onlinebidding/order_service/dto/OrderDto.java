package com.onlinebidding.order_service.dto;

import com.onlinebidding.order_service.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderDto {
    private String id;
    private Long productId;
    private Long buyerId;
    private Long sellerId;
    private Long deliveryPersonId;
    private String deliveryPersonName;
    private LocalDateTime assignedDate;
    private String deliveryStatus;
    private LocalDateTime estimatedDelivery;
    private BigDecimal finalPrice;
    private OrderStatus status;
    private LocalDateTime createdAt;
}
