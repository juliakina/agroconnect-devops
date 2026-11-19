package br.com.fiap.agroconnect_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
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
@Table(name = "\"Properties\"", schema = "agroconnect")
public class Property {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"UserId\"", nullable = false)
	private User user;

	@Column(name = "\"Name\"", nullable = false)
	private String name;

	@Column(name = "\"City\"", nullable = false)
	private String city;

	@Column(name = "\"State\"", nullable = false)
	private String state;

	@Column(name = "\"AreaHectares\"", columnDefinition = "NUMBER", nullable = false)
	private BigDecimal areaHectares;

	@Lob
	@Column(name = "\"Description\"")
	private String description;

	@Column(name = "\"CreatedAt\"", nullable = false)
	private Instant createdAt = Instant.now();

	@Column(name = "\"UpdatedAt\"", nullable = false)
	private Instant updatedAt = Instant.now();

	public Property() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

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
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}
}
