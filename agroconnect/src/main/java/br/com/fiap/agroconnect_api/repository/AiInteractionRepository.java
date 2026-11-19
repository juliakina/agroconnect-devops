package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.AiInteraction;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AiInteractionRepository extends JpaRepository<AiInteraction, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from AiInteraction item where item.id = :id")
	void deleteRecord(@Param("id") UUID id);

	Optional<AiInteraction> findByIdAndUserId(UUID id, UUID userId);
	Page<AiInteraction> findByUserId(UUID userId, Pageable pageable);
}
