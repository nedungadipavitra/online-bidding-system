package com.onlinebidding.product_service.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import com.onlinebidding.product_service.dto.ProductDto;
import com.onlinebidding.product_service.exception.AccessDeniedException;
import com.onlinebidding.product_service.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerSecurityTest {

    @Mock
    private ProductService productService;

    private ProductController productController;

    @BeforeEach
    void setUp() {
        productController = new ProductController(productService);
    }

    @Test
    void updateProduct_rejectsSellerWhoDoesNotOwnProduct() {
        when(productService.getProductById(1L)).thenReturn(product(1L, 42L));

        assertThatThrownBy(() -> productController.updateProduct(
                "SELLER", 99L, product(1L, 99L)))
                .isInstanceOf(AccessDeniedException.class);

        verify(productService, never()).updateProduct(product(1L, 99L));
    }

    @Test
    void deleteProduct_rejectsSellerWhoDoesNotOwnProduct() {
        when(productService.getProductById(1L)).thenReturn(product(1L, 42L));

        assertThatThrownBy(() -> productController.deleteProductById("SELLER", 99L, 1L))
                .isInstanceOf(AccessDeniedException.class);

        verify(productService, never()).deleteProductById(1L);
    }

    @Test
    void updateProduct_preservesCurrentHighestBid() {
        ProductDto existing = product(1L, 42L);
        existing.setCurrentHighestBid(new BigDecimal("250.00"));
        ProductDto request = product(1L, 42L);
        request.setCurrentHighestBid(new BigDecimal("999.00"));
        when(productService.getProductById(1L)).thenReturn(existing);
        when(productService.updateProduct(org.mockito.ArgumentMatchers.any(ProductDto.class)))
                .thenReturn(existing);

        productController.updateProduct("ADMIN", 7L, request);

        ArgumentCaptor<ProductDto> captor = ArgumentCaptor.forClass(ProductDto.class);
        verify(productService).updateProduct(captor.capture());
        assertThat(captor.getValue().getCurrentHighestBid())
                .isEqualByComparingTo("250.00");
    }

    private ProductDto product(Long productId, Long sellerId) {
        return ProductDto.builder()
                .productId(productId)
                .sellerId(sellerId)
                .name("Vintage Watch")
                .basePrice(new BigDecimal("100.00"))
                .auctionEndTime(LocalDateTime.now().plusDays(1))
                .build();
    }
}
