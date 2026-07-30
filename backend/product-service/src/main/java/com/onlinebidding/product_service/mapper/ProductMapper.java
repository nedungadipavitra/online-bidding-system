package com.onlinebidding.product_service.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import com.onlinebidding.product_service.dto.ProductDto;
import com.onlinebidding.product_service.entity.Product;

@Mapper(componentModel = "spring")
public interface ProductMapper {
	ProductDto toDto(Product product);
	Product toEntity(ProductDto dto);
	@BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
	void updateProduct(ProductDto dto, @MappingTarget Product product);
}
