package com.onlinebidding.bid_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.dto.ProductSnapshot;
import com.onlinebidding.bid_service.entity.Bid;
import com.onlinebidding.bid_service.repository.BidRepository;
import com.onlinebidding.bid_service.service.impl.BidServiceImpl;

@ExtendWith(MockitoExtension.class)
class BidServiceImplTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private RestTemplate restTemplate;

    private BidServiceImpl bidService;

    @BeforeEach
    void setUp() {
        bidService = new BidServiceImpl(
                bidRepository,
                restTemplate,
                "http://product",
                "http://wallet",
                "local-development-only");
    }

    @Test
    void placeBid_verifiesAuctionAndUsesInternalWalletSettlement() {
        ProductSnapshot product = activeProduct(9L, 20L);
        when(restTemplate.getForObject("http://product/products/9", ProductSnapshot.class))
                .thenReturn(product);
        when(bidRepository.findFirstByAuctionIdOrderByAmountDesc(9L))
                .thenReturn(Optional.empty());
        when(restTemplate.postForEntity(
                eq("http://wallet/wallets/settle-bid"),
                any(HttpEntity.class),
                eq(Void.class)))
                .thenReturn(ResponseEntity.ok().<Void>build());
        when(bidRepository.save(any(Bid.class))).thenAnswer(invocation -> {
            Bid saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        BidDto result = bidService.placeBid(BidDto.builder()
                .auctionId(9L)
                .bidderId(11L)
                .bidderName("Buyer")
                .amount(new BigDecimal("75.00"))
                .build());

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getAuctionId()).isEqualTo(9L);
        assertThat(result.getAmount()).isEqualByComparingTo("75.00");

        ArgumentCaptor<HttpEntity> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(
                eq("http://wallet/wallets/settle-bid"), entityCaptor.capture(), eq(Void.class));
        HttpEntity<?> settlement = entityCaptor.getValue();
        assertThat(settlement.getHeaders().getFirst("X-Internal-Service-Token"))
                .isEqualTo("local-development-only");
        Map<?, ?> body = (Map<?, ?>) settlement.getBody();
        assertThat(body.get("bidderId")).isEqualTo(11L);
        assertThat(body.get("sellerId")).isEqualTo(20L);
        assertThat(body.get("newAmount")).isEqualTo(new BigDecimal("75.00"));
    }

    @Test
    void placeBid_rejectsSellerBiddingOnOwnAuctionBeforeSettlement() {
        when(restTemplate.getForObject("http://product/products/9", ProductSnapshot.class))
                .thenReturn(activeProduct(9L, 11L));

        assertThatThrownBy(() -> bidService.placeBid(BidDto.builder()
                .auctionId(9L)
                .bidderId(11L)
                .amount(new BigDecimal("75.00"))
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A seller cannot bid on their own auction");

        verify(bidRepository, never()).findFirstByAuctionIdOrderByAmountDesc(9L);
        verify(restTemplate, never()).postForEntity(any(), any(), eq(Void.class));
    }

    @Test
    void placeBid_rejectsBeforeAuctionStartInIndiaZone() {
        ProductSnapshot product = activeProduct(9L, 20L);
        product.setAuctionStartTime(LocalDateTime.now(AUCTION_ZONE).plusHours(2));
        when(restTemplate.getForObject("http://product/products/9", ProductSnapshot.class))
                .thenReturn(product);

        assertThatThrownBy(() -> bidService.placeBid(BidDto.builder()
                .auctionId(9L)
                .bidderId(11L)
                .amount(new BigDecimal("75.00"))
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bidding has not started yet");

        verify(restTemplate, never()).postForEntity(any(), any(), eq(Void.class));
    }

    @Test
    void getHighestBidsForAuctions_returnsOneHighestBidPerAuction() {
        Bid auctionOneHighest = bid(1L, 90L, "90.00");
        Bid auctionTwoHighest = bid(2L, 70L, "70.00");
        Bid auctionTwoLower = bid(2L, 71L, "60.00");
        when(bidRepository.findByAuctionIdInOrderByAuctionIdAscAmountDesc(List.of(1L, 2L)))
                .thenReturn(List.of(auctionOneHighest, auctionTwoHighest, auctionTwoLower));

        Map<Long, BidDto> highest = bidService.getHighestBidsForAuctions(List.of(1L, 2L));

        assertThat(highest).containsOnlyKeys(1L, 2L);
        assertThat(highest.get(1L).getAmount()).isEqualByComparingTo("90.00");
        assertThat(highest.get(2L).getAmount()).isEqualByComparingTo("70.00");
    }

    private static final ZoneId AUCTION_ZONE = ZoneId.of("Asia/Kolkata");

    private ProductSnapshot activeProduct(Long productId, Long sellerId) {
        ProductSnapshot product = new ProductSnapshot();
        product.setProductId(productId);
        product.setSellerId(sellerId);
        product.setStatus("ACTIVE");
        product.setAuctionStartTime(LocalDateTime.now(AUCTION_ZONE).minusMinutes(5));
        product.setAuctionEndTime(LocalDateTime.now(AUCTION_ZONE).plusHours(1));
        return product;
    }

    private Bid bid(Long auctionId, Long bidderId, String amount) {
        return Bid.builder()
                .id(auctionId * 10)
                .auctionId(auctionId)
                .bidderId(bidderId)
                .bidderName("Buyer " + bidderId)
                .amount(new BigDecimal(amount))
                .bidTime(LocalDateTime.now(AUCTION_ZONE))
                .build();
    }
}
