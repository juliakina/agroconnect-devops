package br.com.fiap.agroconnect_api.record;

import br.com.fiap.agroconnect_api.validation.NonZeroUuid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

public record PlantingInput(@NotBlank @Size(min = 2, max = 150) String cropName,
		@NotNull Instant plantedAt,
		@NotBlank @Pattern(regexp = "(?i)SEEDLING|VEGETATIVE|MATURATION|HARVEST") String stage,
		@NonZeroUuid UUID cropId, @Size(max = 100) String variety,
		@Pattern(regexp = "ACTIVE|FINISHED") String status, @Size(max = 1000) String observations) {

	public PlantingInput {
		cropName = cropName == null ? null : cropName.trim();
		stage = stage == null ? null : stage.trim().toUpperCase(Locale.ROOT);
		status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
	}

	public PlantingInput(String cropName, Instant plantedAt, String stage) {
		this(cropName, plantedAt, stage, null, null, null, null);
	}
}
