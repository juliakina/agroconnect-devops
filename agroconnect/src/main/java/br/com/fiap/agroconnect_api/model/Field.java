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
import java.util.UUID;

@Entity
@Table(name = "\"Fields\"", schema = "agroconnect")
public class Field {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "\"PropertyId\"", nullable = false)
	private Property property;

	@Column(name = "\"Name\"", nullable = false)
	private String name;

	@Column(name = "\"AreaHectares\"", columnDefinition = "NUMBER", nullable = false)
	private BigDecimal areaHectares;

	public Field() {

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BigDecimal getAreaHectares() {
		return areaHectares;
	}

	public void setAreaHectares(BigDecimal areaHectares) {
		this.areaHectares = areaHectares;
	}
}
