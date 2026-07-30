package com.onlinebidding.user_service.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.onlinebidding.user_service.entity.Role;
import com.onlinebidding.user_service.entity.User;
import com.onlinebidding.user_service.repository.UserRepository;

@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner seedAdminUser(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            String adminEmail = "admin@mail.com";
            if (!userRepository.existsByEmail(adminEmail)) {
                User admin = User.builder()
                        .name("Admin")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin123"))
                        .role(Role.ADMIN)
                        .enabled(true)
                        .build();

                userRepository.save(admin);
                logger.info("Admin user successfully pre-seeded into the database: {}", adminEmail);
            } else {
                logger.info("Admin user {} already exists. Skipping pre-seeding.", adminEmail);
            }
        };
    }
}
