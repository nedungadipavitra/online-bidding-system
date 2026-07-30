package com.onlinebidding.product_service.service;

import java.util.List;
import com.onlinebidding.product_service.dto.CategoryDto;

public interface CategoryService {
	List<CategoryDto> getAllCategories();
	CategoryDto getCategoryById(Long id);
	CategoryDto addCategory(CategoryDto categoryDto);
	CategoryDto updateCategory(Long id, CategoryDto categoryDto);
	void deleteCategoryById(Long id);
}
