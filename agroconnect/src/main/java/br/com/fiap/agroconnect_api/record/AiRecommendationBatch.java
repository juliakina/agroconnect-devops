package br.com.fiap.agroconnect_api.record;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AiRecommendationBatch(@NotNull @Size(min = 1, max = 20) @JsonProperty(required = true)
		List<@NotNull @Valid AiRecommendationDraft> recommendations) {
}
