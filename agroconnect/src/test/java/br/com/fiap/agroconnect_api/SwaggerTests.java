package br.com.fiap.agroconnect_api;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@TestExecutionListeners(DependencyInjectionTestExecutionListener.class)
class SwaggerTests {

	@Autowired
	private WebApplicationContext context;
	@Autowired
	private FilterChainProxy security;

	@Test
	void documentsMobileContractAndBearerWithoutExposingLegacyRoutes() throws Exception {
		MockMvc mvc = mvc();
		String json = mvc.perform(get("/v3/api-docs/agroconnect-v1")).andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		var document = JsonMapper.builder().build().readTree(json);
		for (var path : document.get("paths").properties()) {
			assertFalse(path.getKey().endsWith("/"), path.getKey());
		}
		assertEquals("bearer", document.at("/components/securitySchemes/bearerAuth/scheme").asText());
		assertTrue(document.at("/paths/~1api~1v1~1auth~1login/post/security").isEmpty());
		assertEquals("bearerAuth", document.at("/paths/~1api~1v1~1devices/get/security/0").properties().iterator().next().getKey());
		assertFalse(document.at("/paths/~1api~1v1~1recommendations/post").isMissingNode());
		assertFalse(document.at("/paths/~1api~1v1~1admin~1users/get").isMissingNode());
		assertTrue(document.at("/paths/~1home").isMissingNode());
		assertTrue(document.at("/paths/~1internal~1recommendations").isMissingNode());
		assertFalse(document.at("/components/schemas/ApiError").isMissingNode());
		assertFalse(document.at("/paths/~1api~1v1~1auth~1register/post/responses/201").isMissingNode());
		assertFalse(document.at("/paths/~1api~1v1~1auth~1logout/post/responses/204").isMissingNode());
		assertFalse(json.contains("\"passwordHash\""));
		mvc.perform(get("/v3/api-docs")).andExpect(status().isOk())
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"));
		mvc.perform(get("/api/v1/devices")).andExpect(status().isUnauthorized());
	}

	@Test
	void swaggerPageAndConfigurationArePublic() throws Exception {
		MockMvc mvc = mvc();
		mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs/swagger-config")).andExpect(status().isOk());
		mvc.perform(get("/v3/api-docs.yaml/agroconnect-v1")).andExpect(status().isOk());
	}

	@Test
	void preservesTheRoutesAndJsonFieldsFromTheCSharpReference() throws Exception {
		var mapper = JsonMapper.builder().build();
		var document = mapper.readTree(mvc().perform(get("/v3/api-docs/agroconnect-v1"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
		try (var resource = getClass().getResourceAsStream("/contracts/csharp-contract.json")) {
			assertNotNull(resource);
			var contract = mapper.readTree(resource);
			for (var method : contract.get("paths").properties()) {
				for (var path : method.getValue()) {
					assertFalse(document.at("/paths/" + path.asText().replace("/", "~1") + "/" + method.getKey()).isMissingNode(),
							method.getKey() + " " + path.asText());
				}
			}
			for (var type : contract.get("fields").properties()) {
				for (var field : type.getValue()) {
					assertFalse(document.at("/components/schemas/" + type.getKey() + "/properties/" + field.asText()).isMissingNode(),
							type.getKey() + "." + field.asText());
				}
			}
		}
	}

	private MockMvc mvc() {
		return MockMvcBuilders.webAppContextSetup(context).addFilters(security).build();
	}
}
