package com.onlinebidding.user_service.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.onlinebidding.user_service.entity.Role;
import com.onlinebidding.user_service.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class DataInitializerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private DataInitializer dataInitializer;

    @BeforeEach
    void setUp() {
        dataInitializer = new DataInitializer();
    }

    @Test
    void seedAdminUser_whenAdminDoesNotExist_seedsAdmin() throws Exception {
        when(userRepository.existsByEmail("admin@mail.com")).thenReturn(false);
        when(passwordEncoder.encode("admin123")).thenReturn("encodedAdmin123");

        CommandLineRunner runner = dataInitializer.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository).save(argThat(user ->
                "Admin".equals(user.getName()) &&
                "admin@mail.com".equals(user.getEmail()) &&
                "encodedAdmin123".equals(user.getPassword()) &&
                Role.ADMIN.equals(user.getRole()) &&
                Boolean.TRUE.equals(user.getEnabled())
        ));
    }

    @Test
    void seedAdminUser_whenAdminAlreadyExists_doesNotSeedAdmin() throws Exception {
        when(userRepository.existsByEmail("admin@mail.com")).thenReturn(true);

        CommandLineRunner runner = dataInitializer.seedAdminUser(userRepository, passwordEncoder);
        runner.run();

        verify(userRepository, never()).save(any());
    }
}
