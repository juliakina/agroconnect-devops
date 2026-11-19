package br.com.fiap.agroconnect_ia.record;

import java.time.Instant;
import java.util.List;

public record AiDeviceContext(String deviceCode, String deviceName, String deviceStatus, Instant lastSeenAt, String fieldName,
		List<AiPlantingContext> plantings, List<AiAlertContext> alerts) {
}
