package br.com.fiap.agroconnect_api.record;

public record AuthResponse(String accessToken, String tokenType, int expiresIn, UserResponse user) {
}
