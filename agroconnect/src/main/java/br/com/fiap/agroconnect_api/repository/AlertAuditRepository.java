package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.AlertAudit;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertAuditRepository extends JpaRepository<AlertAudit, UUID> {

	Page<AlertAudit> findByUserId(UUID userId, Pageable pageable);
	Optional<AlertAudit> findByIdAndUserId(UUID id, UUID userId);
}
