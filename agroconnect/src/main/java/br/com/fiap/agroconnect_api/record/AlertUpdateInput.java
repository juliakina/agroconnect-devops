package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

public record AlertUpdateInput(@NotBlank @Pattern(regexp = "INFO|WARNING|CRITICAL") String severity, @NotBlank @Pattern(regexp = "UNREAD|READ|RESOLVED") String status,
		@NotBlank @Size(min = 3, max = 120) String title, @NotBlank @Size(min = 3, max = 500) String message, @NotBlank @Size(min = 3, max = 500) String reason) {

	public AlertUpdateInput {
		severity = severity == null ? null : severity.trim().toUpperCase(Locale.ROOT);
		status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
		title = title == null ? null : title.trim();
		message = message == null ? null : message.trim();
		reason = reason == null ? null : reason.trim();
	}
}
