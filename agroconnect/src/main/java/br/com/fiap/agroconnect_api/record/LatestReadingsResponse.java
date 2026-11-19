package br.com.fiap.agroconnect_api.record;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LatestReadingsResponse(UUID deviceId, Instant recordedAt, List<ReadingResponse> readings, boolean simulatedData) {
}
