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
@Table(name = "\"Devices\"", schema = "agroconnect")
public class Device {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"FieldId\"", nullable = false)
	private Field field;

	@Column(name = "\"Code\"", length = 60, nullable = false)
	private String code;

	@Column(name = "\"Name\"", length = 150, nullable = false)
	private String name;

	@Column(name = "\"LastSeenAt\"")
	private Instant lastSeenAt;

	@Column(name = "\"Rssi\"")
	private Integer rssi;

	@Column(name = "\"FirmwareVersion\"", length = 40, nullable = false)
	private String firmwareVersion = "1.0.0";

	@Column(name = "\"Transport\"", length = 20, nullable = false)
	private String transport = "WIFI";

	@Column(name = "\"SimulatedData\"", nullable = false)
	private boolean simulatedData = true;

	@Column(name = "\"SoilMoistureThreshold\"", precision = 20, scale = 6, nullable = false)
	private BigDecimal soilMoistureThreshold = new BigDecimal("30");

	public Device() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Instant getLastSeenAt() {
		return lastSeenAt;
	}

	public void setLastSeenAt(Instant lastSeenAt) {
		this.lastSeenAt = lastSeenAt;
	}

	public Integer getRssi() {
		return rssi;
	}

	public void setRssi(Integer rssi) {
		this.rssi = rssi;
	}

	public String getFirmwareVersion() {
		return firmwareVersion;
	}

	public void setFirmwareVersion(String firmwareVersion) {
		this.firmwareVersion = firmwareVersion;
	}

	public String getTransport() {
		return transport;
	}

	public void setTransport(String transport) {
		this.transport = transport;
	}

	public boolean getSimulatedData() {
		return simulatedData;
	}

	public void setSimulatedData(boolean simulatedData) {
		this.simulatedData = simulatedData;
	}

	public BigDecimal getSoilMoistureThreshold() {
		return soilMoistureThreshold;
	}

	public void setSoilMoistureThreshold(BigDecimal soilMoistureThreshold) {
		this.soilMoistureThreshold = soilMoistureThreshold;
	}
}
