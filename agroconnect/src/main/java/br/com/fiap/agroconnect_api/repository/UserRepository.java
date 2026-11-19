package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import jakarta.persistence.LockModeType;

public interface UserRepository extends JpaRepository<User, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from User u where u.id = :id")
	Optional<User> lockById(UUID id);

	Optional<User> findByEmailIgnoreCase(String email);

	Optional<User> findByEmail(String email);
}
