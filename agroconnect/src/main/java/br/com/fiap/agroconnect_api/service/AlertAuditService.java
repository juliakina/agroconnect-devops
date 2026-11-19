package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Alert;
import br.com.fiap.agroconnect_api.model.AlertAudit;
import br.com.fiap.agroconnect_api.record.AlertResponse;
import br.com.fiap.agroconnect_api.record.ReadingSnapshot;
import br.com.fiap.agroconnect_api.repository.AlertAuditRepository;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AlertAuditService {

	@Value("${app.audit.database-enabled:false}")
	private boolean databaseAudit;

	@Autowired
	private DomainSupport support;
	@Autowired
	private AlertRepository alerts;
	@Autowired
	private AlertAuditRepository audits;
	private final JsonMapper mapper = JsonMapper.builder().build();

	private void recordDeletion(UUID userId, List<Alert> rows) {
		if (databaseAudit) return;
		String actor = support.user(userId).getEmail();
		for (Alert item : rows) {
			List<ReadingSnapshot> evidence = mapper.readValue(item.getBasedOnJson(),
					mapper.getTypeFactory().constructCollectionType(List.class, ReadingSnapshot.class));
			AlertAudit audit = new AlertAudit();
			audit.setAlertId(item.getId());
			audit.setUserId(userId);
			audit.setDatabaseUser(actor);
			audit.setOperation("DELETE");
			audit.setOldValues(mapper.writeValueAsString(new AlertResponse(item.getId(), item.getDevice().getId(),
					item.getSeverity(), item.getStatus(), item.getTitle(), item.getMessage(), item.getReason(), evidence, item.getCreatedAt())));
			audits.save(audit);
		}
	}

	public void propertyDeletion(UUID userId, UUID propertyId) {
		recordDeletion(userId, alerts.findByDeviceFieldPropertyIdAndDeviceFieldPropertyUserId(propertyId, userId));
	}

	public void fieldDeletion(UUID userId, UUID fieldId) {
		recordDeletion(userId, alerts.findByDeviceFieldIdAndDeviceFieldPropertyUserId(fieldId, userId));
	}

	public void deviceDeletion(UUID userId, UUID deviceId) {
		recordDeletion(userId, alerts.findByDeviceIdAndDeviceFieldPropertyUserId(deviceId, userId));
	}
}
