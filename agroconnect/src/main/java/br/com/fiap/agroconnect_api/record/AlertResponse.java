package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AlertResponse(UUID id, UUID deviceId, String severity, String status, String title, String message, String reason, List<ReadingSnapshot> basedOn, Instant createdAt) {
}
