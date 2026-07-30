package com.onlinebidding.product_service.service;

import java.util.List;
import com.onlinebidding.product_service.dto.ProductDto;

public interface ProductService {
	List<ProductDto> getAllProducts();
	ProductDto getProductById(Long id);
	ProductDto addProduct(ProductDto newProduct);
	ProductDto updateProduct(ProductDto updatedProductDto);
	void deleteProductById(Long id);
}
