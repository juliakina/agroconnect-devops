package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.Field;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FieldRepository extends JpaRepository<Field, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Field item where item.id = :id")
	void deleteRecord(@Param("id") UUID id);

	Page<Field> findByPropertyIdAndPropertyUserId(UUID propertyId, UUID userId, Pageable pageable);
	Optional<Field> findByIdAndPropertyUserId(UUID id, UUID userId);
}
