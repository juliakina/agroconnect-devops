package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.Alert;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select a from Alert a where a.id = :id and a.device.field.property.user.id = :userId")
	Optional<Alert> lockOwned(@Param("id") UUID id, @Param("userId") UUID userId);

	List<Alert> findByDeviceFieldPropertyIdAndDeviceFieldPropertyUserId(UUID propertyId, UUID userId);
	List<Alert> findByDeviceFieldIdAndDeviceFieldPropertyUserId(UUID fieldId, UUID userId);
	List<Alert> findByDeviceIdAndDeviceFieldPropertyUserId(UUID deviceId, UUID userId);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from Alert item where item.id = :id")
	void deleteRecord(@Param("id") UUID id);

	Optional<Alert> findByIdAndDeviceFieldPropertyUserId(UUID id, UUID userId);
	@Query("select a from Alert a where a.device.field.property.user.id = :userId and (:status is null or a.status = :status) order by case a.severity when 'CRITICAL' then 2 when 'WARNING' then 1 else 0 end desc, a.createdAt desc, a.id asc")
	Page<Alert> findOwned(@Param("userId") UUID userId, @Param("status") String status, Pageable pageable);
	@Query("select a from Alert a where a.device.id = :deviceId and a.device.field.property.user.id = :userId and a.status <> 'RESOLVED' order by case a.severity when 'CRITICAL' then 2 when 'WARNING' then 1 else 0 end desc, a.createdAt desc, a.id asc")
	List<Alert> findForRecommendation(@Param("userId") UUID userId, @Param("deviceId") UUID deviceId, Pageable pageable);
}
