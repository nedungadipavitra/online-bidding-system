package com.onlinebidding.product_service.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.bind.annotation.RequestHeader;

import com.onlinebidding.product_service.dto.CategoryDto;
import com.onlinebidding.product_service.exception.AccessDeniedException;
import com.onlinebidding.product_service.service.CategoryService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/categories")
public class CategoryController {

	private final CategoryService categoryService;

	@Autowired
	public CategoryController(CategoryService categoryService) {
		this.categoryService = categoryService;
	}

	@GetMapping
	public ResponseEntity<List<CategoryDto>> getAllCategories() {
		return ResponseEntity.ok(categoryService.getAllCategories());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CategoryDto> getCategoryById(@PathVariable("id") Long id) {
		return ResponseEntity.ok(categoryService.getCategoryById(id));
	}

	@PostMapping
	public ResponseEntity<CategoryDto> addCategory(
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@RequestBody @Valid CategoryDto categoryDto) {
		if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
			throw new AccessDeniedException("Access denied. Only ADMIN can create categories.");
		}
		return new ResponseEntity<>(categoryService.addCategory(categoryDto), HttpStatus.CREATED);
	}

	@PutMapping("/{id}")
	public ResponseEntity<CategoryDto> updateCategory(
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable("id") Long id,
			@RequestBody @Valid CategoryDto categoryDto) {
		if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
			throw new AccessDeniedException("Access denied. Only ADMIN can update categories.");
		}
		return ResponseEntity.ok(categoryService.updateCategory(id, categoryDto));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<String> deleteCategoryById(
			@RequestHeader(value = "X-User-Role", required = false) String role,
			@PathVariable("id") Long id) {
		if (role == null || !"ADMIN".equalsIgnoreCase(role)) {
			throw new AccessDeniedException("Access denied. Only ADMIN can delete categories.");
		}
		categoryService.deleteCategoryById(id);
		return ResponseEntity.ok("Category has been deleted.");
	}
}
