package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.UUID;

public record AiInteractionResponse(UUID id, UUID propertyId, UUID deviceId, String question, String response, String context, String modelName, Instant createdAt) {
}
