package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.Session;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessionRepository extends JpaRepository<Session, UUID> {

	@Modifying(flushAutomatically = true)
	@Query("delete from Session s where s.user.id = :userId")
	void deleteAllForUser(@Param("userId") UUID userId);

	@Modifying(flushAutomatically = true)
	@Query("delete from Session s where s.user.id = :userId and s.expiresAt <= :now")
	void deleteExpiredForUser(@Param("userId") UUID userId,
			@Param("now") Instant now);

	@Modifying(flushAutomatically = true)
	@Query("delete from Session s where s.id = :sessionId and s.user.id = :userId")
	void deleteOwned(@Param("userId") UUID userId,
			@Param("sessionId") UUID sessionId);

	Optional<Session> findByIdAndUserId(UUID id, UUID userId);
}
