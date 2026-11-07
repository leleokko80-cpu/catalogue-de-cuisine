package com.recipecatalog.backend.auth;

public record AuthResponse(String token, UserResponse user) {
}
