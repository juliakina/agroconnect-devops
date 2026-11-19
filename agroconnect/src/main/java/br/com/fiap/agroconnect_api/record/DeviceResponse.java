package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(UUID id, String code, String name, UUID fieldId, String status, Instant lastSeenAt, Integer rssi, String firmwareVersion) {
}
