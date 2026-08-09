package com.onlinebidding.product_service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
	private final S3Client s3Client;
	@Value("${s3.bucket}")
	private String bucket;
	@Value("${s3.base-path}")
	private String basePath;
	@Value("${aws.region}")
	private String region;

	public String uploadFile(MultipartFile file, Long productId) {
		String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
		String safeName = originalName.replaceAll("[^a-zA-Z0-9._-]", "_");
		String normalizedBase = basePath == null ? "products" : basePath.replaceAll("^/+|/+$", "");
		String key = String.format("%s/%d/%s-%s", normalizedBase, productId, UUID.randomUUID(), safeName);
		try {
			PutObjectRequest request = PutObjectRequest.builder().bucket(bucket).key(key)
					.contentType(file.getContentType()).build();
			s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
			return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
		} catch (IOException e) {
			throw new RuntimeException("Failed to upload file to S3", e);
		} catch (RuntimeException e) {
			throw new RuntimeException("Failed to upload file to S3: " + e.getMessage(), e);
		}
	}
}