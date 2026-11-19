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
@Table(name = "\"Recommendations\"", schema = "agroconnect")
public class Recommendation {

	@Column(name = "\"AiGenerated\"", nullable = false)
	private boolean aiGenerated;

	@Column(name = "\"InputFingerprint\"", length = 64)
	private String inputFingerprint;

	public boolean getAiGenerated() {
		return aiGenerated;
	}

	public void setAiGenerated(boolean aiGenerated) {
		this.aiGenerated = aiGenerated;
	}

	public String getInputFingerprint() {
		return inputFingerprint;
	}

	public void setInputFingerprint(String inputFingerprint) {
		this.inputFingerprint = inputFingerprint;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"DeviceId\"", nullable = false)
	private Device device;

	@Column(name = "\"SourceAlertId\"")
	private UUID sourceAlertId;

	@Column(name = "\"Priority\"", length = 20, nullable = false)
	private String priority;

	@Column(name = "\"Title\"", length = 180, nullable = false)
	private String title;

	@Lob
	@Column(name = "\"Message\"", nullable = false)
	private String message;

	@Lob
	@Column(name = "\"Reason\"", nullable = false)
	private String reason;

	@Column(name = "\"Disclaimer\"", length = 1000, nullable = false)
	private String disclaimer;

	@Column(name = "\"SimulatedData\"", nullable = false)
	private boolean simulatedData = true;

	@Column(name = "\"ModelName\"")
	private String modelName;

	@Lob
	@Column(name = "\"BasedOnJson\"", nullable = false)
	private String basedOnJson = "[]";

	@Column(name = "\"GeneratedAt\"", nullable = false)
	private Instant generatedAt = Instant.now();

	@ManyToMany
	@JoinTable(name = "\"RecommendationReadings\"", schema = "agroconnect",
			joinColumns = @JoinColumn(name = "\"RecommendationId\""),
			inverseJoinColumns = @JoinColumn(name = "\"ReadingId\""))
	private List<Reading> readings = new ArrayList<>();

	public Recommendation() {

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

	public UUID getSourceAlertId() {
		return sourceAlertId;
	}

	public void setSourceAlertId(UUID sourceAlertId) {
		this.sourceAlertId = sourceAlertId;
	}

	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
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

	public String getDisclaimer() {
		return disclaimer;
	}

	public void setDisclaimer(String disclaimer) {
		this.disclaimer = disclaimer;
	}

	public boolean getSimulatedData() {
		return simulatedData;
	}

	public void setSimulatedData(boolean simulatedData) {
		this.simulatedData = simulatedData;
	}

	public String getModelName() {
		return modelName;
	}

	public void setModelName(String modelName) {
		this.modelName = modelName;
	}

	public String getBasedOnJson() {
		return basedOnJson;
	}

	public void setBasedOnJson(String basedOnJson) {
		this.basedOnJson = basedOnJson;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public void setGeneratedAt(Instant generatedAt) {
		this.generatedAt = generatedAt;
	}

	public List<Reading> getReadings() {
		return readings;
	}

	public void setReadings(List<Reading> readings) {
		this.readings = readings;
	}
}
