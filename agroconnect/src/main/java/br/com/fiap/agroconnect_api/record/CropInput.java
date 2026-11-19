package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CropInput(@NotBlank @Size(min = 2, max = 40) String code, @NotBlank @Size(min = 2, max = 100) String name, @Size(max = 140) String scientificName, @Size(max = 1000) String description) {

	public CropInput {
		code = code == null ? null : code.trim();
		name = name == null ? null : name.trim();
		scientificName = scientificName == null ? null : scientificName.trim();
		description = description == null ? null : description.trim();
	}
}
