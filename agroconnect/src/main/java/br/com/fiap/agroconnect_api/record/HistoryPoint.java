package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;

public record HistoryPoint(Instant timestamp, BigDecimal value, BigDecimal min, BigDecimal max, int sampleCount, String quality) {
}
