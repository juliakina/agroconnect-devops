package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;

public record HistorySummary(BigDecimal min, BigDecimal max, BigDecimal avg) {
}
