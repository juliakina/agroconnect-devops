package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record PlantingDetailsResponse(UUID id, UUID fieldId, String cropName, Instant plantedAt, String stage,
		UUID cropId, String variety, String status, String observations) {

}
