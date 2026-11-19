package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(@NotBlank @Email @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") @Size(max = 254) String email, @NotBlank @Size(max = 128) String password) {

	public LoginRequest {
		email = email == null ? null : email.trim();
	}
}
