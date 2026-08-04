package com.onlinebidding.bid_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.onlinebidding.bid_service.dto.BidDto;
import com.onlinebidding.bid_service.service.BidService;

@ExtendWith(MockitoExtension.class)
class BidControllerSecurityTest {

    @Mock
    private BidService bidService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    private BidController controller;

    @BeforeEach
    void setUp() {
        controller = new BidController(bidService, messagingTemplate);
    }

    @Test
    void placeBid_rejectsBidderIdDifferentFromAuthenticatedUser() {
        BidDto request = BidDto.builder()
                .auctionId(9L)
                .bidderId(20L)
                .amount(new BigDecimal("75.00"))
                .build();

        ResponseEntity<?> response = controller.placeBid("BUYER", "10", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(bidService, never()).placeBid(request);
    }

    @Test
    void getHighestBids_requiresAuthenticatedIdentity() {
        ResponseEntity<?> response = controller.getHighestBidsForAuctions(null, "1,2");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(bidService, never()).getHighestBidsForAuctions(org.mockito.ArgumentMatchers.anyList());
    }

    @Test
    void getHighestBids_deduplicatesAuctionIdsBeforeCallingService() {
        when(bidService.getHighestBidsForAuctions(List.of(1L, 2L))).thenReturn(Map.of());

        ResponseEntity<Map<Long, BidDto>> response = controller.getHighestBidsForAuctions("10", "1, 2, 1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(bidService).getHighestBidsForAuctions(List.of(1L, 2L));
    }
}
