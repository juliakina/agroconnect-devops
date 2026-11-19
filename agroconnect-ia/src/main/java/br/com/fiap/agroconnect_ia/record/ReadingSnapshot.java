package br.com.fiap.agroconnect_ia.record;

import java.math.BigDecimal;
import java.time.Instant;

public record ReadingSnapshot(String type, BigDecimal value, String unit, Instant recordedAt) {
}
