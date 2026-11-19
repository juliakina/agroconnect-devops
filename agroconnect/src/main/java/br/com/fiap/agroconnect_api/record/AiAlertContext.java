package br.com.fiap.agroconnect_api.record;

import java.util.List;
import java.util.UUID;

public record AiAlertContext(UUID alertId, String priority, String title, String message, String reason, List<ReadingSnapshot> basedOn, boolean simulatedData) {
}
