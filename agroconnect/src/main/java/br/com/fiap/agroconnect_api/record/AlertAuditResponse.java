package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record AlertAuditResponse(UUID id, UUID alertId, String operation, Instant occurredAt, String databaseUser, String oldValues, String newValues) {
}
