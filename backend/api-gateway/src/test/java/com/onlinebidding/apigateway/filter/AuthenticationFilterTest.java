package com.onlinebidding.apigateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthenticationFilterTest {

    @Autowired
    private ApplicationContext context;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToApplicationContext(context).build();
    }

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    private String generateToken(Long userId, String role, String email, long expirationMs) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("role", role);

        return Jwts.builder()
                .claims(claims)
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    @Test
    void whenRequestToSecuredRouteWithoutToken_returns401() {
        webTestClient.get()
                .uri("/products/1")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenRequestToSecuredRouteWithInvalidTokenFormat_returns401() {
        webTestClient.get()
                .uri("/products/1")
                .header(HttpHeaders.AUTHORIZATION, "InvalidFormat")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenRequestToSecuredRouteWithExpiredToken_returns401() {
        String expiredToken = generateToken(1L, "USER", "user@example.com", -1000L);

        webTestClient.get()
                .uri("/products/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenRequestToSecuredRouteWithValidToken_passesFilter() {
        String validToken = generateToken(1L, "USER", "user@example.com", 60000L);

        webTestClient.get()
                .uri("/products/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + validToken)
                .exchange()
                .expectStatus().value(status -> {
                    assertThat(status).isNotEqualTo(401);
                });
    }

    @Test
    void whenRequestToPublicRouteWithoutToken_passesFilter() {
        webTestClient.post()
                .uri("/auth/login")
                .exchange()
                .expectStatus().value(status -> {
                    assertThat(status).isNotEqualTo(401);
                });
    }

    @Test
    void whenRequestToLogoutWithoutToken_passesFilter() {
        webTestClient.post()
                .uri("/auth/logout")
                .exchange()
                .expectStatus().value(status -> {
                    assertThat(status).isNotEqualTo(401);
                });
    }

    @Test
    void whenRequestToSecuredRouteWithTokenMissingClaims_returns401() {
        String token = generateToken(null, "USER", "user@example.com", 60000L);

        webTestClient.get()
                .uri("/products/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void whenRequestToSecuredRouteWithInvalidSignatureToken_returns401() {
        byte[] bytes = new byte[32];
        SecretKey key = Keys.hmacShaKeyFor(bytes);
        String token = Jwts.builder()
                .claim("userId", 1L)
                .claim("role", "USER")
                .subject("user@example.com")
                .signWith(key)
                .compact();

        webTestClient.get()
                .uri("/products/1")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
