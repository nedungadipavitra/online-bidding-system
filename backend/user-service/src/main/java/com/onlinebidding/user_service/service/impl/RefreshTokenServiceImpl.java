package com.onlinebidding.user_service.service.impl;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.onlinebidding.user_service.entity.RefreshToken;
import com.onlinebidding.user_service.entity.User;
import com.onlinebidding.user_service.exception.InvalidRefreshTokenException;
import com.onlinebidding.user_service.exception.JwtAuthenticationException;
import com.onlinebidding.user_service.exception.RefreshTokenExpiredException;
import com.onlinebidding.user_service.exception.RefreshTokenRevokedException;
import com.onlinebidding.user_service.repository.RefreshTokenRepository;
import com.onlinebidding.user_service.security.JwtService;
import com.onlinebidding.user_service.service.RefreshTokenService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Override
    public RefreshToken createRefreshToken(User user) {

        String jwt = jwtService.generateRefreshToken(user);

        LocalDateTime expiryDate = LocalDateTime.now()
                .plusSeconds(jwtService.getRefreshTokenExpiration() / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(jwt)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Override
    public RefreshToken validateRefreshToken(String token) {

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() ->
                        new InvalidRefreshTokenException("Invalid refresh token"));

        checkRevoked(refreshToken);

        checkExpiry(refreshToken);

        validateJwt(refreshToken);

        return refreshToken;
    }

    @Override
    public void revokeRefreshToken(String token) {

        RefreshToken refreshToken = validateRefreshToken(token);

        refreshToken.setRevoked(true);

        refreshTokenRepository.save(refreshToken);
    }

    @Override
    public void revokeAllRefreshTokens(User user) {

        List<RefreshToken> tokens =
                refreshTokenRepository.findAllByUser(user);

        if (tokens.isEmpty()) {
            return;
        }

        tokens.forEach(token -> token.setRevoked(true));

        refreshTokenRepository.saveAll(tokens);
    }

    private void checkRevoked(RefreshToken token) {

        if (Boolean.TRUE.equals(token.getRevoked())) {
            throw new RefreshTokenRevokedException(
                    "Refresh token has been revoked");
        }
    }

    private void checkExpiry(RefreshToken token) {

        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            throw new RefreshTokenExpiredException(
                    "Refresh token has expired");
        }
    }

    private void validateJwt(RefreshToken token) {

        if (!jwtService.isTokenValid(
                token.getToken(),
                token.getUser())) {

            throw new JwtAuthenticationException(
                    "Invalid refresh token");
        }
    }
}