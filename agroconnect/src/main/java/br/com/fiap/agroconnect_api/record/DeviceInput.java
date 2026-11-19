package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record DeviceInput(@NotNull @NonZeroUuid UUID fieldId, @NotBlank @Size(min = 2, max = 60) String code, @NotBlank @Size(min = 2, max = 150) String name, @PastOrPresent Instant lastSeenAt,
		@Min(-127) @Max(0) Integer rssi, @NotBlank @Size(min = 1, max = 40) String firmwareVersion,
		@Pattern(regexp = "WIFI|ETHERNET|OTHER") String transport, Boolean simulatedData,
		@DecimalMin("0") @DecimalMax("100") @Digits(integer = 14, fraction = 6) BigDecimal soilMoistureThreshold) {

	public DeviceInput {
		code = code == null ? null : code.trim();
		name = name == null ? null : name.trim();
		firmwareVersion = firmwareVersion == null ? null : firmwareVersion.trim();
		transport = transport == null ? null : transport.trim().toUpperCase(Locale.ROOT);
	}
}
