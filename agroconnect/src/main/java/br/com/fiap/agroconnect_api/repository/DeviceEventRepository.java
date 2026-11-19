package br.com.fiap.agroconnect_api.repository;

import br.com.fiap.agroconnect_api.model.DeviceEvent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceEventRepository extends JpaRepository<DeviceEvent, UUID> {

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from DeviceEvent item where item.id = :id")
	void deleteRecord(@Param("id") UUID id);

	Optional<DeviceEvent> findByIdAndDeviceFieldPropertyUserId(UUID id, UUID userId);
	Page<DeviceEvent> findByDeviceIdAndDeviceFieldPropertyUserId(UUID deviceId, UUID userId, Pageable pageable);
}
