package com.onlinebidding.wallet_service.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.onlinebidding.wallet_service.dto.BidSettlementRequest;
import com.onlinebidding.wallet_service.service.WalletService;

@ExtendWith(MockitoExtension.class)
class WalletControllerSecurityTest {

    @Mock
    private WalletService walletService;

    @Test
    void settleBid_rejectsRequestsWithoutInternalServiceToken() {
        WalletController controller = new WalletController(walletService, "expected-token");
        BidSettlementRequest request = new BidSettlementRequest(
                1L, 2L, new BigDecimal("50.00"), null, BigDecimal.ZERO);

        ResponseStatusException exception = catchThrowableOfType(
                () -> controller.settleBid("client-token", request),
                ResponseStatusException.class);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(walletService, never()).settleBid(request);
    }

    @Test
    void getWalletByUserId_rejectsAnotherUsersWallet() {
        WalletController controller = new WalletController(walletService, "expected-token");

        ResponseStatusException exception = catchThrowableOfType(
                () -> controller.getWalletByUserId("10", "BUYER", 20L),
                ResponseStatusException.class);

        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(walletService, never()).getWalletByUserId(20L);
    }
}
