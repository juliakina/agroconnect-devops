package br.com.fiap.agroconnect_ia.control;

import br.com.fiap.agroconnect_ia.record.AiDeviceContext;
import br.com.fiap.agroconnect_ia.record.AiRecommendationBatch;
import br.com.fiap.agroconnect_ia.service.RecommendationAiService;
import br.com.fiap.agroconnect_ia.service.ApiException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {

	@Autowired
	private RecommendationAiService service;

	@PostMapping("/internal/recommendations")
	public AiRecommendationBatch generate(@RequestBody AiDeviceContext context) {
		if (context == null || context.alerts() == null || context.alerts().isEmpty()
				|| context.alerts().size() > 20 || context.alerts().stream().anyMatch(item -> item == null || item.alertId() == null))
			throw new ApiException(400, "INVALID_CONTEXT", "Contexto invÃ¡lido.");
		return service.generate(context);
	}
}
