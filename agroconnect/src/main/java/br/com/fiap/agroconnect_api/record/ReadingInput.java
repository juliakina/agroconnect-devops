package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record ReadingInput(@NotNull @NonZeroUuid UUID sensorId, @Digits(integer = 9, fraction = 3) BigDecimal value,
		@NotBlank @Pattern(regexp = "VALID|OUT_OF_RANGE|SENSOR_ERROR|UNCALIBRATED") String quality, @NotNull @PastOrPresent Instant recordedAt,
		@PastOrPresent Instant receivedAt, Boolean simulatedData, @Pattern(regexp = "SIMULATED|REAL|ESTIMATED") String origin,
		@Size(max = 1000) String observations) {

	public ReadingInput {
		quality = quality == null ? null : quality.trim().toUpperCase(Locale.ROOT);
		origin = origin == null ? null : origin.trim().toUpperCase(Locale.ROOT);
	}

	@AssertTrue(message = "Leitura válida exige valor; erro de sensor exige valor nulo.")
	@JsonIgnore
	public boolean isConsistent() {
		return quality == null || ("VALID".equals(quality) ? value != null
				: !"SENSOR_ERROR".equals(quality) || value == null);
	}

	@AssertTrue(message = "receivedAt não pode ser anterior a recordedAt.")
	@JsonIgnore
	public boolean isChronological() {
		return receivedAt == null || recordedAt == null || !receivedAt.isBefore(recordedAt);
	}
}
