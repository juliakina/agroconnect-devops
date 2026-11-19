package br.com.fiap.agroconnect_api.record;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import br.com.fiap.agroconnect_api.validation.DecimalPrecision;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/* O setter distingue description ausente de description explicitamente null. */
public class PropertyPatch {

	@Size(min = 2, max = 150)
	private String name;
	@Size(min = 2, max = 120)
	private String city;
	@Pattern(regexp = "[a-zA-Z]{2}")
	private String state;
	@DecimalMin(value = "0", inclusive = false)
	@Digits(integer = 29, fraction = 28) @DecimalPrecision
	private BigDecimal areaHectares;
	@Size(max = 2000)
	private String description;
	private boolean hasDescription;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public BigDecimal getAreaHectares() {
		return areaHectares;
	}

	public void setAreaHectares(BigDecimal areaHectares) {
		this.areaHectares = areaHectares;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
		this.hasDescription = true;
	}

	public boolean hasDescription() {
		return hasDescription;
	}
}
