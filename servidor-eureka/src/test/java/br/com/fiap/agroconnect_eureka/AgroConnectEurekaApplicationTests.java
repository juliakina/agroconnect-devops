package br.com.fiap.agroconnect_eureka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.registry.password=test-registry-password-12345678901234567890")
@org.springframework.test.context.TestExecutionListeners(org.springframework.test.context.support.DependencyInjectionTestExecutionListener.class)
class AgroConnectEurekaApplicationTests {

	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.web.context.WebApplicationContext context;
	@org.springframework.beans.factory.annotation.Autowired
	private org.springframework.security.web.FilterChainProxy security;

	@Test
	void protectsTheRegistryAndExposesOnlyTheHealthCheck() throws Exception {
		var mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(context)
				.addFilters(security).build();
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/health"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/"))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
		String basic = java.util.Base64.getEncoder().encodeToString(
				"agroconnect:test-registry-password-12345678901234567890".getBytes(java.nio.charset.StandardCharsets.UTF_8));
		mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/").header("Authorization", "Basic " + basic))
				.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());
	}


	@Test
	void contextLoads() {
	}

}
