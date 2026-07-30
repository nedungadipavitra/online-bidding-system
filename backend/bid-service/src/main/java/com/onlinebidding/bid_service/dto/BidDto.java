package com.onlinebidding.bid_service.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BidDto {
    private Long id;

    @JsonProperty("auctionId")
    @JsonAlias({ "productId", "product_id", "auction_id" })
    private Long auctionId;

    @JsonProperty("bidderId")
    @JsonAlias({ "buyerId", "buyer_id", "bidder_id", "userId", "user_id" })
    private Long bidderId;

    private String bidderName;

    @JsonProperty("amount")
    @JsonAlias({ "bidAmount", "bid_amount", "price" })
    private BigDecimal amount;

    private LocalDateTime bidTime;

    // Additional helper getters/setters for compatibility with bidAmount /
    // productId / buyerId
    public BigDecimal getBidAmount() {
        return amount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        if (this.amount == null) {
            this.amount = bidAmount;
        }
    }
}
