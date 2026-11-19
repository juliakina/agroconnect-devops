package br.com.fiap.agroconnect_api.record;

import java.math.BigDecimal;
import java.util.UUID;

public record FieldResponse(UUID id, UUID propertyId, String name, BigDecimal areaHectares) {

}
