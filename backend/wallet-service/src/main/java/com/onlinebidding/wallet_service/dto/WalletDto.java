package com.onlinebidding.wallet_service.dto;

import java.math.BigDecimal;
import com.onlinebidding.wallet_service.entity.WalletStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletDto {
	private Long walletId;
	private Long userId;
	private WalletStatus status;
	private BigDecimal balance;
}
