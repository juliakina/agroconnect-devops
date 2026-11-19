package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.PushToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PushTokenRepository extends JpaRepository<PushToken, UUID> {

	@Query("select p from PushToken p where p.user.id = :userId and p.session.expiresAt > CURRENT_TIMESTAMP")
	java.util.List<PushToken> findAllByUserId(@Param("userId") UUID userId);

	@Modifying(flushAutomatically = true)
	@Query("delete from PushToken p where p.user.id = :userId")
	void deleteAllForUser(@Param("userId") UUID userId);

	@Modifying(flushAutomatically = true)
	@Query("delete from PushToken p where p.user.id = :userId and p.session.id = :sessionId")
	void deleteForSession(@Param("userId") UUID userId,
			@Param("sessionId") UUID sessionId);

	@Modifying(flushAutomatically = true)
	@Query("delete from PushToken p where p.session.user.id = :userId and p.session.expiresAt <= :now")
	void deleteExpiredForUser(@Param("userId") UUID userId,
			@Param("now") Instant now);

	Optional<PushToken> findByToken(String token);
}
