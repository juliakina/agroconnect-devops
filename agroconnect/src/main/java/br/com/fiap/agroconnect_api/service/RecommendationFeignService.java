package br.com.fiap.agroconnect_api.service;

import br.com.fiap.agroconnect_api.record.AiDeviceContext;
import br.com.fiap.agroconnect_api.record.AiRecommendationBatch;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "agroconnect-ia", contextId = "recommendationAi")
public interface RecommendationFeignService {

	@PostMapping("/internal/recommendations")
	AiRecommendationBatch generate(@RequestHeader("X-Internal-Key") String key, @RequestBody AiDeviceContext context);
}
