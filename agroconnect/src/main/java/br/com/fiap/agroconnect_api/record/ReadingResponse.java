package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ReadingResponse(UUID sensorId, String type, BigDecimal value, String unit, String quality, Instant recordedAt) {
}
