package com.onlinebidding.product_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

import com.onlinebidding.product_service.dto.ProductDto;
import com.onlinebidding.product_service.exception.AccessDeniedException;
import com.onlinebidding.product_service.service.ProductService;

import jakarta.validation.Valid;

import java.util.Objects;

@RestController
@RequestMapping("/products")
public class ProductController {

	private final ProductService productService;

	@Autowired
	public ProductController(ProductService productService) {
		this.productService = productService;
	}

	@GetMapping
	public ResponseEntity<List<ProductDto>> getAllProducts() {
		return ResponseEntity.ok(productService.getAllProducts());
	}

	@GetMapping("/{productId}")
	public ResponseEntity<ProductDto> getProductById(@PathVariable("productId") Long id) {
		return ResponseEntity.ok(productService.getProductById(id));
	}

	@PostMapping
    public ResponseEntity<ProductDto> addProduct(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody @Valid ProductDto newProduct) {
        if (role == null || !"SELLER".equalsIgnoreCase(role) || userId == null) {
            throw new AccessDeniedException("Access denied. Only SELLER can create products.");
        }
        // The authenticated gateway identity is authoritative; do not trust a sellerId
        // supplied by the browser.
        newProduct.setSellerId(userId);
        return new ResponseEntity<>(productService.addProduct(newProduct), HttpStatus.CREATED);
	}

	@PatchMapping
    public ResponseEntity<ProductDto> updateProduct(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestBody @Valid ProductDto updatedProduct) {
        if (updatedProduct.getProductId() == null) {
            throw new IllegalArgumentException("Product ID is required for updates.");
        }
        ProductDto existingProduct = productService.getProductById(updatedProduct.getProductId());
        boolean canUpdate = "ADMIN".equalsIgnoreCase(role)
                || ("SELLER".equalsIgnoreCase(role)
                && userId != null
                && Objects.equals(userId, existingProduct.getSellerId()));
        if (!canUpdate) {
            throw new AccessDeniedException("Access denied. Only SELLER or ADMIN can update products.");
        }
		// The current highest bid is owned by the bidding workflow, not by product
		// administration. Ignore any client-supplied value during product edits.
		updatedProduct.setCurrentHighestBid(existingProduct.getCurrentHighestBid());
		return ResponseEntity.ok(productService.updateProduct(updatedProduct));
	}

	@DeleteMapping("/{productId}")
    public ResponseEntity<String> deleteProductById(
            @RequestHeader(value = "X-User-Role", required = false) String role,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable("productId") Long id) {
        ProductDto existingProduct = productService.getProductById(id);
        boolean canDelete = "ADMIN".equalsIgnoreCase(role)
                || ("SELLER".equalsIgnoreCase(role)
                && userId != null
                && Objects.equals(userId, existingProduct.getSellerId()));
        if (!canDelete) {
            throw new AccessDeniedException("Access denied. Only SELLER or ADMIN can delete products.");
        }
		productService.deleteProductById(id);
		return ResponseEntity.ok("Product has been deleted.");
	}
}
