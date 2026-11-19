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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"Sensors\"", schema = "agroconnect")
public class Sensor {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"DeviceId\"", nullable = false)
	private Device device;

	@Column(name = "\"Type\"", length = 50, nullable = false)
	private String type;

	@Column(name = "\"Label\"", length = 150, nullable = false)
	private String label;

	@Column(name = "\"Unit\"", length = 20, nullable = false)
	private String unit;

	@Column(name = "\"Code\"")
	private String code;

	@Column(name = "\"Calibrated\"", nullable = false)
	private boolean calibrated = true;

	@Column(name = "\"InstalledAt\"")
	private Instant installedAt;

	public Sensor() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public Device getDevice() {
		return device;
	}

	public void setDevice(Device device) {
		this.device = device;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public boolean getCalibrated() {
		return calibrated;
	}

	public void setCalibrated(boolean calibrated) {
		this.calibrated = calibrated;
	}

	public Instant getInstalledAt() {
		return installedAt;
	}

	public void setInstalledAt(Instant installedAt) {
		this.installedAt = installedAt;
	}
}
