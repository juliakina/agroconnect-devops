package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record DeviceEventResponse(UUID id, UUID deviceId, String type, String description, Instant occurredAt, String origin) {
}
