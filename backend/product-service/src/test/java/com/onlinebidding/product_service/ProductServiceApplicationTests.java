package com.onlinebidding.product_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke-load application context.
 * Must not require live AWS Parameter Store or RDS — see src/test/resources/application.properties (H2).
 * Production still loads SSM via application-prod.yml when profile=prod.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ProductServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
