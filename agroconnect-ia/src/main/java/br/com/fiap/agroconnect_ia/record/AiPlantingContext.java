package br.com.fiap.agroconnect_ia.record;

import java.time.Instant;

public record AiPlantingContext(String cropName, String stage, Instant plantedAt) {
}
