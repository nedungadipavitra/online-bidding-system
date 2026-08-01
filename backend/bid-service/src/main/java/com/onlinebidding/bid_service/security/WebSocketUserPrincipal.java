package com.onlinebidding.bid_service.security;

import java.security.Principal;

public final class WebSocketUserPrincipal implements Principal {

    private final Long userId;
    private final String role;
    private final String email;

    public WebSocketUserPrincipal(Long userId, String role, String email) {
        this.userId = userId;
        this.role = role;
        this.email = email;
    }

    @Override
    public String getName() {
        return String.valueOf(userId);
    }

    public Long getUserId() {
        return userId;
    }

    public String getRole() {
        return role;
    }

    public String getEmail() {
        return email;
    }
}
