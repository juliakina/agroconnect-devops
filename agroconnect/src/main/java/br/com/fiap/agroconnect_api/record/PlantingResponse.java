package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record PlantingResponse(UUID id, UUID fieldId, String cropName, Instant plantedAt, String stage) {

}
