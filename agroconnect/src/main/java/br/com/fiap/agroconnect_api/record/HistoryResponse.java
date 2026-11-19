package br.com.fiap.agroconnect_api.record;

import java.util.List;
import java.util.UUID;

public record HistoryResponse(UUID deviceId, UUID sensorId, String type, String unit, String interval, List<HistoryPoint> items, HistorySummary summary, boolean simulatedData) {
}
