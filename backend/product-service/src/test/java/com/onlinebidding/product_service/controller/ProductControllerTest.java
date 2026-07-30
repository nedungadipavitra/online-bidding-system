package com.onlinebidding.product_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.onlinebidding.product_service.dto.ProductDto;
import com.onlinebidding.product_service.exception.GlobalExceptionHandler;
import com.onlinebidding.product_service.service.ProductService;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(productController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void addProduct_withSellerRole_returns201Created() throws Exception {
        ProductDto responseDto = ProductDto.builder()
                .productId(1L)
                .name("Vintage Watch")
                .description("Rare collectible watch")
                .basePrice(new BigDecimal("100.00"))
                .build();

        when(productService.addProduct(any(ProductDto.class))).thenReturn(responseDto);

        String jsonPayload = """
                {
                    "name": "Vintage Watch",
                    "description": "Rare collectible watch",
                    "basePrice": 100.00,
                    "auctionEndTime": "2030-12-31T23:59:59"
                }
                """;

        mockMvc.perform(post("/products")
                .header("X-User-Role", "SELLER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(1))
                .andExpect(jsonPath("$.name").value("Vintage Watch"));
    }

    @Test
    void addProduct_withUserRole_returns403Forbidden() throws Exception {
        String jsonPayload = """
                {
                    "name": "Vintage Watch",
                    "description": "Rare collectible watch",
                    "basePrice": 100.00,
                    "auctionEndTime": "2030-12-31T23:59:59"
                }
                """;

        mockMvc.perform(post("/products")
                .header("X-User-Role", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only SELLER can create products."));
    }

    @Test
    void addProduct_withoutRoleHeader_returns403Forbidden() throws Exception {
        String jsonPayload = """
                {
                    "name": "Vintage Watch",
                    "description": "Rare collectible watch",
                    "basePrice": 100.00,
                    "auctionEndTime": "2030-12-31T23:59:59"
                }
                """;

        mockMvc.perform(post("/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied. Only SELLER can create products."));
    }
}
