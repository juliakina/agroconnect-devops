package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecommendationResponse(UUID id, Instant generatedAt, String priority, String title, String message, String reason, List<ReadingSnapshot> basedOn, String disclaimer, boolean simulatedData) {
}
