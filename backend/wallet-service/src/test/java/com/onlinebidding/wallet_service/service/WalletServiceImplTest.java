package com.onlinebidding.wallet_service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlinebidding.wallet_service.dto.BidSettlementRequest;
import com.onlinebidding.wallet_service.dto.WalletDto;
import com.onlinebidding.wallet_service.entity.Wallet;
import com.onlinebidding.wallet_service.entity.WalletStatus;
import com.onlinebidding.wallet_service.exception.InsufficientBalanceException;
import com.onlinebidding.wallet_service.mapper.WalletMapper;
import com.onlinebidding.wallet_service.repository.TransactionRepository;
import com.onlinebidding.wallet_service.repository.WalletRepository;
import com.onlinebidding.wallet_service.service.impl.WalletServiceImpl;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private TransactionRepository transactionRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void settleBid_debitsBidderAndCreditsSellerAtomically() {
        Wallet bidder = wallet(1L, "100.00");
        Wallet seller = wallet(2L, "25.00");
        stubWallets(bidder, seller);
        when(walletMapper.toDto(bidder)).thenReturn(WalletDto.builder()
                .userId(1L)
                .balance(new BigDecimal("40.00"))
                .build());

        WalletDto result = walletService.settleBid(new BidSettlementRequest(
                1L, 2L, new BigDecimal("60.00"), null, BigDecimal.ZERO));

        assertThat(bidder.getBalance()).isEqualByComparingTo("40.00");
        assertThat(seller.getBalance()).isEqualByComparingTo("85.00");
        assertThat(result.getBalance()).isEqualByComparingTo("40.00");
        verify(transactionRepository, org.mockito.Mockito.times(2)).save(any());
        verify(walletRepository).saveAll(any());
    }

    @Test
    void settleBid_refundsPreviousBidderAndRemovesPreviousAmountFromSeller() {
        Wallet bidder = wallet(1L, "100.00");
        Wallet seller = wallet(2L, "100.00");
        Wallet previousBidder = wallet(3L, "20.00");
        stubWallets(bidder, seller, previousBidder);
        when(walletMapper.toDto(bidder)).thenReturn(WalletDto.builder()
                .userId(1L)
                .balance(new BigDecimal("50.00"))
                .build());

        walletService.settleBid(new BidSettlementRequest(
                1L, 2L, new BigDecimal("50.00"), 3L, new BigDecimal("20.00")));

        assertThat(bidder.getBalance()).isEqualByComparingTo("50.00");
        assertThat(seller.getBalance()).isEqualByComparingTo("130.00");
        assertThat(previousBidder.getBalance()).isEqualByComparingTo("40.00");
        verify(transactionRepository, org.mockito.Mockito.times(4)).save(any());
    }

    @Test
    void settleBid_rejectsInsufficientBalanceWithoutPersistingChanges() {
        Wallet bidder = wallet(1L, "10.00");
        Wallet seller = wallet(2L, "25.00");
        stubWallets(bidder, seller);

        assertThatThrownBy(() -> walletService.settleBid(new BidSettlementRequest(
                1L, 2L, new BigDecimal("60.00"), null, BigDecimal.ZERO)))
                .isInstanceOf(InsufficientBalanceException.class);

        assertThat(bidder.getBalance()).isEqualByComparingTo("10.00");
        assertThat(seller.getBalance()).isEqualByComparingTo("25.00");
        verify(transactionRepository, never()).save(any());
        verify(walletRepository, never()).saveAll(any());
    }

    @Test
    void settleBid_rejectsSellerBiddingOnOwnAuction() {
        assertThatThrownBy(() -> walletService.settleBid(new BidSettlementRequest(
                2L, 2L, new BigDecimal("60.00"), null, BigDecimal.ZERO)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A seller cannot bid on their own auction");

        verify(walletRepository, never()).findByUserIdForUpdate(any());
    }

    private void stubWallets(Wallet... wallets) {
        for (Wallet wallet : wallets) {
            when(walletRepository.findByUserIdForUpdate(wallet.getUserId()))
                    .thenReturn(Optional.of(wallet));
        }
    }

    private Wallet wallet(Long userId, String balance) {
        return Wallet.builder()
                .userId(userId)
                .balance(new BigDecimal(balance))
                .status(WalletStatus.ACTIVE)
                .build();
    }
}
