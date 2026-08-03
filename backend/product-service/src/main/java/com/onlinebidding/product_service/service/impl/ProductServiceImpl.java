package com.onlinebidding.product_service.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.onlinebidding.product_service.dto.ProductDto;
import com.onlinebidding.product_service.entity.Product;
import com.onlinebidding.product_service.entity.ProductStatus;
import com.onlinebidding.product_service.exception.ResourceNotFoundException;
import com.onlinebidding.product_service.mapper.ProductMapper;
import com.onlinebidding.product_service.repository.ProductRepository;
import com.onlinebidding.product_service.service.ProductService;
import com.onlinebidding.product_service.service.S3Service;

@Service
@Transactional
public class ProductServiceImpl implements ProductService {
	
	@Autowired
	private ProductRepository productRepo;
	@Autowired
	private ProductMapper mapper;
	@Autowired
	private S3Service s3Service;

//	@Autowired
//	public ProductServiceImpl(ProductRepository productRepo, ProductMapper mapper) {
//		this.productRepo = productRepo;
//		this.mapper = mapper;
//	}

	@Override
	@Transactional(readOnly = true)
	public List<ProductDto> getAllProducts() {
		return productRepo.findAll().stream().map(mapper::toDto).toList();
	}

	@Override
	@Transactional(readOnly = true)
	public ProductDto getProductById(Long id) {
		Product product = productRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		return mapper.toDto(product);
	}

	@Override
	public ProductDto addProduct(ProductDto newProduct) {
		Product prd = mapper.toEntity(newProduct);
		if (prd.getStatus() == null) {
			prd.setStatus(ProductStatus.ACTIVE);
		}
		Product saved = productRepo.save(prd);
		return mapper.toDto(saved);
	}

	@Override
	public ProductDto updateProduct(ProductDto updatedProductDto) {
		Product prd = productRepo.findById(updatedProductDto.getProductId())
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + updatedProductDto.getProductId()));
		mapper.updateProduct(updatedProductDto, prd);
		return mapper.toDto(productRepo.save(prd));
	}

	@Override
	public void deleteProductById(Long id) {
		Product prd = productRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		productRepo.delete(prd);
	}
	
	@Override
	public String uploadImage(Long id, MultipartFile file) {
		Product product = productRepo.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
		String imageUrl = s3Service.uploadFile(file, id);
		product.setImageUrl(imageUrl);
		productRepo.save(product);
		return imageUrl;

	}
}
