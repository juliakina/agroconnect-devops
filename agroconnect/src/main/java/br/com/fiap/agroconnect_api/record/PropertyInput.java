package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import br.com.fiap.agroconnect_api.validation.DecimalPrecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record PropertyInput(@NotBlank @Size(min = 2, max = 150) String name,
		@NotBlank @Size(min = 2, max = 120) String city,
		@NotBlank @Pattern(regexp = "[a-zA-Z]{2}") String state,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 29, fraction = 28) @DecimalPrecision BigDecimal areaHectares,
		@Size(max = 2000) String description) {

	public PropertyInput {
		name = name == null ? null : name.trim();
		city = city == null ? null : city.trim();
		state = state == null ? null : state.trim();
	}
}
