package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record SensorInput(@NotNull @NonZeroUuid UUID deviceId, @NotBlank @Pattern(regexp = "SOIL_MOISTURE|AIR_TEMPERATURE|AIR_HUMIDITY|LIGHT_ILLUMINANCE|RAINFALL|WATER_LEVEL") String type,
		@NotBlank @Size(min = 2, max = 150) String label, @NotBlank @Size(min = 1, max = 20) String unit, @Size(max = 60) String code, Boolean calibrated, @PastOrPresent Instant installedAt) {

	public SensorInput {
		type = type == null ? null : type.trim().toUpperCase(Locale.ROOT);
		label = label == null ? null : label.trim();
		unit = unit == null ? null : unit.trim();
		code = code == null ? null : code.trim();
	}
}
