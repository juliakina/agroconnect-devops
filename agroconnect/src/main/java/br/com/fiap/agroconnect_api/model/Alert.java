package br.com.fiap.agroconnect_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "\"Alerts\"", schema = "agroconnect")
public class Alert {

	@ManyToMany
	@JoinTable(name = "\"AlertReadings\"", schema = "agroconnect",
			joinColumns = @JoinColumn(name = "\"AlertId\""),
			inverseJoinColumns = @JoinColumn(name = "\"ReadingId\""))
	private List<Reading> readings = new ArrayList<>();

	public List<Reading> getReadings() {
		return readings;
	}

	public void setReadings(List<Reading> readings) {
		this.readings = readings;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"DeviceId\"", nullable = false)
	private Device device;

	@Column(name = "\"Severity\"", length = 20, nullable = false)
	private String severity = "INFO";

	@Column(name = "\"Status\"", length = 20, nullable = false)
	private String status = "UNREAD";

	@Column(name = "\"Title\"", length = 120, nullable = false)
	private String title;

	@Column(name = "\"Message\"", length = 500, nullable = false)
	private String message;

	@Column(name = "\"Reason\"", length = 500, nullable = false)
	private String reason;

	@Column(name = "\"PushPending\"", nullable = false)
	private boolean pushPending = false;

	@Lob
	@Column(name = "\"BasedOnJson\"", nullable = false)
	private String basedOnJson = "[]";

	@Column(name = "\"CreatedAt\"", nullable = false)
	private Instant createdAt = Instant.now();

	public Alert() {

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

	public String getSeverity() {
		return severity;
	}

	public void setSeverity(String severity) {
		this.severity = severity;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public boolean getPushPending() {
		return pushPending;
	}

	public void setPushPending(boolean pushPending) {
		this.pushPending = pushPending;
	}

	public String getBasedOnJson() {
		return basedOnJson;
	}

	public void setBasedOnJson(String basedOnJson) {
		this.basedOnJson = basedOnJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
