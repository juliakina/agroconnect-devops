package br.com.fiap.agroconnect_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"AlertAudits\"", schema = "agroconnect")
public class AlertAudit {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@Column(name = "\"AlertId\"", nullable = false)
	private UUID alertId;

	@Column(name = "\"UserId\"", nullable = false)
	private UUID userId;

	@Column(name = "\"Operation\"", length = 10, nullable = false)
	private String operation;

	@Column(name = "\"OccurredAt\"", nullable = false)
	private Instant occurredAt = Instant.now();

	@Column(name = "\"DatabaseUser\"", length = 254, nullable = false)
	private String databaseUser;

	@Lob
	@Column(name = "\"OldValues\"")
	private String oldValues;

	@Lob
	@Column(name = "\"NewValues\"")
	private String newValues;

	public AlertAudit() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public UUID getAlertId() {
		return alertId;
	}

	public void setAlertId(UUID alertId) {
		this.alertId = alertId;
	}

	public UUID getUserId() {
		return userId;
	}

	public void setUserId(UUID userId) {
		this.userId = userId;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public Instant getOccurredAt() {
		return occurredAt;
	}

	public void setOccurredAt(Instant occurredAt) {
		this.occurredAt = occurredAt;
	}

	public String getDatabaseUser() {
		return databaseUser;
	}

	public void setDatabaseUser(String databaseUser) {
		this.databaseUser = databaseUser;
	}

	public String getOldValues() {
		return oldValues;
	}

	public void setOldValues(String oldValues) {
		this.oldValues = oldValues;
	}

	public String getNewValues() {
		return newValues;
	}

	public void setNewValues(String newValues) {
		this.newValues = newValues;
	}
}
