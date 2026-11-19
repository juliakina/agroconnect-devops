package br.com.fiap.agroconnect_ia;

import br.com.fiap.agroconnect_ia.control.InternalKeyFilter;
import br.com.fiap.agroconnect_ia.control.RecommendationController;
import br.com.fiap.agroconnect_ia.record.AiRecommendationBatch;
import br.com.fiap.agroconnect_ia.record.AiRecommendationDraft;
import br.com.fiap.agroconnect_ia.service.RecommendationAiService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import br.com.fiap.agroconnect_ia.record.AiDeviceContext;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class InternalEndpointTests {

	@Test
	void requiresInternalKeyAndReturnsStructuredOutput() throws Exception {
		FakeService service = new FakeService();
		UUID id = UUID.randomUUID();
		service.output = new AiRecommendationBatch(List.of(new AiRecommendationDraft(id, "Solo seco", "Confira o solo.", "Umidade reduzida.")));
		RecommendationController controller = new RecommendationController();
		ReflectionTestUtils.setField(controller, "service", service);
		InternalKeyFilter filter = new InternalKeyFilter();
		String key = "test-internal-key-12345678901234567890";
		ReflectionTestUtils.setField(filter, "key", key);
		filter.initialize();
		MockMvc mvc = MockMvcBuilders.standaloneSetup(controller).addFilters(filter).build();
		String json = "{\"deviceCode\":\"ESP32-001\",\"plantings\":[],\"alerts\":[{\"alertId\":\"" + id + "\",\"basedOn\":[],\"simulatedData\":true}]}";
		mvc.perform(post("/internal/recommendations").servletPath("/internal/recommendations").contentType("application/json").content(json)).andExpect(status().isUnauthorized());
		assertEquals(0, service.calls);
		mvc.perform(post("/internal/recommendations").servletPath("/internal/recommendations").header("X-Internal-Key", key).contentType("application/json").content(json))
				.andExpect(status().isOk()).andExpect(jsonPath("$.recommendations[0].alertId").value(id.toString()));
		assertEquals(1, service.calls);
	}

	private static class FakeService extends RecommendationAiService {
		private AiRecommendationBatch output;
		private int calls;

		@Override
		public AiRecommendationBatch generate(AiDeviceContext context) {
			calls++;
			return output;
		}
	}
}
