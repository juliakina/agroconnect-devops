package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadingDetailsResponse(UUID id, UUID sensorId, BigDecimal value, String quality, Instant recordedAt, Instant receivedAt, boolean simulatedData, String origin, String observations) {
}
