package com.onlinebidding.user_service.service.impl;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import org.springframework.web.client.RestTemplate;
import com.onlinebidding.user_service.dto.LoginRequest;
import com.onlinebidding.user_service.dto.LoginResponse;
import com.onlinebidding.user_service.dto.RefreshTokenRequest;
import com.onlinebidding.user_service.dto.RefreshTokenResponse;
import com.onlinebidding.user_service.dto.RegisterRequest;
import com.onlinebidding.user_service.dto.RegisterResponse;
import com.onlinebidding.user_service.entity.RefreshToken;
import com.onlinebidding.user_service.entity.Role;
import com.onlinebidding.user_service.entity.User;
import com.onlinebidding.user_service.exception.EmailAlreadyExistsException;
import com.onlinebidding.user_service.repository.UserRepository;
import com.onlinebidding.user_service.security.CustomUserDetails;
import com.onlinebidding.user_service.security.JwtService;
import com.onlinebidding.user_service.service.AuthService;
import com.onlinebidding.user_service.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RestTemplate restTemplate;

    @Override
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        // Parse and validate role — only BUYER and SELLER can self-register
        Role role;
        try {
            role = Role.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }

        if (role == Role.ADMIN) {
            throw new IllegalArgumentException("Cannot register as ADMIN");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        if (role == Role.BUYER) {
            try {
                String walletServiceUrl = "http://localhost:8083/wallets/create";
                Map<String, Object> walletRequest = Map.of(
                        "userId", savedUser.getId(),
                        "initialBalance", BigDecimal.ZERO
                );
                restTemplate.postForObject(walletServiceUrl, walletRequest, Object.class);
            } catch (Exception e) {
                System.err.println("Failed to auto-create wallet: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return new RegisterResponse("User registered successfully");
    }

    @Override
    public LoginResponse login(LoginRequest request) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()));

        CustomUserDetails userDetails =
                (CustomUserDetails) authentication.getPrincipal();

        User user = userDetails.getUser();

        String accessToken = jwtService.generateAccessToken(user);

        RefreshToken refreshToken =
                refreshTokenService.createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .build();
    }
    
    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken refreshToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken());

        User user = refreshToken.getUser();

        String accessToken =
                jwtService.generateAccessToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .build();
    }
    
    @Override
    public void logout(String refreshToken) {
        refreshTokenService.revokeRefreshToken(refreshToken);
    }
}