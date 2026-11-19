package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;
import java.util.UUID;

public record AlertInput(@NotNull @NonZeroUuid UUID deviceId, @NotBlank @Pattern(regexp = "INFO|WARNING|CRITICAL") String severity,
		@NotBlank @Size(min = 3, max = 120) String title, @NotBlank @Size(min = 3, max = 500) String message, @NotBlank @Size(min = 3, max = 500) String reason) {

	public AlertInput {
		severity = severity == null ? null : severity.trim().toUpperCase(Locale.ROOT);
		title = title == null ? null : title.trim();
		message = message == null ? null : message.trim();
		reason = reason == null ? null : reason.trim();
	}
}
