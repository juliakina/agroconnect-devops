package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserInput(@NotBlank @Size(min = 2, max = 150) String name, @NotBlank @Email @Pattern(regexp = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") @Size(max = 254) String email) {

	public UserInput {
		name = name == null ? null : name.trim();
		email = email == null ? null : email.trim();
	}
}
