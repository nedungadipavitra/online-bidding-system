package com.onlinebidding.user_service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinebidding.user_service.dto.LoginRequest;
import com.onlinebidding.user_service.dto.LoginResponse;
import com.onlinebidding.user_service.dto.LogoutRequest;
import com.onlinebidding.user_service.dto.RefreshTokenRequest;
import com.onlinebidding.user_service.dto.RefreshTokenResponse;
import com.onlinebidding.user_service.dto.RegisterRequest;
import com.onlinebidding.user_service.dto.RegisterResponse;
import com.onlinebidding.user_service.exception.EmailAlreadyExistsException;
import com.onlinebidding.user_service.exception.GlobalExceptionHandler;
import com.onlinebidding.user_service.exception.InvalidRefreshTokenException;
import com.onlinebidding.user_service.exception.RefreshTokenExpiredException;
import com.onlinebidding.user_service.exception.RefreshTokenRevokedException;
import com.onlinebidding.user_service.service.AuthService;

/**
 * Standalone MVC tests — no Spring context / JPA. Full {@code @SpringBootTest}
 * needs MySQL and fails in Jenkins with Communications link failure.
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AuthService authService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // --- REGISTER TESTS ---

    @Test
    void register_success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("BUYER");

        RegisterResponse response = new RegisterResponse("User registered successfully");
        when(authService.register(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User registered successfully"));
    }

    @Test
    void register_emailAlreadyExists_returns409() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName("John Doe");
        request.setEmail("john@example.com");
        request.setPassword("password123");
        request.setRole("BUYER");

        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new EmailAlreadyExistsException("Email already exists"));

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    void register_validationFailure_returns400() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setName(""); // Invalid: blank
        request.setEmail("invalid-email"); // Invalid: not email format
        request.setPassword("short"); // Invalid: less than 8 chars

        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.role").exists());
    }

    // --- LOGIN TESTS ---

    @Test
    void login_success() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("password123");

        LoginResponse response = LoginResponse.builder()
                .accessToken("access-token")
                .refreshToken("refresh-token")
                .tokenType("Bearer")
                .build();
        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_badCredentials_returns401() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("john@example.com");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Invalid email or password"));

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void login_validationFailure_returns400() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("");
        request.setPassword("");

        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // --- ME TESTS ---

    @Test
    void me_success() throws Exception {
        mockMvc.perform(get("/auth/me")
                .principal(new UsernamePasswordAuthenticationToken("john@example.com", "n/a", List.of())))
                .andExpect(status().isOk())
                .andExpect(content().string("john@example.com"));
    }

    // --- REFRESH TOKEN TESTS ---

    @Test
    void refreshToken_success() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");
        RefreshTokenResponse response = RefreshTokenResponse.builder()
                .accessToken("new-access-token")
                .tokenType("Bearer")
                .build();

        when(authService.refreshToken(any(RefreshTokenRequest.class))).thenReturn(response);

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void refreshToken_invalidToken_returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new InvalidRefreshTokenException("Invalid refresh token"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refreshToken_expiredToken_returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("expired-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RefreshTokenExpiredException("Refresh token was expired"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token was expired"));
    }

    @Test
    void refreshToken_revokedToken_returns401() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest("revoked-refresh-token");

        when(authService.refreshToken(any(RefreshTokenRequest.class)))
                .thenThrow(new RefreshTokenRevokedException("Refresh token was revoked"));

        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token was revoked"));
    }

    // --- LOGOUT TESTS ---

    @Test
    void logout_success() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("valid-refresh-token");

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("Logged out successfully"));
    }

    @Test
    void logout_validationFailure_returns400() throws Exception {
        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken(""); // Blank

        mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
