package com.onlinebidding.wallet_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.onlinebidding.wallet_service.dto.WalletDto;
import com.onlinebidding.wallet_service.entity.Wallet;

@Mapper(componentModel = "spring")
public interface WalletMapper {
	WalletDto toDto(Wallet wallet);
	Wallet toEntity(WalletDto dto);
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateWallet(WalletDto dto, @MappingTarget Wallet wallet);
}
