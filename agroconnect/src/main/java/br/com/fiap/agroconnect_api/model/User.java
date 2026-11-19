package br.com.fiap.agroconnect_api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "\"Users\"", schema = "agroconnect")
public class User {

	@Enumerated(EnumType.STRING)
	@Column(name = "\"Role\"", length = 10, nullable = false)
	private UserRole role = UserRole.USER;

	@Column(name = "\"Active\"", nullable = false)
	private boolean active = true;

	public UserRole getRole() {
		return role;
	}

	public void setRole(UserRole role) {
		this.role = role;
	}

	public boolean getActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	@Column(name = "\"Id\"")
	private UUID id;

	@Column(name = "\"Name\"", nullable = false)
	private String name;

	@Column(name = "\"Email\"", length = 254, nullable = false, unique = true)
	private String email;

	@Column(name = "\"PasswordHash\"", nullable = false)
	private String passwordHash;

	@Column(name = "\"CreatedAt\"", nullable = false)
	private Instant createdAt = Instant.now();

	public User() {

	}

	public UUID getId() {
		return id;
	}

	public void setId(UUID id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
