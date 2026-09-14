package main.dto;

public record AuthResponse(String accessToken, String refreshToken, long expiresIn
) {}