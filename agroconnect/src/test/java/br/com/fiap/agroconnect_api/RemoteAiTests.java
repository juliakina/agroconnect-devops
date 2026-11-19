package br.com.fiap.agroconnect_api;

import br.com.fiap.agroconnect_api.record.AiAlertContext;
import br.com.fiap.agroconnect_api.record.AiDeviceContext;
import br.com.fiap.agroconnect_api.record.AiRecommendationBatch;
import br.com.fiap.agroconnect_api.record.AiRecommendationDraft;
import br.com.fiap.agroconnect_api.service.ApiException;
import br.com.fiap.agroconnect_api.service.RecommendationAiService;
import br.com.fiap.agroconnect_api.service.RecommendationFeignService;
import jakarta.validation.Validation;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import static org.junit.jupiter.api.Assertions.*;


class RemoteAiTests {

	@Test
	void delegatesAndValidatesWithoutLocalModel() throws Exception {
		FakeRemote remote = new FakeRemote();
		RecommendationAiService service = service(remote);
		UUID id = UUID.randomUUID();
		AiDeviceContext context = context(id);
		AiRecommendationBatch output = new AiRecommendationBatch(List.of(new AiRecommendationDraft(id, "Solo seco", "Confira o solo.", "Umidade reduzida.")));
		remote.output = output;
		assertEquals(output, service.generate(context));
		assertEquals("test-internal-key-12345678901234567890", remote.key);
		assertEquals(context, remote.context);
		remote.output = new AiRecommendationBatch(List.of(new AiRecommendationDraft(UUID.randomUUID(), "Solo seco", "Confira o solo.", "Umidade reduzida.")));
		assertEquals("AI_INVALID_RESPONSE", assertThrows(ApiException.class, () -> service.generate(context)).getCode());
	}

	@Test
	void serviceFailureDoesNotFallBackToLocalModel() throws Exception {
		FakeRemote remote = new FakeRemote();
		RecommendationAiService service = service(remote);
		remote.fail = true;
		assertEquals("AI_UNAVAILABLE", assertThrows(ApiException.class, () -> service.generate(context(UUID.randomUUID()))).getCode());
	}

	private RecommendationAiService service(RecommendationFeignService remote) throws Exception {
		RecommendationAiService service = new RecommendationAiService();
		ReflectionTestUtils.setField(service, "remote", remote);
		ReflectionTestUtils.setField(service, "transport", "remote");
		ReflectionTestUtils.setField(service, "internalKey", "test-internal-key-12345678901234567890");
		ReflectionTestUtils.setField(service, "validator", Validation.buildDefaultValidatorFactory().getValidator());
		ReflectionTestUtils.setField(service, "instructions", new ClassPathResource("prompts/recommendations-system.txt"));
		ReflectionTestUtils.setField(service, "clock", Clock.systemUTC());
		service.initialize();
		return service;
	}

	private static class FakeRemote implements RecommendationFeignService {
		private AiRecommendationBatch output;
		private AiDeviceContext context;
		private String key;
		private boolean fail;

		@Override
		public AiRecommendationBatch generate(String key, AiDeviceContext context) {
			if (fail) throw new IllegalStateException("offline");
			this.key = key;
			this.context = context;
			return output;
		}
	}

	private AiDeviceContext context(UUID id) {
		return new AiDeviceContext("ESP32-001", "EstaÃƒÆ’Ã‚Â§ÃƒÆ’Ã‚Â£o", "ONLINE", null, "Canteiro", List.of(),
				List.of(new AiAlertContext(id, "WARNING", "Solo", "Solo seco", "Leitura baixa", List.of(), true)));
	}
}
