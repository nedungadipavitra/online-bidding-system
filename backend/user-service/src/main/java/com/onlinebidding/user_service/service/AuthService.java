package com.onlinebidding.user_service.service;

import com.onlinebidding.user_service.dto.LoginRequest;
import com.onlinebidding.user_service.dto.LoginResponse;
import com.onlinebidding.user_service.dto.RefreshTokenRequest;
import com.onlinebidding.user_service.dto.RefreshTokenResponse;
import com.onlinebidding.user_service.dto.RegisterRequest;
import com.onlinebidding.user_service.dto.RegisterResponse;

public interface AuthService {
	RegisterResponse register(RegisterRequest request);
	LoginResponse login(LoginRequest request);
	RefreshTokenResponse refreshToken(RefreshTokenRequest request);
	void logout(String refreshToken);
}
