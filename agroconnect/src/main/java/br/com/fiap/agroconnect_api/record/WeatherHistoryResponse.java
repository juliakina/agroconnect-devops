package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WeatherHistoryResponse(UUID id, UUID propertyId, Instant observedAt, BigDecimal temperatureC, BigDecimal relativeHumidity, BigDecimal precipitationMm, BigDecimal windSpeedKmh, String origin) {
}
