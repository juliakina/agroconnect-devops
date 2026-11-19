package br.com.fiap.agroconnect_api.record;

import java.time.Instant;

public record AiPlantingContext(String cropName, String stage, Instant plantedAt) {
}
