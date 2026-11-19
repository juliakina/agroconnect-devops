package br.com.fiap.agroconnect_api.record;

import java.util.UUID;

public record CropResponse(UUID id, String code, String name, String scientificName, String description) {
}
