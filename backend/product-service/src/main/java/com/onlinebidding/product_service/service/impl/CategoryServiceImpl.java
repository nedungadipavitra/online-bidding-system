package com.onlinebidding.product_service.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.onlinebidding.product_service.dto.CategoryDto;
import com.onlinebidding.product_service.entity.Category;
import com.onlinebidding.product_service.exception.ResourceNotFoundException;
import com.onlinebidding.product_service.repository.CategoryRepository;
import com.onlinebidding.product_service.service.CategoryService;

@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

	private final CategoryRepository categoryRepo;

	@Autowired
	public CategoryServiceImpl(CategoryRepository categoryRepo) {
		this.categoryRepo = categoryRepo;
	}

	@Override
	@Transactional(readOnly = true)
	public List<CategoryDto> getAllCategories() {
		return categoryRepo.findAll().stream()
				.map(cat -> CategoryDto.builder()
						.id(cat.getId())
						.name(cat.getName())
						.description(cat.getDescription())
						.build())
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public CategoryDto getCategoryById(Long id) {
		Category cat = categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
		return CategoryDto.builder()
				.id(cat.getId())
				.name(cat.getName())
				.description(cat.getDescription())
				.build();
	}

	@Override
	public CategoryDto addCategory(CategoryDto categoryDto) {
		Category cat = Category.builder()
				.name(categoryDto.getName())
				.description(categoryDto.getDescription())
				.build();
		Category saved = categoryRepo.save(cat);
		return CategoryDto.builder()
				.id(saved.getId())
				.name(saved.getName())
				.description(saved.getDescription())
				.build();
	}

	@Override
	public CategoryDto updateCategory(Long id, CategoryDto categoryDto) {
		Category cat = categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
		if (categoryDto.getName() != null) cat.setName(categoryDto.getName());
		if (categoryDto.getDescription() != null) cat.setDescription(categoryDto.getDescription());
		
		Category saved = categoryRepo.save(cat);
		return CategoryDto.builder()
				.id(saved.getId())
				.name(saved.getName())
				.description(saved.getDescription())
				.build();
	}

	@Override
	public void deleteCategoryById(Long id) {
		Category cat = categoryRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
		categoryRepo.delete(cat);
	}
}
