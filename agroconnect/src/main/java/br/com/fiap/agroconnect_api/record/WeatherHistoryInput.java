package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record WeatherHistoryInput(@NotNull @NonZeroUuid UUID propertyId, @NotNull @PastOrPresent Instant observedAt,
		@DecimalMin("-100") @DecimalMax("100") @Digits(integer = 14, fraction = 6) BigDecimal temperatureC,
		@DecimalMin("0") @DecimalMax("100") @Digits(integer = 14, fraction = 6) BigDecimal relativeHumidity,
		@DecimalMin("0") @DecimalMax("999999") @Digits(integer = 14, fraction = 6) BigDecimal precipitationMm,
		@DecimalMin("0") @DecimalMax("999999") @Digits(integer = 14, fraction = 6) BigDecimal windSpeedKmh, @NotBlank @Pattern(regexp = "SIMULATED|REAL|ESTIMATED") String origin) {

	public WeatherHistoryInput {
		origin = origin == null ? null : origin.trim().toUpperCase(Locale.ROOT);
	}
}
