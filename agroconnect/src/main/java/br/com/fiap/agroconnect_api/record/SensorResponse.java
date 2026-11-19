package br.com.fiap.agroconnect_api.record;

import java.util.UUID;

public record SensorResponse(UUID id, UUID deviceId, String type, String label, String unit) {
}
