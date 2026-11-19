package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.record.AiAlertContext;
import br.com.fiap.agroconnect_api.record.AiDeviceContext;
import br.com.fiap.agroconnect_api.record.PageMeta;
import br.com.fiap.agroconnect_api.record.PageResponse;
import br.com.fiap.agroconnect_api.record.RecommendationInput;
import br.com.fiap.agroconnect_api.record.RecommendationResponse;
import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService {

	@Autowired
	private DomainSupport support;
	@Autowired
	private RecommendationContextService contexts;
	@Autowired
	private RecommendationAiService ai;
	@Autowired
	private RecommendationStoreService store;
	@Autowired
	private Clock clock;
	@Value("${app.ai.cache-minutes:30}")
	private int cacheMinutes;
	private final ConcurrentHashMap<UUID, Boolean> inProgress = new ConcurrentHashMap<>();
	private final Semaphore slots = new Semaphore(4);

	public PageResponse<RecommendationResponse> generate(UUID userId, RecommendationInput input) {
		support.validate(input);
		contexts.verifyOwnership(userId, input.deviceId());
		if (inProgress.putIfAbsent(input.deviceId(), Boolean.TRUE) != null)
			throw new ApiException(409, "AI_GENERATION_IN_PROGRESS", "Já existe uma análise em andamento para este dispositivo.");
		boolean acquired = false;
		try {
			var work = contexts.load(userId, input.deviceId());
			if (work.context().alerts().isEmpty()) return page(List.of());
			Map<UUID, RecommendationResponse> result = new LinkedHashMap<>();
			Map<UUID, String> fingerprints = new LinkedHashMap<>();
			List<AiAlertContext> pending = new ArrayList<>();
			for (var alert : work.context().alerts()) {
				String fingerprint = ai.fingerprint(work.context(), alert);
				fingerprints.put(alert.alertId(), fingerprint);
				var cached = work.cached().get(alert.alertId());
				if (cached != null && cached.aiGenerated() && ai.modelName().equals(cached.modelName())
						&& fingerprint.equals(cached.inputFingerprint())
						&& !cached.response().generatedAt().isAfter(clock.instant())
						&& Duration.between(cached.response().generatedAt(), clock.instant()).compareTo(Duration.ofMinutes(Math.max(0, cacheMinutes))) < 0) {
					result.put(alert.alertId(), cached.response());
				} else pending.add(alert);
			}
			if (!pending.isEmpty()) {
				acquired = slots.tryAcquire();
				if (!acquired) throw new ApiException(429, "AI_BUSY", "O serviço de IA está ocupado. Tente novamente em instantes.");
				var context = work.context();
				var request = new AiDeviceContext(context.deviceCode(), context.deviceName(), context.deviceStatus(),
						context.lastSeenAt(), context.fieldName(), context.plantings(), pending);
				var output = ai.generate(request);
				try {
					result.putAll(store.save(userId, input.deviceId(), request, output, fingerprints));
				} catch (DataIntegrityViolationException exception) {
					throw new ApiException(409, "AI_CONTEXT_CHANGED", "Outra análise foi salva. Consulte as recomendações novamente.");
				}
			}
			return page(work.context().alerts().stream().map(item -> result.get(item.alertId())).toList());
		} finally {
			if (acquired) slots.release();
			inProgress.remove(input.deviceId());
		}
	}

	private PageResponse<RecommendationResponse> page(List<RecommendationResponse> items) {
		return new PageResponse<>(items, new PageMeta(1, 20, items.size(), 1));
	}
}
