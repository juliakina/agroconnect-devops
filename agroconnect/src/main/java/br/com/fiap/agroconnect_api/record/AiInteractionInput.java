package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.JsonObject;
import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AiInteractionInput(@NonZeroUuid UUID propertyId, @NonZeroUuid UUID deviceId, @NotBlank @Size(min = 1, max = 20000) String question,
		@Size(max = 20000) String response, @JsonObject @Size(max = 20000) String context, @Size(max = 120) String modelName) {

	public AiInteractionInput {
		question = question == null ? null : question.trim();
		modelName = modelName == null ? null : modelName.trim();
	}
}
