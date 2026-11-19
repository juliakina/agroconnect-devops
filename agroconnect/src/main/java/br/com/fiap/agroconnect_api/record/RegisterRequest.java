package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(@NotBlank @Size(min = 2, max = 150) String name, @NotBlank @Email @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") @Size(max = 254) String email, @NotBlank @Size(min = 8, max = 128) String password) {

	public RegisterRequest {
		name = name == null ? null : name.trim();
		email = email == null ? null : email.trim();
	}
}
