package br.com.fiap.agroconnect_api.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiRecommendationDraft(@NotNull @JsonProperty(required = true) UUID alertId,
		@NotBlank @Size(min = 3, max = 120) @JsonProperty(required = true) String title,
		@NotBlank @Size(min = 3, max = 500) @JsonProperty(required = true) String message,
		@NotBlank @Size(min = 3, max = 500) @JsonProperty(required = true) String reason) {

	public AiRecommendationDraft {
		title = title == null ? null : title.trim();
		message = message == null ? null : message.trim();
		reason = reason == null ? null : reason.trim();
	}
}
