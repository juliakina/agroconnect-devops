package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import br.com.fiap.agroconnect_api.validation.DecimalPrecision;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record FieldInput(@NotBlank @Size(min = 2, max = 150) String name,
		@NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 29, fraction = 28) @DecimalPrecision BigDecimal areaHectares) {

	public FieldInput {
		name = name == null ? null : name.trim();
	}
}
