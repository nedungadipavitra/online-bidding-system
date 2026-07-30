package com.onlinebidding.bid_service.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bids")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bid {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long auctionId;

    @Column(name = "buyer_id", nullable = false)
    private Long bidderId;

    @Column(name = "bidder_name", nullable = false)
    private String bidderName;

    @Column(name = "bid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "bid_time", nullable = false)
    private LocalDateTime bidTime;
}
