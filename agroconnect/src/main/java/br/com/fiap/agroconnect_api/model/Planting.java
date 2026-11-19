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
@Table(name = "\"Plantings\"", schema = "agroconnect")
public class Planting {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "\"CropId\"")
	private Crop crop;

	@Column(name = "\"Variety\"", length = 100)
	private String variety;

	@Column(name = "\"Status\"", length = 20, nullable = false)
	private String status = "ACTIVE";

	@Column(name = "\"Observations\"", length = 1000)
	private String observations;

	public Crop getCrop() {
		return crop;
	}

	public void setCrop(Crop crop) {
		this.crop = crop;
	}

	public String getVariety() {
		return variety;
	}

	public void setVariety(String variety) {
		this.variety = variety;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getObservations() {
		return observations;
	}

	public void setObservations(String observations) {
		this.observations = observations;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"FieldId\"", nullable = false)
	private Field field;

	@Column(name = "\"CropName\"", nullable = false)
	private String cropName;

	@Column(name = "\"PlantedAt\"", nullable = false)
	private Instant plantedAt;

	@Column(name = "\"Stage\"", nullable = false)
	private String stage;

	public Planting() {

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

	public String getCropName() {
		return cropName;
	}

	public void setCropName(String cropName) {
		this.cropName = cropName;
	}

	public Instant getPlantedAt() {
		return plantedAt;
	}

	public void setPlantedAt(Instant plantedAt) {
		this.plantedAt = plantedAt;
	}

	public String getStage() {
		return stage;
	}

	public void setStage(String stage) {
		this.stage = stage;
	}
}
