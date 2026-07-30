package com.onlinebidding.user_service.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import com.onlinebidding.user_service.entity.Role;
import com.onlinebidding.user_service.entity.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {
	
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;
    
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    private Claims extractAllClaims(String token) {
    	return Jwts.parser()
    			.verifyWith(getSigningKey())
    			.build()
    			.parseSignedClaims(token)
    			.getPayload();
    }
    
    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
    	Claims claims = extractAllClaims(token);
    	return resolver.apply(claims);
    }
    
    
    private String buildToken(
            Map<String, Object> extraClaims,
            User user,
            long expiration
    ) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();

    }
    
    public String generateAccessToken(User user) {
    	Map<String, Object> claims = new HashMap<>();
    	claims.put("userId", user.getId());
    	claims.put("role", user.getRole().name());
    	
    	return buildToken(claims, user, accessTokenExpiration);
    }
    
    public String generateRefreshToken(User user) {
    	return buildToken(new HashMap<>(), user, refreshTokenExpiration);
    }
    
    public String extractUsername(String token) {
    	return extractClaim(token, Claims::getSubject);
    }
    
    public Long extractUserId(String token) {
    	return extractClaim(token, 
    			claims -> claims.get("userId", Long.class));
    }
    
    public Role extractRole(String token) {
    	String role = extractClaim(token, 
    			claims -> claims.get("role", String.class));
    	return Role.valueOf(role);
    }
    
    public boolean isTokenExpired(String token) {
    	Date expiry = extractClaim(token, Claims::getExpiration);
    	return expiry.before(new Date());
    }
    
    public boolean isTokenValid(String token, UserDetails userDetails) {
    	String email = extractUsername(token);
    	return email.equals(userDetails.getUsername()) && !isTokenExpired(token); 
    }
    
    public boolean isTokenValid(String token, User user) {
        String email = extractUsername(token);
        return email.equals(user.getEmail()) && !isTokenExpired(token);
    }
    
    public long getRefreshTokenExpiration() {
        return refreshTokenExpiration;
    }
}
