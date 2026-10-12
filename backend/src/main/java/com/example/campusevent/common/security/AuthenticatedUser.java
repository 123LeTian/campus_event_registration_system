package com.example.campusevent.common.security;

public record AuthenticatedUser(Long userId, String username, String role) {

    public String authority() {
        return "ROLE_" + role;
    }
}
