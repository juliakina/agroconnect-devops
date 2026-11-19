package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PropertyResponse(UUID id, String name, String city, String state, BigDecimal areaHectares,
		String description, Instant createdAt, Instant updatedAt) {

}
