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

public record DeviceEventInput(@NotNull @NonZeroUuid UUID deviceId, @NotBlank @Pattern(regexp = "CONNECTED|DISCONNECTED|UNSTABLE|SENSOR_FAILURE|SENSOR_RECOVERED|UNKNOWN") String type,
		@NotBlank @Size(min = 1, max = 1000) String description, @NotNull @PastOrPresent Instant occurredAt, @NotBlank @Pattern(regexp = "SIMULATED|REAL") String origin) {

	public DeviceEventInput {
		type = type == null ? null : type.trim().toUpperCase(Locale.ROOT);
		description = description == null ? null : description.trim();
		origin = origin == null ? null : origin.trim().toUpperCase(Locale.ROOT);
	}
}
