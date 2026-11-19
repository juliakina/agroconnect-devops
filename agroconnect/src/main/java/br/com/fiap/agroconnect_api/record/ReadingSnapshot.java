package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;

public record ReadingSnapshot(String type, BigDecimal value, String unit, Instant recordedAt) {
}
