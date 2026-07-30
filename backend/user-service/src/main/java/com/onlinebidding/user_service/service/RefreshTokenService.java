package com.onlinebidding.user_service.service;

import com.onlinebidding.user_service.entity.RefreshToken;
import com.onlinebidding.user_service.entity.User;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken validateRefreshToken(String token);
    void revokeRefreshToken(String token);
    void revokeAllRefreshTokens(User user);
}