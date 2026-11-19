package br.com.fiap.agroconnect_api;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class IdempotencyTests {

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private FilterChainProxy security;
	@Autowired
	private JsonMapper mapper;

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
	}

	private String token(MockMvc mvc) throws Exception {
		String json = "{\"name\":\"Produtor teste\",\"email\":\"" + UUID.randomUUID()
				+ "@example.com\",\"password\":\"SenhaTeste123!\"}";
		String response = mvc.perform(post("/api/v1/auth/register").contentType("application/json").content(json))
				.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
		return "Bearer " + mapper.readTree(response).get("accessToken").asText();
	}

	@Test
	void concurrentRetriesAndTrailingSlashReplayTheSameCreation() throws Exception {
		MockMvc mvc = mvc();
		String token = token(mvc);
		String body = "{\"name\":\"Horta\",\"city\":\"Cotia\",\"state\":\"SP\",\"areaHectares\":1}";
		var pool = Executors.newFixedThreadPool(4);
		try {
			var calls = new ArrayList<Callable<String>>();
			for (int i = 0; i < 4; i++) calls.add(() -> mvc.perform(post("/api/v1/properties")
					.header("Authorization", token).contentType("application/json").content(body))
					.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString());
			var responses = pool.invokeAll(calls);
			String first = responses.get(0).get();
			for (var response : responses) assertEquals(first, response.get());
			mvc.perform(post("/api/v1/properties/").header("Authorization", token).contentType("application/json").content(body))
					.andExpect(status().isCreated()).andExpect(content().json(first));
			mvc.perform(get("/api/v1/properties").header("Authorization", token))
					.andExpect(status().isOk()).andExpect(jsonPath("$.page.totalItems").value(1));
		} finally {
			pool.shutdownNow();
		}
	}

	@Test
	void explicitKeyRejectsChangedContentAndInvalidHeaders() throws Exception {
		MockMvc mvc = mvc();
		String token = token(mvc);
		String body = "{\"name\":\"Horta\",\"city\":\"Cotia\",\"state\":\"SP\",\"areaHectares\":1}";
		mvc.perform(post("/api/v1/properties").header("Authorization", token).header("Idempotency-Key", "property-1")
				.contentType("application/json").content(body)).andExpect(status().isCreated());
		mvc.perform(post("/api/v1/properties").header("Authorization", token).header("Idempotency-Key", "property-1")
				.contentType("application/json").content(body.replace("Horta", "Outra")))
				.andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("IDEMPOTENCY_CONFLICT"));
		mvc.perform(post("/api/v1/properties").header("Authorization", token).header("Idempotency-Key", "one", "two")
				.contentType("application/json").content(body)).andExpect(status().isBadRequest());
	}
}
