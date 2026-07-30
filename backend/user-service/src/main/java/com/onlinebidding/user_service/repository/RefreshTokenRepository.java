package com.onlinebidding.user_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.onlinebidding.user_service.entity.RefreshToken;
import com.onlinebidding.user_service.entity.User;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
	Optional<RefreshToken> findByToken(String token);
	List<RefreshToken> findAllByUser(User user);
	void deleteByUser(User user);
}