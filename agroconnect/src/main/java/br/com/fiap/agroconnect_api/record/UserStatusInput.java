package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.NotNull;

public record UserStatusInput(@NotNull Boolean active) {
}
