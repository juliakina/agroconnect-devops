package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.model.UserRole;
import jakarta.validation.constraints.NotNull;

public record UserRoleInput(@NotNull UserRole role) {
}
