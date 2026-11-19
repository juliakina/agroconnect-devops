package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.Device;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

	Optional<Device> findByCode(String code);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Device item where item.id = :id")
	void deleteRecord(@Param("id") UUID id);

	@Query("select d from Device d where d.field.property.user.id = :userId and (:fieldId is null or d.field.id = :fieldId)")
	Page<Device> findOwned(@Param("userId") UUID userId, @Param("fieldId") UUID fieldId, Pageable pageable);
	Optional<Device> findByIdAndFieldPropertyUserId(UUID id, UUID userId);
}
