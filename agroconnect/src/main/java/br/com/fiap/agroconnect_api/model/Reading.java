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
@Table(name = "\"Readings\"", schema = "agroconnect")
public class Reading {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"SensorId\"", nullable = false)
	private Sensor sensor;

	@Column(name = "\"Value\"", precision = 12, scale = 3)
	private BigDecimal value;

	@Column(name = "\"Quality\"", length = 20, nullable = false)
	private String quality = "VALID";

	@Column(name = "\"RecordedAt\"", nullable = false)
	private Instant recordedAt;

	@Column(name = "\"ReceivedAt\"", nullable = false)
	private Instant receivedAt;

	@Column(name = "\"SimulatedData\"", nullable = false)
	private boolean simulatedData = true;

	@Column(name = "\"Origin\"", length = 20, nullable = false)
	private String origin = "SIMULATED";

	@Column(name = "\"Observations\"")
	private String observations;

	public Reading() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Sensor getSensor() {
		return sensor;
	}

	public void setSensor(Sensor sensor) {
		this.sensor = sensor;
	}

	public BigDecimal getValue() {
		return value;
	}

	public void setValue(BigDecimal value) {
		this.value = value;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}

	public void setRecordedAt(Instant recordedAt) {
		this.recordedAt = recordedAt;
	}

	public Instant getReceivedAt() {
		return receivedAt;
	}

	public void setReceivedAt(Instant receivedAt) {
		this.receivedAt = receivedAt;
	}

	public boolean getSimulatedData() {
		return simulatedData;
	}

	public void setSimulatedData(boolean simulatedData) {
		this.simulatedData = simulatedData;
	}

	public String getOrigin() {
		return origin;
	}

	public void setOrigin(String origin) {
		this.origin = origin;
	}

	public String getObservations() {
		return observations;
	}

	public void setObservations(String observations) {
		this.observations = observations;
	}
}
