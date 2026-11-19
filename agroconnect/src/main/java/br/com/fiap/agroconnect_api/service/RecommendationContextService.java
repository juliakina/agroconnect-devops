package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.record.AiAlertContext;
import br.com.fiap.agroconnect_api.record.AiDeviceContext;
import br.com.fiap.agroconnect_api.record.AiPlantingContext;
import br.com.fiap.agroconnect_api.record.ReadingSnapshot;
import br.com.fiap.agroconnect_api.record.RecommendationCache;
import br.com.fiap.agroconnect_api.record.RecommendationResponse;
import br.com.fiap.agroconnect_api.record.RecommendationWork;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import br.com.fiap.agroconnect_api.repository.PlantingRepository;
import br.com.fiap.agroconnect_api.repository.RecommendationRepository;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RecommendationContextService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private AlertRepository alerts;
	@Autowired
	private PlantingRepository plantings;
	@Autowired
	private RecommendationRepository recommendations;
	@Autowired
	private Clock clock;
	private final JsonMapper mapper = JsonMapper.builder().build();

	@Transactional(readOnly = true)
	public void verifyOwnership(UUID userId, UUID deviceId) {
		support.device(userId, deviceId);
	}

	@Transactional(readOnly = true)
	public RecommendationWork load(UUID userId, UUID deviceId) {
		var device = support.device(userId, deviceId);
		var sources = alerts.findForRecommendation(userId, deviceId, PageRequest.of(0, 20));
		var crops = plantings.findByFieldIdAndFieldPropertyUserId(device.getField().getId(), userId,
				PageRequest.of(0, 20, Sort.by(Sort.Order.desc("plantedAt"), Sort.Order.asc("id"))))
				.stream().map(item -> new AiPlantingContext(item.getCropName(), item.getStage(), item.getPlantedAt())).toList();
		List<AiAlertContext> items = sources.stream().map(item -> {
			List<ReadingSnapshot> basedOn = mapper.readValue(item.getBasedOnJson(),
					mapper.getTypeFactory().constructCollectionType(List.class, ReadingSnapshot.class));
			boolean simulated = item.getReadings().isEmpty() || item.getReadings().stream().anyMatch(row -> row.getSimulatedData());
			return new AiAlertContext(item.getId(), item.getSeverity(), item.getTitle(), item.getMessage(), item.getReason(), basedOn, simulated);
		}).filter(item -> item.basedOn().stream().anyMatch(row -> row.value() != null)).toList();
		Map<UUID, RecommendationCache> cached = new LinkedHashMap<>();
		for (var item : items) {
			recommendations.findBySourceAlertId(item.alertId())
					.filter(row -> row.getDevice().getId().equals(deviceId)).ifPresent(row -> {
						List<ReadingSnapshot> evidence = mapper.readValue(row.getBasedOnJson(),
								mapper.getTypeFactory().constructCollectionType(List.class, ReadingSnapshot.class));
						var response = new RecommendationResponse(row.getId(), row.getGeneratedAt(), row.getPriority(),
								row.getTitle(), row.getMessage(), row.getReason(), evidence, row.getDisclaimer(), row.getSimulatedData());
						cached.put(item.alertId(), new RecommendationCache(response, row.getAiGenerated(), row.getModelName(), row.getInputFingerprint()));
					});
		}
		return new RecommendationWork(new AiDeviceContext(device.getCode(), device.getName(),
				MonitoringService.deviceStatus(device.getLastSeenAt(), clock.instant()), device.getLastSeenAt(),
				device.getField().getName(), crops, items), cached);
	}
}
