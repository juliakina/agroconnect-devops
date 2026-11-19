package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.Alert;
import br.com.fiap.agroconnect_api.model.AlertAudit;
import br.com.fiap.agroconnect_api.model.Reading;
import br.com.fiap.agroconnect_api.model.Recommendation;
import br.com.fiap.agroconnect_api.record.AlertAuditResponse;
import br.com.fiap.agroconnect_api.record.AlertInput;
import br.com.fiap.agroconnect_api.record.AlertResponse;
import br.com.fiap.agroconnect_api.record.AlertUpdateInput;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.ReadingSnapshot;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.record.RecommendationResponse;
import br.com.fiap.agroconnect_api.record.RecommendationWriteInput;
import br.com.fiap.agroconnect_api.repository.AlertAuditRepository;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import br.com.fiap.agroconnect_api.repository.ReadingRepository;
import br.com.fiap.agroconnect_api.repository.RecommendationRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class AlertService {

	@Autowired
	private org.springframework.context.ApplicationEventPublisher events;

	@Value("${app.audit.database-enabled:false}")
	private boolean databaseAudit;

	private final JsonMapper mapper = JsonMapper.builder().build();
	@Autowired
	private DomainSupport support;
	@Autowired
	private MonitoringService monitoring;
	@Autowired
	private AlertRepository alerts;
	@Autowired
	private AlertAuditRepository audits;
	@Autowired
	private RecommendationRepository recommendations;
	@Autowired
	private ReadingRepository readings;
	@Autowired
	private RecommendationService recommendationService;

	private List<ReadingSnapshot> snapshots(String json) {
		return mapper.readValue(json, mapper.getTypeFactory().constructCollectionType(List.class, ReadingSnapshot.class));
	}

	private AlertResponse response(Alert item) {
		return new AlertResponse(item.getId(), item.getDevice().getId(), item.getSeverity(), item.getStatus(),
				item.getTitle(), item.getMessage(), item.getReason(), snapshots(item.getBasedOnJson()), item.getCreatedAt());
	}

	private RecommendationResponse response(Recommendation item) {
		return new RecommendationResponse(item.getId(), item.getGeneratedAt(), item.getPriority(), item.getTitle(),
				item.getMessage(), item.getReason(), snapshots(item.getBasedOnJson()), item.getDisclaimer(), item.getSimulatedData());
	}

	private Alert alert(UUID userId, UUID id) {
		return alerts.findByIdAndDeviceFieldPropertyUserId(id, userId).orElseThrow(() -> support.missing("Alerta"));
	}

	private Recommendation recommendation(UUID userId, UUID id) {
		return recommendations.findByIdAndDeviceFieldPropertyUserId(id, userId).orElseThrow(() -> support.missing("Recomendação"));
	}

	private void audit(UUID userId, Alert item, String operation, String before, String after) {
		if (databaseAudit) return;
		AlertAudit audit = new AlertAudit();
		audit.setAlertId(item.getId());
		audit.setUserId(userId);
		audit.setOperation(operation);
		audit.setDatabaseUser(support.user(userId).getEmail());
		audit.setOldValues(before);
		audit.setNewValues(after);
		audits.save(audit);
	}

	@Transactional(readOnly = true)
	public PageResponse<AlertResponse> listAlerts(UUID userId, String status, int page, int size) {
		status = status == null ? null : status.trim().toUpperCase(Locale.ROOT);
		if (status != null && !List.of("UNREAD", "READ", "RESOLVED").contains(status))
			throw support.invalid("status", "Informe UNREAD, READ ou RESOLVED.");
		return PageResponse.from(alerts.findOwned(userId, status, support.page(page, size)).map(this::response));
	}

	@Transactional(readOnly = true)
	public AlertResponse getAlert(UUID userId, UUID id) {
		return response(alert(userId, id));
	}

	@Transactional
	public AlertResponse createAlert(UUID userId, AlertInput input) {
		support.validate(input);
		Alert item = new Alert();
		item.setDevice(support.device(userId, input.deviceId()));
		item.setSeverity(input.severity());
		item.setTitle(input.title());
		item.setMessage(input.message());
		item.setReason(input.reason());
		var evidence = monitoring.evidenceRows(userId, input.deviceId());
		item.setReadings(new ArrayList<>(evidence));
		item.setBasedOnJson(mapper.writeValueAsString(evidence.stream().map(monitoring::snapshot).toList()));
		item.setPushPending(false);
		alerts.saveAndFlush(item);
		audit(userId, item, "INSERT", null, mapper.writeValueAsString(response(item)));
		events.publishEvent(new AlertCreated(userId, item.getId(), item.getTitle(), item.getMessage(), item.getSeverity()));
		return response(item);
	}

	@Transactional
	public AlertResponse updateAlert(UUID userId, UUID id, AlertUpdateInput input) {
		support.validate(input);
		Alert item = alert(userId, id);
		String before = mapper.writeValueAsString(response(item));
		item.setSeverity(input.severity());
		item.setStatus(input.status());
		item.setTitle(input.title());
		item.setMessage(input.message());
		item.setReason(input.reason());
		String after = mapper.writeValueAsString(response(item));
		if (!before.equals(after)) audit(userId, item, "UPDATE", before, after);
		return response(item);
	}

	@Transactional
	public AlertResponse markRead(UUID userId, UUID id) {
		Alert item = alert(userId, id);
		if ("UNREAD".equals(item.getStatus())) {
			String before = mapper.writeValueAsString(response(item));
			item.setStatus("READ");
			audit(userId, item, "UPDATE", before, mapper.writeValueAsString(response(item)));
		}
		return response(item);
	}

	@Transactional
	public void deleteAlert(UUID userId, UUID id) {
		Alert item = alert(userId, id);
		audit(userId, item, "DELETE", mapper.writeValueAsString(response(item)), null);
		alerts.deleteRecord(id);
	}

	@Transactional(readOnly = true)
	public PageResponse<AlertAuditResponse> listAudits(UUID userId, int page, int size) {
		return PageResponse.from(audits.findByUserId(userId, support.page(page, size,
				Sort.by(Sort.Order.desc("occurredAt"),
						Sort.Order.asc("id"))))
				.map(a -> new AlertAuditResponse(a.getId(), a.getAlertId(), a.getOperation(), a.getOccurredAt(),
						a.getDatabaseUser(), a.getOldValues(), a.getNewValues())));
	}

	@Transactional(readOnly = true)
	public AlertAuditResponse getAudit(UUID userId, UUID id) {
		var a = audits.findByIdAndUserId(id, userId).orElseThrow(() -> support.missing("Auditoria"));
		return new AlertAuditResponse(a.getId(), a.getAlertId(), a.getOperation(), a.getOccurredAt(),
				a.getDatabaseUser(), a.getOldValues(), a.getNewValues());
	}

	public PageResponse<RecommendationResponse> generate(UUID userId, RecommendationInput input) {
		return recommendationService.generate(userId, input);
	}

	private void transfer(Recommendation item, UUID userId, RecommendationWriteInput input) {
		support.validate(input);
		item.setDevice(support.device(userId, input.deviceId()));
		item.setPriority(input.priority());
		item.setTitle(input.title());
		item.setMessage(input.message());
		item.setReason(input.reason());
		item.setDisclaimer(input.disclaimer());
		item.setSimulatedData(input.simulatedData() == null || input.simulatedData());
		item.setModelName(input.modelName());
		item.setAiGenerated(false);
		item.setInputFingerprint(null);
		List<Reading> sources = new ArrayList<>();
		for (UUID id : input.readingIds() == null ? List.<UUID>of() : input.readingIds()) {
			Reading row = readings.findByIdAndSensorDeviceFieldPropertyUserId(id, userId)
					.orElseThrow(() -> support.missing("Leitura"));
			if (!row.getSensor().getDevice().getId().equals(input.deviceId()))
				throw support.invalid("readingIds", "As leituras devem pertencer ao dispositivo informado.");
			if (sources.stream().noneMatch(r -> r.getId().equals(id))) sources.add(row);
		}
		item.setReadings(sources);
		item.setBasedOnJson(mapper.writeValueAsString(sources.stream().map(monitoring::snapshot).toList()));
	}

	@Transactional
	public RecommendationResponse createRecommendation(UUID userId, RecommendationWriteInput input) {
		Recommendation item = new Recommendation();
		transfer(item, userId, input);
		return response(recommendations.save(item));
	}

	@Transactional
	public RecommendationResponse updateRecommendation(UUID userId, UUID id, RecommendationWriteInput input) {
		Recommendation item = recommendation(userId, id);
		transfer(item, userId, input);
		return response(item);
	}

	@Transactional(readOnly = true)
	public RecommendationResponse getRecommendation(UUID userId, UUID id) {
		return response(recommendation(userId, id));
	}

	@Transactional(readOnly = true)
	public PageResponse<RecommendationResponse> listRecommendations(UUID userId, UUID deviceId, int page, int size) {
		support.device(userId, deviceId);
		return PageResponse.from(recommendations.findByDeviceIdAndDeviceFieldPropertyUserId(deviceId, userId,
				support.page(page, size, Sort.by(
						Sort.Order.desc("generatedAt"), Sort.Order.asc("id"))))
				.map(this::response));
	}

	@Transactional
	public void deleteRecommendation(UUID userId, UUID id) {
		recommendation(userId, id);
		recommendations.deleteRecord(id);
	}
}
