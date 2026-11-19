package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.model.AiInteraction;
import br.com.fiap.agroconnect_api.model.Recommendation;
import br.com.fiap.agroconnect_api.record.AiAlertContext;
import br.com.fiap.agroconnect_api.record.AiDeviceContext;
import br.com.fiap.agroconnect_api.record.AiRecommendationBatch;
import br.com.fiap.agroconnect_api.record.RecommendationResponse;
import br.com.fiap.agroconnect_api.repository.AiInteractionRepository;
import br.com.fiap.agroconnect_api.repository.AlertRepository;
import br.com.fiap.agroconnect_api.repository.RecommendationRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Clock;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class RecommendationStoreService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private RecommendationContextService contexts;
	@Autowired
	private RecommendationAiService ai;
	@Autowired
	private AlertRepository alerts;
	@Autowired
	private RecommendationRepository recommendations;
	@Autowired
	private AiInteractionRepository interactions;
	@Autowired
	private Clock clock;
	private final JsonMapper mapper = JsonMapper.builder().build();
	@PersistenceContext
	private EntityManager entityManager;

	@Transactional
	public Map<UUID, RecommendationResponse> save(UUID userId, UUID deviceId, AiDeviceContext context,
			AiRecommendationBatch generated, Map<UUID, String> fingerprints) {
		entityManager.flush();
		entityManager.clear();
		var device = support.device(userId, deviceId);
		Map<UUID, AiAlertContext> original = new LinkedHashMap<>();
		context.alerts().forEach(item -> original.put(item.alertId(), item));
		var locked = context.alerts().stream().map(AiAlertContext::alertId).sorted().map(id ->
				alerts.lockOwned(id, userId).orElseThrow(this::changed)).toList();
		var current = contexts.load(userId, deviceId).context();
		for (var expected : context.alerts()) {
			var actual = current.alerts().stream().filter(item -> item.alertId().equals(expected.alertId()))
					.findFirst().orElseThrow(this::changed);
			if (!fingerprints.get(expected.alertId()).equals(ai.fingerprint(current, actual))) throw changed();
		}
		Map<UUID, RecommendationResponse> result = new LinkedHashMap<>();
		for (var draft : generated.recommendations()) {
			var source = locked.stream().filter(item -> item.getId().equals(draft.alertId())).findFirst().orElseThrow(this::changed);
			var evidence = original.get(draft.alertId());
			Recommendation item = recommendations.findBySourceAlertId(source.getId()).orElseGet(Recommendation::new);
			if (item.getId() != null && !item.getDevice().getId().equals(deviceId)) throw changed();
			item.setDevice(device);
			item.setSourceAlertId(source.getId());
			item.setGeneratedAt(clock.instant());
			item.setPriority(source.getSeverity());
			item.setTitle(draft.title());
			item.setMessage(draft.message());
			item.setReason(draft.reason());
			item.setSimulatedData(evidence.simulatedData());
			item.setDisclaimer(evidence.simulatedData()
					? "Orientação gerada por IA com dados simulados. Confira as condições reais antes de decidir."
					: "Orientação informativa gerada por IA. Confira as condições locais antes de decidir.");
			item.setModelName(ai.modelName());
			item.setAiGenerated(true);
			item.setInputFingerprint(fingerprints.get(source.getId()));
			item.setBasedOnJson(mapper.writeValueAsString(evidence.basedOn()));
			item.setReadings(new ArrayList<>(source.getReadings()));
			recommendations.save(item);
			result.put(source.getId(), new RecommendationResponse(item.getId(), item.getGeneratedAt(), item.getPriority(),
					item.getTitle(), item.getMessage(), item.getReason(), evidence.basedOn(), item.getDisclaimer(), item.getSimulatedData()));
		}
		AiInteraction interaction = new AiInteraction();
		interaction.setUser(support.user(userId));
		interaction.setProperty(device.getField().getProperty());
		interaction.setDevice(device);
		interaction.setQuestion("Interpretar alertas e leituras do dispositivo para orientar o produtor.");
		interaction.setContext(ai.contextJson(context));
		interaction.setResponse(mapper.writeValueAsString(generated));
		interaction.setModelName(ai.modelName());
		interaction.setCreatedAt(clock.instant());
		interactions.save(interaction);
		recommendations.flush();
		return result;
	}

	private ApiException changed() {
		return new ApiException(409, "AI_CONTEXT_CHANGED", "Os dados mudaram durante a análise. Solicite a recomendação novamente.");
	}
}
