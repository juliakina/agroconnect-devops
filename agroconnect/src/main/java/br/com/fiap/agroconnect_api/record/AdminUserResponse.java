package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.model.UserRole;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(UUID id, String name, String email, UserRole role, boolean active, Instant createdAt) {
}
