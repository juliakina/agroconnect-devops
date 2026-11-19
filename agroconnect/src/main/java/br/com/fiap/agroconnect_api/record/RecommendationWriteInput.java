package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public record RecommendationWriteInput(@NotNull @NonZeroUuid UUID deviceId, @NotBlank @Pattern(regexp = "INFO|WARNING|CRITICAL") String priority,
		@NotBlank @Size(min = 3, max = 180) String title, @NotBlank @Size(min = 3, max = 2000) String message, @NotBlank @Size(min = 3, max = 2000) String reason, @NotBlank @Size(min = 3, max = 1000) String disclaimer,
		Boolean simulatedData, @Size(max = 120) String modelName, @Size(max = 100) List<@NotNull @NonZeroUuid UUID> readingIds) {

	public RecommendationWriteInput {
		priority = priority == null ? null : priority.trim().toUpperCase(Locale.ROOT);
		title = title == null ? null : title.trim();
		message = message == null ? null : message.trim();
		reason = reason == null ? null : reason.trim();
		disclaimer = disclaimer == null ? null : disclaimer.trim();
		modelName = modelName == null ? null : modelName.trim();
	}
}
