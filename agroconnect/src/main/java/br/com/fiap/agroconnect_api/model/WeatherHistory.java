package br.com.fiap.agroconnect_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"WeatherHistory\"", schema = "agroconnect")
public class WeatherHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"PropertyId\"", nullable = false)
	private Property property;

	@Column(name = "\"ObservedAt\"", nullable = false)
	private Instant observedAt;

	@Column(name = "\"TemperatureC\"", precision = 20, scale = 6)
	private BigDecimal temperatureC;

	@Column(name = "\"RelativeHumidity\"", precision = 20, scale = 6)
	private BigDecimal relativeHumidity;

	@Column(name = "\"PrecipitationMm\"", precision = 20, scale = 6)
	private BigDecimal precipitationMm;

	@Column(name = "\"WindSpeedKmh\"", precision = 20, scale = 6)
	private BigDecimal windSpeedKmh;

	@Column(name = "\"Origin\"", length = 20, nullable = false)
	private String origin;

	public WeatherHistory() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Property getProperty() {
		return property;
	}

	public void setProperty(Property property) {
		this.property = property;
	}

	public Instant getObservedAt() {
		return observedAt;
	}

	public void setObservedAt(Instant observedAt) {
		this.observedAt = observedAt;
	}

	public BigDecimal getTemperatureC() {
		return temperatureC;
	}

	public void setTemperatureC(BigDecimal temperatureC) {
		this.temperatureC = temperatureC;
	}

	public BigDecimal getRelativeHumidity() {
		return relativeHumidity;
	}

	public void setRelativeHumidity(BigDecimal relativeHumidity) {
		this.relativeHumidity = relativeHumidity;
	}

	public BigDecimal getPrecipitationMm() {
		return precipitationMm;
	}

	public void setPrecipitationMm(BigDecimal precipitationMm) {
		this.precipitationMm = precipitationMm;
	}

	public BigDecimal getWindSpeedKmh() {
		return windSpeedKmh;
	}

	public void setWindSpeedKmh(BigDecimal windSpeedKmh) {
		this.windSpeedKmh = windSpeedKmh;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}
}
