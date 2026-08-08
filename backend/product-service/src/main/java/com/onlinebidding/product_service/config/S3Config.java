package com.onlinebidding.product_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
//reads the region from parameter store, /obs/prod/aws/region
@Configuration
public class S3Config {
	@Value("${aws.region}")
	private String region;

	@Bean
	public S3Client s3Client() {
		return S3Client.builder().region(Region.of(region)).build();
	}
}
